package com.longvuong.plix.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.notification.NotificationPermissionChecker;
import com.longvuong.plix.data.reminder.ReminderPreferences;
import com.longvuong.plix.data.reminder.ReminderScheduler;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

//Cài đặt nhắc nhở (FIN-10, FIN-13): lưu cấu hình theo người dùng hiện tại rồi báo ReminderScheduler xếp hoặc huỷ lịch.
//Cấu hình đọc đồng bộ từ SharedPreferences nên không có trạng thái Loading/Error. Không import lớp Android, không gọi DAO hay Retrofit
@HiltViewModel
public class ReminderSettingsViewModel extends ViewModel {
    private static final int MINUTES_PER_HOUR = 60;

    private final ReminderPreferences reminderPreferences;
    private final ReminderScheduler reminderScheduler;
    private final AuthManager authManager;
    private final NotificationPermissionChecker notificationPermissionChecker;

    private final MutableLiveData<Boolean> dailyEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> dailyMinutes = new MutableLiveData<>(); //phút trong ngày, ví dụ 07:30 = 450; có giá trị khi đã đăng nhập
    private final MutableLiveData<Boolean> recurringEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> goalEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> permissionBannerNeeded = new MutableLiveData<>(false);

    @Inject
    public ReminderSettingsViewModel(ReminderPreferences reminderPreferences,
                                     ReminderScheduler reminderScheduler,
                                     AuthManager authManager,
                                     NotificationPermissionChecker notificationPermissionChecker) {
        this.reminderPreferences = reminderPreferences;
        this.reminderScheduler = reminderScheduler;
        this.authManager = authManager;
        this.notificationPermissionChecker = notificationPermissionChecker;
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            dailyEnabled.setValue(reminderPreferences.isDailyEnabled(userId));
            dailyMinutes.setValue(reminderPreferences.getDailyMinutes(userId));
            recurringEnabled.setValue(reminderPreferences.isRecurringEnabled(userId));
            goalEnabled.setValue(reminderPreferences.isGoalEnabled(userId));
        }
        updatePermissionBanner();
    }

    public LiveData<Boolean> getDailyEnabled() {
        return dailyEnabled;
    }

    public LiveData<Integer> getDailyMinutes() {
        return dailyMinutes;
    }

    public LiveData<Boolean> getRecurringEnabled() {
        return recurringEnabled;
    }

    public LiveData<Boolean> getGoalEnabled() {
        return goalEnabled;
    }

    //true khi có công tắc đang bật mà app thiếu quyền thông báo
    public LiveData<Boolean> getPermissionBannerNeeded() {
        return permissionBannerNeeded;
    }

    //Bật thì xếp lịch, tắt thì huỷ lịch ngay. Cấu hình vẫn được lưu kể cả khi thiếu quyền thông báo
    public void setDailyEnabled(boolean enabled) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        reminderPreferences.setDailyEnabled(userId, enabled);
        dailyEnabled.setValue(enabled);
        if (enabled) {
            reminderScheduler.applyForCurrentUser();
        } else {
            reminderScheduler.cancelDaily();
        }
        updatePermissionBanner();
    }

    //Đổi giờ nhắc: huỷ lượt đang chờ ở giờ cũ rồi xếp lại theo giờ mới (applyForCurrentUser không tự huỷ lượt cũ)
    public void setDailyTime(int hour, int minute) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        int minutesOfDay = hour * MINUTES_PER_HOUR + minute;
        reminderPreferences.setDailyMinutes(userId, minutesOfDay);
        dailyMinutes.setValue(minutesOfDay);
        reminderScheduler.cancelDaily();
        reminderScheduler.applyForCurrentUser();
    }

    public void setRecurringEnabled(boolean enabled) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        reminderPreferences.setRecurringEnabled(userId, enabled);
        recurringEnabled.setValue(enabled);
        syncDueSchedule(userId);
        updatePermissionBanner();
    }

    public void setGoalEnabled(boolean enabled) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            return;
        }
        reminderPreferences.setGoalEnabled(userId, enabled);
        goalEnabled.setValue(enabled);
        syncDueSchedule(userId);
        updatePermissionBanner();
    }

    //Nhắc định kỳ và nhắc mục tiêu dùng chung một lịch 09:00: còn ít nhất một công tắc bật thì xếp, tắt cả hai thì huỷ
    private void syncDueSchedule(String userId) {
        if (reminderPreferences.isRecurringEnabled(userId) || reminderPreferences.isGoalEnabled(userId)) {
            reminderScheduler.applyForCurrentUser();
        } else {
            reminderScheduler.cancelDue();
        }
    }

    private void updatePermissionBanner() {
        boolean anyEnabled = Boolean.TRUE.equals(dailyEnabled.getValue())
                || Boolean.TRUE.equals(recurringEnabled.getValue())
                || Boolean.TRUE.equals(goalEnabled.getValue());
        permissionBannerNeeded.setValue(anyEnabled && !notificationPermissionChecker.isNotificationPermissionGranted());
    }
}
