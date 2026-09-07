package com.longvuong.plix.core;

import android.app.Application;

import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.core.error.GlobalExceptionHandler;
import com.longvuong.plix.data.local.AppDatabase;

import javax.inject.Inject;

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
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build();
    }
}