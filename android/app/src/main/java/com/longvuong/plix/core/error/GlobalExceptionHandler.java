package com.longvuong.plix.core.error;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "GlobalExceptionHandler";

    private final Thread.UncaughtExceptionHandler defaultHandler;

    @Inject
    public GlobalExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.e(TAG, "Loi khong luong truoc tren thread \"" + thread.getName() + "\"", throwable);

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}