package com.longvuong.plix.data.repository;

import android.content.Context;

import androidx.work.WorkManager;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.core.network.NetworkObserver;
import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.sync.SyncLock;
import com.longvuong.plix.data.sync.SyncPreferences;
import com.longvuong.plix.data.sync.SyncPusher;
import com.longvuong.plix.data.sync.SyncScheduler;
import com.longvuong.plix.data.sync.SyncTables;
import com.longvuong.plix.data.sync.SyncWorker;
import com.longvuong.plix.data.sync.SyncableEntity;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class LogoutRepositoryImpl implements LogoutRepository {
    private final Context context;
    private final AuthManager authManager;
    private final AppDatabase appDatabase;
    private final SyncTables syncTables;
    private final SyncPusher syncPusher;
    private final SyncPreferences syncPreferences;
    private final SyncScheduler syncScheduler;
    private final SyncLock syncLock;
    private final NetworkObserver networkObserver;
    private final AppExecutors appExecutors;

    @Inject
    public LogoutRepositoryImpl(@ApplicationContext Context context, AuthManager authManager, AppDatabase appDatabase,
                                SyncTables syncTables, SyncPusher syncPusher, SyncPreferences syncPreferences,
                                SyncScheduler syncScheduler, SyncLock syncLock, NetworkObserver networkObserver,
                                AppExecutors appExecutors) {
        this.context = context;
        this.authManager = authManager;
        this.appDatabase = appDatabase;
        this.syncTables = syncTables;
        this.syncPusher = syncPusher;
        this.syncPreferences = syncPreferences;
        this.syncScheduler = syncScheduler;
        this.syncLock = syncLock;
        this.networkObserver = networkObserver;
        this.appExecutors = appExecutors;
    }

    @Override
    public int countPendingChanges() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        int total = 0;
        for (SyncableEntity<?, ?> table : syncTables.inSyncOrder()) {
            total += table.getPendingSync(userId).size();
        }
        return total;
    }

    @Override
    public boolean isOnline() {
        //Chưa có giá trị thì coi như không có mạng: đi thẳng tới hộp thoại xác nhận, không bao giờ mất dữ liệu âm thầm
        return Boolean.TRUE.equals(networkObserver.getIsConnected().getValue());
    }

    @Override
    public void cancelRunningSync() {
        WorkManager workManager = WorkManager.getInstance(context);
        try {
            workManager.cancelUniqueWork(SyncWorker.UNIQUE_ONE_TIME_WORK_NAME).getResult().get();
            workManager.cancelUniqueWork(SyncWorker.UNIQUE_PERIODIC_WORK_NAME).getResult().get();
        } catch (ExecutionException e) {
            Timber.w(e, "Huỷ SyncWorker thất bại, vẫn chờ lượt đang chạy kết thúc");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //WorkManager chỉ đánh dấu huỷ, luồng đang chạy dở vẫn chạy tiếp tới khi doWork() trả về -> chờ khoá để chắc chắn nó đã xong
        syncLock.awaitIdle();
    }

    @Override
    public void pushAllPending(long timeoutMillis) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        AtomicBoolean stopRequested = new AtomicBoolean(false);
        FutureTask<Void> task = new FutureTask<>(() -> {
            //Refresh token thất bại trong lúc này thì không đá ra màn hình đăng nhập, để hộp thoại xác nhận hiện ra (Mục 6.9 bước 3-4)
            authManager.setSessionExpiredSignalSuppressed(true);
            try {
                pushAllTables(userId, stopRequested);
            } finally {
                authManager.setSessionExpiredSignalSuppressed(false); //reset ngay khi luồng đẩy kết thúc, kể cả khi quá hạn 10 giây mà nó còn chạy nốt
            }
            return null;
        });
        appExecutors.networkIO().execute(task);
        try {
            task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Timber.w("Đồng bộ lần cuối quá %d ms, bỏ qua", timeoutMillis);
        } catch (ExecutionException e) {
            Timber.w(e.getCause(), "Đồng bộ lần cuối thất bại");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            //Quá hạn thì báo cho luồng đẩy dừng ở lần kiểm tra kế tiếp (giữa các lô / các bảng)
            stopRequested.set(true);
        }
    }

    @Override
    public void clearLocalSession() {
        cancelRunningSync();
        syncLock.lock(); //giữ khoá suốt lúc xoá: không SyncWorker nào chen vào ghi dữ liệu của người dùng cũ
        try {
            String userId = authManager.getCurrentUserId();
            if (userId != null) {
                appDatabase.runInTransaction(() -> {
                    appDatabase.transactionDao().deleteAllByUserId(userId);
                    appDatabase.categoryDao().deleteAllByUserId(userId);
                    appDatabase.budgetDao().deleteAllByUserId(userId);
                    appDatabase.goalDao().deleteAllByUserId(userId);
                    appDatabase.correctionDao().deleteAllByUserId(userId);
                });
                syncPreferences.clearPullCursors(userId);
            }
            syncPreferences.clearLastError();
            authManager.logout();
            Timber.d("Đã đăng xuất và xoá dữ liệu cục bộ của phiên");
        } finally {
            syncLock.unlock();
        }
        //Đã huỷ SyncWorker định kỳ ở trên, đăng ký lại để lần đăng nhập sau không phải chờ tới khi mở lại app
        syncScheduler.schedulePeriodicSync();
    }

    @Override
    public void resumeSync() {
        syncScheduler.schedulePeriodicSync();
        syncScheduler.requestSync(); //bản ghi pending vẫn còn, cho đồng bộ nền thử lại
    }

    //Đẩy lần lượt theo thứ tự cố định của SyncTables, giữ khoá để không chạy chồng với SyncWorker
    private void pushAllTables(String userId, AtomicBoolean stopRequested) throws IOException {
        syncLock.lock();
        try {
            for (SyncableEntity<?, ?> table : syncTables.inSyncOrder()) {
                if (stopRequested.get()) {
                    return;
                }
                syncPusher.push(table, userId, stopRequested::get);
            }
        } finally {
            syncLock.unlock();
        }
    }
}