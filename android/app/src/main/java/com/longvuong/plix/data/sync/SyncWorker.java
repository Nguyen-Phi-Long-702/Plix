package com.longvuong.plix.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.api.HealthApiService;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionPushRequestDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import retrofit2.Response;
import timber.log.Timber;

@HiltWorker
public class SyncWorker extends Worker {
    public static final String TAG = "sync_worker";
    public static final String UNIQUE_PERIODIC_WORK_NAME = "sync_periodic_worker";
    public static final String UNIQUE_ONE_TIME_WORK_NAME = "sync_one_time_worker";

    private static final int BATCH_SIZE = 50;
    private static final long WARM_UP_INTERVAL_MILLIS = 10 * 60 * 1000L;

    private final TransactionDao transactionDao;
    private final AuthManager authManager;
    private final SyncApiService syncApiService;
    private final HealthApiService healthApiService;
    private final SyncPreferences syncPreferences;

    @AssistedInject
    public SyncWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters workerParameters,
                      TransactionDao transactionDao, AuthManager authManager, SyncApiService syncApiService,
                      HealthApiService healthApiService, SyncPreferences syncPreferences) {
        super(context, workerParameters);
        this.transactionDao = transactionDao;
        this.authManager = authManager;
        this.syncApiService = syncApiService;
        this.healthApiService = healthApiService;
        this.syncPreferences = syncPreferences;
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
            List<TransactionEntity> pending = transactionDao.getPendingSync(userId);
            if (pending.isEmpty()) {
                syncPreferences.clearLastError();
                return Result.success();
            }
            warmUpIfNeeded(System.currentTimeMillis()); //chỉ warm-up khi thật sự có dữ liệu cần đẩy (tránh gọi /health vô ích mỗi 15 phút)
            Timber.d("Bắt đầu đồng bộ %d giao dịch đang chờ", pending.size());

            for (int from = 0; from < pending.size(); from += BATCH_SIZE) {
                if (isStopped()) {
                    return Result.retry(); //bị thay thế bởi lượt debounce mới, kết quả này sẽ bị bỏ qua
                }
                int to = Math.min(from + BATCH_SIZE, pending.size());
                Result batchFailure = pushBatch(pending.subList(from, to));
                if (batchFailure != null) {
                    return batchFailure;
                }
            }
            syncPreferences.clearLastError();
            Timber.d("Đồng bộ xong");
            return Result.success();
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

    //Trả về null nếu đợt này thành công, ngược lại trả về kết quả để doWork() dừng và trả ra ngoài
    private Result pushBatch(List<TransactionEntity> batch) throws IOException {
        List<TransactionSyncRecordDto> records = new ArrayList<>();
        for (TransactionEntity entity : batch) {
            records.add(TransactionSyncRecordDto.fromEntity(entity));
        }
        Response<SyncPushResponseDto> response =
                syncApiService.pushTransactions(new TransactionPushRequestDto(records)).execute();
        if (!response.isSuccessful() || response.body() == null) {
            return handleHttpFailure(response.code());
        }
        markSynced(batch, response.body());
        return null;
    }

    private void markSynced(List<TransactionEntity> batch, SyncPushResponseDto body) {
        Set<String> upsertedIds = new HashSet<>(
                body.upsertedIds != null ? body.upsertedIds : Collections.<String>emptyList());
        for (TransactionEntity entity : batch) {
            if (upsertedIds.contains(entity.id)) {
                //chỉ đánh dấu khi updated_at không đổi, nếu người dùng vừa sửa thì giữ pending cho lượt sau
                transactionDao.markSynced(entity.id, entity.updatedAt);
            }
        }
        if (body.rejected != null && !body.rejected.isEmpty()) {
            Timber.w("Máy chủ từ chối %d giao dịch, giữ nguyên trạng thái pending", body.rejected.size());
        }
    }

    private Result handleHttpFailure(int code) {
        Timber.w("Đẩy giao dịch thất bại, mã HTTP %d", code);
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