package com.longvuong.plix.data.sync;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SyncScheduler {
    private static final long PERIODIC_INTERVAL_MINUTES = 15;
    private static final long DEBOUNCE_DELAY_SECONDS = 3;

    private final Context context;

    @Inject
    public SyncScheduler(@ApplicationContext Context context) {
        this.context = context;
    }

    //Chạy định kỳ mỗi 15 phút (KEEP: mở app nhiều lần không đăng ký lại)
    public void schedulePeriodicSync() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(connectedConstraints())
                .addTag(SyncWorker.TAG)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SyncWorker.UNIQUE_PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    //Chạy ngay sau khi ghi dữ liệu, có debounce: REPLACE + initialDelay -> nhiều lần gọi liên tiếp trong vài giây chỉ còn 1 lượt chạy thật
    public void requestSync() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInitialDelay(DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS)
                .setConstraints(connectedConstraints())
                .addTag(SyncWorker.TAG)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                SyncWorker.UNIQUE_ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    private Constraints connectedConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }
}