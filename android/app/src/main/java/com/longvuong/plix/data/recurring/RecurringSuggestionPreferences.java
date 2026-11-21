package com.longvuong.plix.data.recurring;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

//Danh sách gợi ý giao dịch định kỳ mà người dùng đã bấm "Bỏ qua", lưu riêng theo từng user_id (FIN-19, NF-11)
@Singleton
public class RecurringSuggestionPreferences {
    private static final String PREF_NAME = "recurring_suggestion_prefs";
    private static final String KEY_DISMISSED_PREFIX = "dismissed_";

    private final SharedPreferences prefs;

    @Inject
    public RecurringSuggestionPreferences(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    //Mỗi phần tử có dạng type|categoryId (RecurringCandidate.getKey()). Chưa bỏ qua gì thì trả về tập rỗng
    public Set<String> getDismissedKeys(String userId) {
        //Tập SharedPreferences trả về không được sửa trực tiếp, nên luôn trả về bản sao
        Set<String> stored = prefs.getStringSet(dismissedKey(userId), null);
        return stored == null ? new HashSet<>() : new HashSet<>(stored);
    }

    public void dismiss(String userId, String key) {
        Set<String> updated = getDismissedKeys(userId);
        updated.add(key);
        prefs.edit().putStringSet(dismissedKey(userId), updated).apply();
    }

    //Cùng chữ ký hàm clearForUser của hợp đồng C1 để DP-22-V1 nối bằng method reference; userId null thì không có gì để xoá
    public void clearForUser(@Nullable String userId) {
        if (userId == null) {
            return;
        }
        prefs.edit().remove(dismissedKey(userId)).apply();
    }

    private static String dismissedKey(String userId) {
        return KEY_DISMISSED_PREFIX + userId;
    }
}