package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.longvuong.plix.data.sync.SyncStatus;

public interface SyncRepository {
    LiveData<SyncStatus> getSyncStatus();

    @Nullable
    String getLastSyncError();

    void requestSync();

    LiveData<Boolean> getDeletedCategoryNotice();

    void onDeletedCategoryNoticeHandled();
}