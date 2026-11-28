package com.longvuong.plix.data.reminder;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.auth.UserSessionCleaner;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

//Cấu hình nhắc nhở lưu riêng theo từng user_id; đồng thời là bộ dọn UserSessionCleaner để xoá khi đăng xuất (FIN-13, NF-11)
@Singleton
public class ReminderPreferences implements UserSessionCleaner {
    private static final String PREF_NAME = "reminder_prefs";
    private static final String KEY_DAILY_ENABLED = "_daily_enabled";
    private static final String KEY_DAILY_MINUTES = "_daily_minutes";
    private static final String KEY_RECURRING_ENABLED = "_recurring_enabled";
    private static final String KEY_GOAL_ENABLED = "_goal_enabled";
    private static final int DEFAULT_DAILY_MINUTES = 1200; //20:00 (20 x 60 phút)

    private final SharedPreferences prefs;
    private final Provider<ReminderScheduler> reminderSchedulerProvider; //dùng Provider để tránh vòng phụ thuộc: ReminderSchedulerImpl cũng cần ReminderPreferences

    @Inject
    public ReminderPreferences(@ApplicationContext Context context, Provider<ReminderScheduler> reminderSchedulerProvider) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.reminderSchedulerProvider = reminderSchedulerProvider;
    }

    public boolean isDailyEnabled(String userId) {
        return prefs.getBoolean(key(userId, KEY_DAILY_ENABLED), false);
    }

    public void setDailyEnabled(String userId, boolean enabled) {
        prefs.edit().putBoolean(key(userId, KEY_DAILY_ENABLED), enabled).apply();
    }

    //Phút trong ngày của giờ nhắc, ví dụ 07:30 lưu 450. Chưa đặt thì trả 1200 (20:00)
    public int getDailyMinutes(String userId) {
        return prefs.getInt(key(userId, KEY_DAILY_MINUTES), DEFAULT_DAILY_MINUTES);
    }

    public void setDailyMinutes(String userId, int minutesOfDay) {
        prefs.edit().putInt(key(userId, KEY_DAILY_MINUTES), minutesOfDay).apply();
    }

    public boolean isRecurringEnabled(String userId) {
        return prefs.getBoolean(key(userId, KEY_RECURRING_ENABLED), false);
    }

    public void setRecurringEnabled(String userId, boolean enabled) {
        prefs.edit().putBoolean(key(userId, KEY_RECURRING_ENABLED), enabled).apply();
    }

    public boolean isGoalEnabled(String userId) {
        return prefs.getBoolean(key(userId, KEY_GOAL_ENABLED), false);
    }

    public void setGoalEnabled(String userId, boolean enabled) {
        prefs.edit().putBoolean(key(userId, KEY_GOAL_ENABLED), enabled).apply();
    }

    //Xoá bốn khoá của người dùng (hợp đồng C1) rồi huỷ mọi lịch nhắc (DP_02 mục 3.7). userId null thì không có khoá để xoá nhưng vẫn huỷ lịch
    @Override
    public void clearForUser(@Nullable String userId) {
        if (userId != null) {
            prefs.edit()
                    .remove(key(userId, KEY_DAILY_ENABLED))
                    .remove(key(userId, KEY_DAILY_MINUTES))
                    .remove(key(userId, KEY_RECURRING_ENABLED))
                    .remove(key(userId, KEY_GOAL_ENABLED))
                    .apply();
        }
        reminderSchedulerProvider.get().cancelAll();
    }

    //Khoá có dạng <userId>_daily_enabled (DP_02 mục 3.5)
    private static String key(String userId, String suffix) {
        return userId + suffix;
    }
}
