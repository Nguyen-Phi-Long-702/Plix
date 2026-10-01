package com.longvuong.plix.presentation.sync;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.repository.SyncRepository;
import com.longvuong.plix.data.sync.SyncStatus;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SyncStatusViewModel extends ViewModel {
    private final SyncRepository syncRepository;

    @Inject
    public SyncStatusViewModel(SyncRepository syncRepository) {
        this.syncRepository = syncRepository;
    }

    public LiveData<SyncStatus> getSyncStatus() {
        return syncRepository.getSyncStatus();
    }

    @Nullable
    public String getLastSyncError() {
        return syncRepository.getLastSyncError();
    }
}