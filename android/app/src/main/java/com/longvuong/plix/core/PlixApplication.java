package com.longvuong.plix.core;

import android.app.Application;

import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.longvuong.plix.data.sync.RecurringTransactionWorker;
import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.core.error.GlobalExceptionHandler;
import com.longvuong.plix.data.local.AppDatabase;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class PlixApplication extends Application implements Configuration.Provider {

    @Inject
    AppDatabase appDatabase;

    @Inject
    HiltWorkerFactory hiltWorkerFactory;

    @Inject
    GlobalExceptionHandler globalExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase ignored = appDatabase;
        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        scheduleRecurringTransactionWorker();
    }

    private void scheduleRecurringTransactionWorker() {
        PeriodicWorkRequest recurringTransactionRequest = new PeriodicWorkRequest.Builder(RecurringTransactionWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                RecurringTransactionWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                recurringTransactionRequest);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build();
    }
}