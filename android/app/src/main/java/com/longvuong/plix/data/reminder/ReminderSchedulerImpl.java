package com.longvuong.plix.data.reminder;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
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
//Nhắc hằng ngày (reminder_daily) và nhắc định kỳ/mục tiêu (reminder_due) đều là chuỗi OneTimeWork tự xếp lượt kế tiếp.
//CXM-13 không xác nhận được REPLACE từ bên trong chính Worker cùng tên nên mỗi chuỗi dùng hai tên xen kẽ:
//lượt kế tiếp luôn nằm ở tên khác lượt đang chạy (chọn theo ngày chẵn/lẻ của lần nhắc kế tiếp)
public class ReminderSchedulerImpl implements ReminderScheduler {
    private static final String DAILY_WORK_NAME = "reminder_daily";
    private static final String DAILY_ALT_WORK_NAME = "reminder_daily_alt";
    private static final String DUE_WORK_NAME = "reminder_due";
    private static final String DUE_ALT_WORK_NAME = "reminder_due_alt";
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

    //Đưa lịch về đúng với cấu hình hiện tại: công tắc bật thì xếp lượt kế tiếp, tắt hoặc chưa đăng nhập thì huỷ.
    //Không huỷ trước khi xếp, nên gọi lúc khởi động app (PlixApplication) không làm mất lượt đang tới giờ chạy khi WorkManager
    //đánh thức tiến trình; lượt kế tiếp nằm ở tên xen kẽ nên không đụng lượt đó. Muốn bỏ lượt cũ (đổi giờ) thì gọi cancelDaily() trước
    @Override
    public void applyForCurrentUser() {
        String userId = currentUserId();
        boolean dailyOn = userId != null && reminderPreferences.isDailyEnabled(userId);
        boolean dueOn = userId != null
                && (reminderPreferences.isRecurringEnabled(userId) || reminderPreferences.isGoalEnabled(userId));
        if (dailyOn) {
            scheduleNextDaily();
        } else {
            cancelDaily();
        }
        if (dueOn) {
            scheduleNextDue();
        } else {
            cancelDue();
        }
    }

    @Override
    public void scheduleNextDaily() {
        String userId = currentUserId();
        if (userId == null || !reminderPreferences.isDailyEnabled(userId)) {
            return;
        }
        int minutesOfDay = reminderPreferences.getDailyMinutes(userId);
        LocalTime target = LocalTime.of(minutesOfDay / MINUTES_PER_HOUR, minutesOfDay % MINUTES_PER_HOUR);
        enqueueNext(DailyReminderWorker.class, DAILY_WORK_NAME, DAILY_ALT_WORK_NAME, target);
    }

    //Lượt kiểm tra định kỳ và mục tiêu luôn lúc 09:00 (giờ máy)
    private void scheduleNextDue() {
        enqueueNext(DueReminderWorker.class, DUE_WORK_NAME, DUE_ALT_WORK_NAME,
                LocalTime.of(DueReminderWorker.DUE_CHECK_HOUR, 0));
    }

    //Tính độ trễ tới lần `target` kế tiếp từ đồng hồ hiện tại rồi xếp ở tên theo ngày chẵn/lẻ của lần đó
    private void enqueueNext(Class<? extends ListenableWorker> workerClass, String workName, String altWorkName, LocalTime target) {
        ZonedDateTime now = ZonedDateTime.now();
        long delayMillis = computeNextTriggerUseCase.execute(now, target);
        LocalDate triggerDate = now.plus(Duration.ofMillis(delayMillis)).toLocalDate();
        String name = triggerDate.toEpochDay() % 2 == 0 ? workName : altWorkName;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(workerClass)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request);
    }

    @Override
    public void cancelDaily() {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(DAILY_WORK_NAME);
        workManager.cancelUniqueWork(DAILY_ALT_WORK_NAME);
    }

    @Override
    public void cancelDue() {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(DUE_WORK_NAME);
        workManager.cancelUniqueWork(DUE_ALT_WORK_NAME);
    }

    @Override
    public void cancelAll() {
        cancelDaily();
        cancelDue();
    }

    @Nullable
    private String currentUserId() {
        return authManager.isLoggedIn() ? authManager.getCurrentUserId() : null;
    }
}
