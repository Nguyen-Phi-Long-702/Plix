package com.longvuong.plix.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.api.HealthApiService;
import com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

@HiltWorker
public class SyncWorker extends Worker {
    public static final String TAG = "sync_worker";
    public static final String UNIQUE_PERIODIC_WORK_NAME = "sync_periodic_worker";
    public static final String UNIQUE_ONE_TIME_WORK_NAME = "sync_one_time_worker";

    private static final long WARM_UP_INTERVAL_MILLIS = 10 * 60 * 1000L;

    private final AuthManager authManager;
    private final HealthApiService healthApiService;
    private final SyncPreferences syncPreferences;
    private final SyncTables syncTables;
    private final SyncPusher syncPusher;
    private final SyncPuller syncPuller;
    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;
    private final DeletedCategoryNotice deletedCategoryNotice;
    private final SyncLock syncLock;

    @AssistedInject
    public SyncWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters workerParameters,
                      AuthManager authManager, HealthApiService healthApiService, SyncPreferences syncPreferences,
                      SyncTables syncTables, SyncPusher syncPusher, SyncPuller syncPuller,
                      CheckBudgetThresholdUseCase checkBudgetThresholdUseCase, DeletedCategoryNotice deletedCategoryNotice,
                      SyncLock syncLock) {
        super(context, workerParameters);
        this.authManager = authManager;
        this.healthApiService = healthApiService;
        this.syncPreferences = syncPreferences;
        this.syncTables = syncTables;
        this.syncPusher = syncPusher;
        this.syncPuller = syncPuller;
        this.checkBudgetThresholdUseCase = checkBudgetThresholdUseCase;
        this.deletedCategoryNotice = deletedCategoryNotice;
        this.syncLock = syncLock;
    }

    @NonNull
    @Override
    public Result doWork() {
        syncLock.lock(); //đăng xuất sẽ chờ ở đây tới khi lượt chạy này kết thúc, tránh xoá dữ liệu giữa chừng
        try {
            return doWorkLocked();
        } finally {
            syncLock.unlock();
        }
    }

    private Result doWorkLocked() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Timber.d("Chưa có phiên đăng nhập trong bộ nhớ, bỏ qua lượt đồng bộ");
            return Result.success();
        }
        try {
            warmUpIfNeeded(System.currentTimeMillis()); //lượt nào cũng có request thật (đẩy hoặc kéo) nên luôn kiểm tra warm-up
            pushAllTables(userId);
            if (isStopped()) {
                return Result.retry(); //bị thay thế bởi lượt debounce mới, kết quả này sẽ bị bỏ qua
            }
            List<EntityChange<TransactionEntity>> pulledTransactions = new ArrayList<>();
            List<EntityChange<CategoryEntity>> pulledCategories = new ArrayList<>();
            try {
                pullAllTables(userId, pulledTransactions, pulledCategories); //đẩy xong cả 5 bảng mới kéo về
            } finally {
                //Giao dịch đã ghi vào Room thì phải kiểm tra ngưỡng, kể cả khi lượt kéo bị dừng hoặc lỗi giữa chừng (con trỏ đã tiến qua các lô đó)
                checkBudgetThresholdAfterPull(pulledTransactions);
                //Danh mục vừa bị xoá trên thiết bị khác mà giao dịch trên máy vẫn dùng: báo giao diện hiện 1 thông báo
                deletedCategoryNotice.checkAfterPull(pulledCategories);
            }
            if (isStopped()) {
                return Result.retry(); //bị huỷ giữa chừng khi đang kéo, kết quả này sẽ bị bỏ qua
            }
            syncPreferences.clearLastError();
            Timber.d("Đồng bộ xong");
            return Result.success();
        } catch (HttpException e) {
            return handleHttpFailure(e.code()); //máy chủ trả mã lỗi khi đẩy hoặc kéo dữ liệu
        } catch (IOException e) {
            if (isStopped()) {
                return Result.retry(); //bị huỷ do lượt debounce mới thay thế, không phải lỗi thật
            }
            Timber.w(e, "Đồng bộ thất bại do lỗi mạng, sẽ thử lại");
            syncPreferences.saveLastError("Không kết nối được máy chủ");
            return Result.retry();
        } catch (Exception e) {
            Timber.e(e, "Đồng bộ thất bại do lỗi không xác định");
            syncPreferences.saveLastError("Đã xảy ra lỗi không xác định");
            return Result.failure();
        }
    }

    //Đẩy lần lượt theo thứ tự cố định của SyncTables
    private void pushAllTables(String userId) throws IOException {
        for (SyncableEntity<?, ?> table : syncTables.inSyncOrder()) {
            if (isStopped()) {
                return;
            }
            syncPusher.push(table, userId, this::isStopped);
        }
    }

    //Kéo lần lượt cùng thứ tự; mỗi bảng có con trỏ riêng (khoá theo tên bảng trong SyncPreferences)
    //Bảng giao dịch ghi nhận thay đổi để kiểm tra ngưỡng ngân sách, bảng danh mục ghi nhận thay đổi để phát hiện danh mục vừa bị xoá
    private void pullAllTables(String userId, List<EntityChange<TransactionEntity>> pulledTransactions,
                               List<EntityChange<CategoryEntity>> pulledCategories) throws IOException {
        TransactionSyncableEntity transactionTable = syncTables.transactions();
        CategorySyncableEntity categoryTable = syncTables.categories();
        for (SyncableEntity<?, ?> table : syncTables.inSyncOrder()) {
            if (isStopped()) {
                return;
            }
            if (table == transactionTable) {
                syncPuller.pull(transactionTable, userId, this::isStopped, pulledTransactions);
            } else if (table == categoryTable) {
                syncPuller.pull(categoryTable, userId, this::isStopped, pulledCategories);
            } else {
                syncPuller.pull(table, userId, this::isStopped);
            }
        }
    }

    private void checkBudgetThresholdAfterPull(List<EntityChange<TransactionEntity>> pulledTransactions) {
        if (pulledTransactions.isEmpty()) {
            return;
        }
        List<TransactionEntity> oldVersions = new ArrayList<>();
        List<TransactionEntity> newVersions = new ArrayList<>();
        for (EntityChange<TransactionEntity> change : pulledTransactions) {
            newVersions.add(change.after);
            if (change.before != null) {
                oldVersions.add(change.before);
            }
        }
        checkBudgetThresholdUseCase.checkAfterPull(oldVersions, newVersions);
    }

    private Result handleHttpFailure(int code) {
        Timber.w("Đồng bộ thất bại, mã HTTP %d", code);
        syncPreferences.saveLastError("Máy chủ phản hồi lỗi (mã " + code + ")");
        boolean retryable = code >= 500 || code == 429 || code == 408;
        return retryable ? Result.retry() : Result.failure();
    }

    private void warmUpIfNeeded(long now) {
        if (!needsWarmUp(syncPreferences.getLastHealthCallAt(), now)) {
            return;
        }
        Timber.d("Gọi /health warm-up trước khi đồng bộ");
        try {
            Response<Void> response = healthApiService.health().execute();
            if (response.isSuccessful()) {
                syncPreferences.setLastHealthCallAt(System.currentTimeMillis());
            }
        } catch (IOException e) {
            Timber.w(e, "Gọi /health warm-up thất bại, vẫn tiếp tục đồng bộ");
        }
    }

    private static boolean needsWarmUp(long lastCallAt, long now) {
        return now - lastCallAt > WARM_UP_INTERVAL_MILLIS;
    }
}