package com.longvuong.plix.presentation.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.reminder.ReminderPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ReminderSettingsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final String USER_A = "user-A";

    private FakeReminderPreferences fakePreferences;
    private FakeReminderScheduler fakeScheduler;
    private FakeNotificationPermission fakePermission;
    private ReminderSettingsViewModel viewModel;

    @Before
    public void setUp() {
        fakePreferences = new FakeReminderPreferences();
        fakeScheduler = new FakeReminderScheduler();
        fakePermission = new FakeNotificationPermission();
        viewModel = new ReminderSettingsViewModel(fakePreferences, fakeScheduler, new FakeAuthManagerWithUser(USER_A), fakePermission);
    }

    /**
     * TN3-U11
     * Chưa có quyền thông báo, setDailyEnabled(true): cấu hình lưu enabled=true; permissionBannerNeeded = true; scheduler.applyForCurrentUser được gọi 1 lần.
     */
    @Test
    public void setDailyEnabled_trueWithoutNotificationPermission_savesConfigShowsBannerAndAppliesSchedule() {
        fakePermission.granted = false;

        viewModel.setDailyEnabled(true);

        assertTrue(fakePreferences.isDailyEnabled(USER_A));
        assertEquals(Boolean.TRUE, viewModel.getDailyEnabled().getValue());
        assertEquals(Boolean.TRUE, viewModel.getPermissionBannerNeeded().getValue());
        assertEquals(1, fakeScheduler.applyCalls);
    }

    /**
     * TN3-U12
     * setDailyTime(7, 30): lưu 450 phút; getDailyMinutes() = 450; FakeReminderScheduler.applyCalls tăng 1.
     */
    @Test
    public void setDailyTime_7h30_saves450MinutesAndAppliesScheduleOnce() {
        viewModel.setDailyTime(7, 30);

        assertEquals(450, fakePreferences.getDailyMinutes(USER_A));
        assertEquals(Integer.valueOf(450), viewModel.getDailyMinutes().getValue());
        assertEquals(1, fakeScheduler.applyCalls);
    }

    /**
     * TN3-U13
     * setDailyEnabled(false): FakeReminderScheduler.cancelDailyCalls = 1 và cấu hình lưu enabled=false.
     */
    @Test
    public void setDailyEnabled_false_cancelsDailySchedule() {
        viewModel.setDailyEnabled(false);

        assertEquals(1, fakeScheduler.cancelDailyCalls);
        assertFalse(fakePreferences.isDailyEnabled(USER_A));
    }

    //AuthManager giả: không có phiên đăng nhập thật, chỉ trả userId do test đặt
    private static class FakeAuthManagerWithUser extends AuthManager {
        private final String userId;

        FakeAuthManagerWithUser(String userId) {
            super(null);
            this.userId = userId;
        }

        @Override
        public String getCurrentUserId() {
            return userId;
        }
    }

    //Cấu hình nhắc nhở lưu trong bộ nhớ, không dùng SharedPreferences thật. Phải ghi đè cả 8 hàm đọc/ghi vì ViewModel đọc cả 4 cấu hình khi khởi tạo
    private static class FakeReminderPreferences extends ReminderPreferences {
        private static final int DEFAULT_MINUTES = 1200; //20:00, cùng mặc định với ReminderPreferences
        private final Map<String, Boolean> dailyEnabled = new HashMap<>();
        private final Map<String, Integer> dailyMinutes = new HashMap<>();
        private final Map<String, Boolean> recurringEnabled = new HashMap<>();
        private final Map<String, Boolean> goalEnabled = new HashMap<>();

        FakeReminderPreferences() {
            //Hàm khởi tạo của lớp cha gọi context.getSharedPreferences, nên cần một Context không ném lỗi "not mocked"
            super(new ContextWrapper(null) {
                @Override
                public SharedPreferences getSharedPreferences(String name, int mode) {
                    return null; //các hàm bên dưới đều được ghi đè nên không bao giờ dùng tới
                }
            }, () -> null);
        }

        @Override
        public boolean isDailyEnabled(String userId) {
            return Boolean.TRUE.equals(dailyEnabled.get(userId));
        }

        @Override
        public void setDailyEnabled(String userId, boolean enabled) {
            dailyEnabled.put(userId, enabled);
        }

        @Override
        public int getDailyMinutes(String userId) {
            Integer minutes = dailyMinutes.get(userId);
            return minutes == null ? DEFAULT_MINUTES : minutes;
        }

        @Override
        public void setDailyMinutes(String userId, int minutesOfDay) {
            dailyMinutes.put(userId, minutesOfDay);
        }

        @Override
        public boolean isRecurringEnabled(String userId) {
            return Boolean.TRUE.equals(recurringEnabled.get(userId));
        }

        @Override
        public void setRecurringEnabled(String userId, boolean enabled) {
            recurringEnabled.put(userId, enabled);
        }

        @Override
        public boolean isGoalEnabled(String userId) {
            return Boolean.TRUE.equals(goalEnabled.get(userId));
        }

        @Override
        public void setGoalEnabled(String userId, boolean enabled) {
            goalEnabled.put(userId, enabled);
        }
    }
}
