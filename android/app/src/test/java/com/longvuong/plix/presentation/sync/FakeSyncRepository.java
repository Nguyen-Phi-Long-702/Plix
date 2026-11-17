package com.longvuong.plix.presentation.sync;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.data.repository.SyncRepository;
import com.longvuong.plix.data.sync.SyncStatus;

//Bản giả của SyncRepository: test đổi syncStatus/online/lastSyncError và đếm số lần gọi requestSync/requestSyncNow
public class FakeSyncRepository implements SyncRepository {
    public final MutableLiveData<SyncStatus> syncStatus = new MutableLiveData<>(SyncStatus.IDLE);
    public String lastSyncError;
    public boolean online = true;
    public int requestSyncCalls;
    public int requestSyncNowCalls;
    private final MutableLiveData<Boolean> deletedCategoryNotice = new MutableLiveData<>(false);

    @Override
    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatus;
    }

    @Nullable
    @Override
    public String getLastSyncError() {
        return lastSyncError;
    }

    @Override
    public void requestSync() {
        requestSyncCalls++;
    }

    @Override
    public void requestSyncNow() {
        requestSyncNowCalls++;
    }

    @Override
    public long getLastSuccessAt() {
        return 0L;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    public LiveData<Boolean> getDeletedCategoryNotice() {
        return deletedCategoryNotice;
    }

    @Override
    public void onDeletedCategoryNoticeHandled() {
        //không dùng trong các test của TN-5
    }
}