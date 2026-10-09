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
    private static final String KEY_PULL_CURSOR_PREFIX = "pull_cursor_";

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

    //Con trỏ kéo dữ liệu theo từng (user_id, bảng). Chưa có thì trả về 0 để kéo toàn bộ
    public long getPullCursor(String userId, String table) {
        return prefs.getLong(pullCursorKey(userId, table), 0L);
    }

    public void setPullCursor(String userId, String table, long cursor) {
        prefs.edit().putLong(pullCursorKey(userId, table), cursor).apply();
    }

    private static String pullCursorKey(String userId, String table) {
        return KEY_PULL_CURSOR_PREFIX + userId + "_" + table;
    }

    //Đăng xuất xoá dữ liệu cục bộ nên phải xoá luôn con trỏ của user đó, lần đăng nhập sau mới kéo lại toàn bộ (con trỏ về 0)
    public void clearPullCursors(String userId) {
        String prefix = KEY_PULL_CURSOR_PREFIX + userId + "_";
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }
}