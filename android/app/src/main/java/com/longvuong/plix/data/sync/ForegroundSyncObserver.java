package com.longvuong.plix.data.sync;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.repository.SyncRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

//Tự đồng bộ khi app quay lại foreground (SYNC-11)
//Gắn vào ProcessLifecycleOwner (không phải lifecycle của Activity) để xoay màn hình không kích hoạt đồng bộ; điều này được kiểm chứng ở TN5-M03, không coi là đã biết
//Giới hạn 60 giây nằm ở đây; debounce 3 giây của SyncScheduler.requestSync() và SyncLock của SyncWorker giữ nguyên
@Singleton
public class ForegroundSyncObserver implements DefaultLifecycleObserver {
    private static final long MIN_INTERVAL_MILLIS = 60_000L;

    private final SyncRepository syncRepository;
    private final AuthManager authManager;
    //0 = chưa yêu cầu lần nào: lần khởi động nguội đầu tiên cũng yêu cầu một lượt đồng bộ
    private long lastRequestAt;

    @Inject
    public ForegroundSyncObserver(SyncRepository syncRepository, AuthManager authManager) {
        this.syncRepository = syncRepository;
        this.authManager = authManager;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        onForeground(System.currentTimeMillis());
    }

    //Tách riêng khỏi onStart để test không cần Android (TN5-U08, TN5-U09)
    public void onForeground(long nowMillis) {
        if (!authManager.isLoggedIn()) {
            return;
        }
        if (nowMillis - lastRequestAt < MIN_INTERVAL_MILLIS) {
            return;
        }
        syncRepository.requestSync();
        lastRequestAt = nowMillis;
    }
}