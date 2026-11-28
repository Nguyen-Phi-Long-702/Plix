package com.longvuong.plix.data.reminder;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.domain.usecase.reminder.ComputeNextTriggerUseCase;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

//Cài đặt ReminderScheduler bằng WorkManager (DP_02 mục 3.1 và 3.3).
//Nhắc hằng ngày là chuỗi OneTimeWork tự xếp lượt kế tiếp. CXM-13 không xác nhận được REPLACE từ bên trong chính Worker cùng tên
//nên dùng hai tên xen kẽ: lượt kế tiếp luôn nằm ở tên khác lượt đang chạy (chọn theo ngày chẵn/lẻ của lần nhắc)
public class ReminderSchedulerImpl implements ReminderScheduler {
    private static final String DAILY_WORK_NAME = "reminder_daily";
    private static final String DAILY_ALT_WORK_NAME = "reminder_daily_alt";
    private static final String DUE_WORK_NAME = "reminder_due";
    private static final int MINUTES_PER_HOUR = 60;

    private final Context context;
    private final AuthManager authManager;
    private final ReminderPreferences reminderPreferences;
    private final ComputeNextTriggerUseCase computeNextTriggerUseCase;

    @Inject
    public ReminderSchedulerImpl(
            @ApplicationContext Context context,
            AuthManager authManager,
            ReminderPreferences reminderPreferences,
            ComputeNextTriggerUseCase computeNextTriggerUseCase) {
        this.context = context;
        this.authManager = authManager;
        this.reminderPreferences = reminderPreferences;
        this.computeNextTriggerUseCase = computeNextTriggerUseCase;
    }

    //Huỷ cả hai tên rồi xếp lại từ đầu: người dùng đổi giờ thì giờ mới có hiệu lực, công tắc tắt thì không xếp lại gì
    @Override
    public void applyForCurrentUser() {
        cancelDaily();
        scheduleNextDaily();
    }

    @Override
    public void scheduleNextDaily() {
        String userId = authManager.isLoggedIn() ? authManager.getCurrentUserId() : null;
        if (userId == null || !reminderPreferences.isDailyEnabled(userId)) {
            return;
        }
        int minutesOfDay = reminderPreferences.getDailyMinutes(userId);
        LocalTime target = LocalTime.of(minutesOfDay / MINUTES_PER_HOUR, minutesOfDay % MINUTES_PER_HOUR);
        ZonedDateTime now = ZonedDateTime.now();
        long delayMillis = computeNextTriggerUseCase.execute(now, target);
        LocalDate triggerDate = now.plus(Duration.ofMillis(delayMillis)).toLocalDate();
        String workName = triggerDate.toEpochDay() % 2 == 0 ? DAILY_WORK_NAME : DAILY_ALT_WORK_NAME;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DailyReminderWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request);
    }

    @Override
    public void cancelDaily() {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(DAILY_WORK_NAME);
        workManager.cancelUniqueWork(DAILY_ALT_WORK_NAME);
    }

    @Override
    public void cancelDue() {
        WorkManager.getInstance(context).cancelUniqueWork(DUE_WORK_NAME);
    }

    @Override
    public void cancelAll() {
        cancelDaily();
        cancelDue();
    }
}
