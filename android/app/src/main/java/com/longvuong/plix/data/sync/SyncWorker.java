package com.longvuong.plix.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.remote.api.HealthApiService;

import java.io.IOException;

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
    private final TransactionSyncableEntity transactionTable;
    private final SyncPusher syncPusher;
    private final SyncPuller syncPuller;

    @AssistedInject
    public SyncWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters workerParameters,
                      AuthManager authManager, HealthApiService healthApiService, SyncPreferences syncPreferences,
                      TransactionSyncableEntity transactionTable, SyncPusher syncPusher, SyncPuller syncPuller) {
        super(context, workerParameters);
        this.authManager = authManager;
        this.healthApiService = healthApiService;
        this.syncPreferences = syncPreferences;
        this.transactionTable = transactionTable;
        this.syncPusher = syncPusher;
        this.syncPuller = syncPuller;
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Timber.d("Chưa có phiên đăng nhập trong bộ nhớ, bỏ qua lượt đồng bộ");
            return Result.success();
        }
        try {
            warmUpIfNeeded(System.currentTimeMillis()); //lượt nào cũng có request thật (đẩy hoặc kéo) nên luôn kiểm tra warm-up
            syncPusher.push(transactionTable, userId, this::isStopped);
            if (isStopped()) {
                return Result.retry(); //bị thay thế bởi lượt debounce mới, kết quả này sẽ bị bỏ qua
            }
            syncPuller.pull(transactionTable, userId, this::isStopped); //đẩy xong mới kéo về
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