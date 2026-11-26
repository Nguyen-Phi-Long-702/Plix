package com.longvuong.plix.data.reminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReminderPreferencesInstrumentedTest {
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final int DEFAULT_DAILY_MINUTES = 1200; //20:00
    private static final int CUSTOM_DAILY_MINUTES = 450; //07:30

    private ReminderPreferences preferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = new ReminderPreferences(context);
        cleanUp(); //bắt đầu từ trạng thái sạch, không phụ thuộc lần chạy trước
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        preferences.clearForUser(USER_A);
        preferences.clearForUser(USER_B);
    }

    /**
     * TN3-I01
     * set daily enabled cho user-A; đọc cho user-B; clearForUser('user-A'):
     * user-B mặc định false; sau clearForUser user-A trở về mặc định, user-B không đổi.
     */
    @Test
    public void settings_areIsolatedPerUser_andBackToDefaultAfterClearForUser() {
        //Mặc định khi chưa đặt gì: ba công tắc tắt, giờ nhắc 20:00
        assertFalse(preferences.isDailyEnabled(USER_A));
        assertFalse(preferences.isRecurringEnabled(USER_A));
        assertFalse(preferences.isGoalEnabled(USER_A));
        assertEquals(DEFAULT_DAILY_MINUTES, preferences.getDailyMinutes(USER_A));

        //Cấu hình của user-A không lọt sang user-B
        preferences.setDailyEnabled(USER_A, true);
        preferences.setDailyMinutes(USER_A, CUSTOM_DAILY_MINUTES);
        preferences.setRecurringEnabled(USER_A, true);
        preferences.setGoalEnabled(USER_A, true);
        assertTrue(preferences.isDailyEnabled(USER_A));
        assertEquals(CUSTOM_DAILY_MINUTES, preferences.getDailyMinutes(USER_A));
        assertFalse(preferences.isDailyEnabled(USER_B));
        assertFalse(preferences.isRecurringEnabled(USER_B));
        assertFalse(preferences.isGoalEnabled(USER_B));
        assertEquals(DEFAULT_DAILY_MINUTES, preferences.getDailyMinutes(USER_B));

        //user-B có một cấu hình riêng để kiểm tra việc xoá của user-A không đụng tới
        preferences.setGoalEnabled(USER_B, true);

        //clearForUser('user-A'): cả bốn khoá của user-A về mặc định, user-B không đổi
        preferences.clearForUser(USER_A);
        assertFalse(preferences.isDailyEnabled(USER_A));
        assertFalse(preferences.isRecurringEnabled(USER_A));
        assertFalse(preferences.isGoalEnabled(USER_A));
        assertEquals(DEFAULT_DAILY_MINUTES, preferences.getDailyMinutes(USER_A));
        assertTrue(preferences.isGoalEnabled(USER_B));
        assertFalse(preferences.isDailyEnabled(USER_B));

        //userId null không làm văng và không đổi gì (hợp đồng C1)
        preferences.clearForUser(null);
        assertTrue(preferences.isGoalEnabled(USER_B));
    }
}
