package com.longvuong.plix.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.longvuong.plix.data.sync.SyncPreferences;
import com.longvuong.plix.data.sync.SyncStatus;
import com.longvuong.plix.data.sync.SyncWorker;
import com.longvuong.plix.data.sync.SyncScheduler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SyncRepositoryImpl implements SyncRepository {
    private final SyncPreferences syncPreferences;
    private final SyncScheduler syncScheduler;
    private final LiveData<SyncStatus> syncStatus;

    @Inject
    public SyncRepositoryImpl(@ApplicationContext Context context, SyncPreferences syncPreferences, SyncScheduler syncScheduler) {
        this.syncPreferences = syncPreferences;
        this.syncScheduler = syncScheduler;
        LiveData<List<WorkInfo>> workInfos = WorkManager.getInstance(context).getWorkInfosByTagLiveData(SyncWorker.TAG);
        this.syncStatus = Transformations.map(workInfos, infos -> toSyncStatus(infos));
    }

    @Override
    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatus;
    }

    @Nullable
    @Override
    public String getLastSyncError() {
        return syncPreferences.getLastError();
    }

    @Override
    public void requestSync() {
        syncScheduler.requestSync();
    }

    private SyncStatus toSyncStatus(@Nullable List<WorkInfo> workInfos) {
        if (workInfos == null) {
            return SyncStatus.IDLE;
        }
        boolean hasErrorState = false;
        for (WorkInfo info : workInfos) {
            WorkInfo.State state = info.getState();
            if (state == WorkInfo.State.RUNNING) {
                return SyncStatus.SYNCING;
            }
            if (state == WorkInfo.State.FAILED || (state == WorkInfo.State.ENQUEUED && info.getRunAttemptCount() > 0)) {
                hasErrorState = true;
            }
        }
        return (hasErrorState && syncPreferences.getLastError() != null) ? SyncStatus.ERROR : SyncStatus.IDLE;
    }
}