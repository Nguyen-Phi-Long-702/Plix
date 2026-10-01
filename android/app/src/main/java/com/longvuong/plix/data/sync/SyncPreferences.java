package com.longvuong.plix.data.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SyncPreferences {
    private static final String PREF_NAME = "sync_prefs";
    private static final String KEY_LAST_HEALTH_CALL_AT = "last_health_call_at";
    private static final String KEY_LAST_ERROR = "last_error";

    private final SharedPreferences prefs;

    @Inject
    public SyncPreferences(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public long getLastHealthCallAt() {
        return prefs.getLong(KEY_LAST_HEALTH_CALL_AT, 0L);
    }

    public void setLastHealthCallAt(long timeMillis) {
        prefs.edit().putLong(KEY_LAST_HEALTH_CALL_AT, timeMillis).apply();
    }

    @Nullable
    public String getLastError() {
        return prefs.getString(KEY_LAST_ERROR, null);
    }

    public void saveLastError(String message) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply();
    }

    public void clearLastError() {
        prefs.edit().remove(KEY_LAST_ERROR).apply();
    }
}