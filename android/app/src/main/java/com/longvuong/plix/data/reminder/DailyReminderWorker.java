package com.longvuong.plix.data.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.notification.NotificationHelper;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

//Phát thông báo nhắc ghi chép hằng ngày (FIN-10). Tự xếp lượt kế tiếp ở cuối, nên chuỗi nhắc không phụ thuộc PeriodicWork
@HiltWorker
public class DailyReminderWorker extends Worker {
    private static final int NOTIFICATION_ID = 7001; //id cố định để mỗi ngày chỉ có một thông báo nhắc ghi chép (DP_01 mục 3.5)
    private static final String NOTIFICATION_TITLE = "Nhắc ghi chép";
    private static final String NOTIFICATION_TEXT = "Hôm nay bạn đã ghi chi tiêu chưa?";

    private final AuthManager authManager;
    private final ReminderPreferences reminderPreferences;
    private final NotificationHelper notificationHelper;
    private final ReminderScheduler reminderScheduler;

    @AssistedInject
    public DailyReminderWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParameters,
            AuthManager authManager,
            ReminderPreferences reminderPreferences,
            NotificationHelper notificationHelper,
            ReminderScheduler reminderScheduler) {
        super(context, workerParameters);
        this.authManager = authManager;
        this.reminderPreferences = reminderPreferences;
        this.notificationHelper = notificationHelper;
        this.reminderScheduler = reminderScheduler;
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = authManager.isLoggedIn() ? authManager.getCurrentUserId() : null;
        if (userId == null || !reminderPreferences.isDailyEnabled(userId)) {
            //Đã đăng xuất hoặc công tắc đã tắt: huỷ lịch và dừng, không xếp lượt mới
            reminderScheduler.cancelDaily();
            return Result.success();
        }
        try {
            notificationHelper.notifyReminder(NOTIFICATION_ID, NOTIFICATION_TITLE, NOTIFICATION_TEXT);
        } finally {
            //Xếp lượt kế tiếp ở cuối và luôn chạy, kể cả khi phát thông báo lỗi, để chuỗi nhắc không bị đứt
            reminderScheduler.scheduleNextDaily();
        }
        return Result.success();
    }
}
