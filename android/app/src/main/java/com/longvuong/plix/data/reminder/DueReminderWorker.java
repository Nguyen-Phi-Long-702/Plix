package com.longvuong.plix.data.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.notification.NotificationHelper;
import com.longvuong.plix.data.local.dao.GoalDao;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.reminder.FindGoalsNearDeadlineUseCase;
import com.longvuong.plix.domain.usecase.reminder.FindUpcomingRecurringDueUseCase;
import com.longvuong.plix.domain.usecase.reminder.GoalDeadlineReminder;
import com.longvuong.plix.domain.usecase.reminder.UpcomingRecurringDue;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

//Mỗi ngày lúc 09:00 kiểm tra giao dịch định kỳ đến hạn ngày mai và mục tiêu còn 7 hoặc 1 ngày, rồi phát nhắc (FIN-11, FIN-12).
//Chỉ đọc Room và phát thông báo, không tạo giao dịch, không gọi mạng. Tự xếp lượt kế tiếp ở cuối như DailyReminderWorker
@HiltWorker
public class DueReminderWorker extends Worker {
    static final int DUE_CHECK_HOUR = 9; //giờ kiểm tra mỗi ngày; ReminderSchedulerImpl dùng cùng hằng này để tính lượt kế tiếp
    private static final int RECURRING_NOTIFICATION_ID = 7002; //id cố định (DP_02 mục 3.2)
    private static final int GOAL_NOTIFICATION_ID = 7003;
    private static final int RECURRING_LEAD_DAYS = 1; //nhắc trước 1 ngày (FIN-11)
    private static final String RECURRING_TITLE = "Nhắc giao dịch định kỳ";
    private static final String GOAL_TITLE = "Nhắc mục tiêu";
    private static final String NO_NOTE_TEXT = "giao dịch định kỳ"; //mẫu không có ghi chú

    private final TransactionDao transactionDao;
    private final GoalDao goalDao;
    private final AuthManager authManager;
    private final ReminderPreferences reminderPreferences;
    private final NotificationHelper notificationHelper;
    private final ReminderScheduler reminderScheduler;
    private final FindUpcomingRecurringDueUseCase findUpcomingRecurringDueUseCase;
    private final FindGoalsNearDeadlineUseCase findGoalsNearDeadlineUseCase;

    @AssistedInject
    public DueReminderWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParameters,
            TransactionDao transactionDao,
            GoalDao goalDao,
            AuthManager authManager,
            ReminderPreferences reminderPreferences,
            NotificationHelper notificationHelper,
            ReminderScheduler reminderScheduler,
            FindUpcomingRecurringDueUseCase findUpcomingRecurringDueUseCase,
            FindGoalsNearDeadlineUseCase findGoalsNearDeadlineUseCase) {
        super(context, workerParameters);
        this.transactionDao = transactionDao;
        this.goalDao = goalDao;
        this.authManager = authManager;
        this.reminderPreferences = reminderPreferences;
        this.notificationHelper = notificationHelper;
        this.reminderScheduler = reminderScheduler;
        this.findUpcomingRecurringDueUseCase = findUpcomingRecurringDueUseCase;
        this.findGoalsNearDeadlineUseCase = findGoalsNearDeadlineUseCase;
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = authManager.isLoggedIn() ? authManager.getCurrentUserId() : null;
        if (userId == null) {
            reminderScheduler.cancelDue(); //đã đăng xuất: huỷ lịch và dừng
            return Result.success();
        }
        boolean recurringEnabled = reminderPreferences.isRecurringEnabled(userId);
        boolean goalEnabled = reminderPreferences.isGoalEnabled(userId);
        if (!recurringEnabled && !goalEnabled) {
            reminderScheduler.cancelDue(); //cả hai công tắc đã tắt: huỷ lịch và dừng, không xếp lượt mới
            return Result.success();
        }
        try {
            LocalDate today = LocalDate.now();
            if (recurringEnabled) {
                notifyRecurringDue(userId, today);
            }
            if (goalEnabled) {
                notifyGoalsNearDeadline(userId, today, ZoneId.systemDefault());
            }
        } finally {
            //Xếp lượt kế tiếp ở cuối và luôn chạy để chuỗi nhắc không bị đứt (giao diện ReminderScheduler chỉ có applyForCurrentUser để xếp lịch nhắc định kỳ và mục tiêu)
            reminderScheduler.applyForCurrentUser();
        }
        return Result.success();
    }

    //Mẫu định kỳ đến hạn vào ngày mai: một mẫu thì nêu ghi chú và số tiền, nhiều mẫu thì nêu số lượng
    private void notifyRecurringDue(String userId, LocalDate today) {
        List<UpcomingRecurringDue> dueList = findUpcomingRecurringDueUseCase.execute(
                transactionDao.getActiveRecurringTemplates(), userId, today, RECURRING_LEAD_DAYS);
        if (dueList.isEmpty()) {
            return;
        }
        String text;
        if (dueList.size() == 1) {
            TransactionEntity template = dueList.get(0).template;
            String note = template.note == null || template.note.trim().isEmpty() ? NO_NOTE_TEXT : template.note.trim();
            text = "Ngày mai đến hạn giao dịch định kỳ: " + note + " " + formatCurrency(template.amount);
        } else {
            text = "Ngày mai có " + dueList.size() + " giao dịch định kỳ đến hạn";
        }
        notificationHelper.notifyReminder(RECURRING_NOTIFICATION_ID, RECURRING_TITLE, text);
    }

    //Mục tiêu chưa đạt còn đúng 7 hoặc 1 ngày: một mục tiêu thì nêu tên và số ngày, nhiều mục tiêu thì nêu số lượng
    private void notifyGoalsNearDeadline(String userId, LocalDate today, ZoneId zone) {
        List<GoalDeadlineReminder> reminders = findGoalsNearDeadlineUseCase.execute(
                goalDao.getActiveGoalsOnce(), userId, today, zone);
        if (reminders.isEmpty()) {
            return;
        }
        String text;
        if (reminders.size() == 1) {
            GoalDeadlineReminder reminder = reminders.get(0);
            text = "Mục tiêu \"" + reminder.goal.name + "\" còn " + reminder.daysLeft + " ngày đến hạn";
        } else {
            text = "Có " + reminders.size() + " mục tiêu sắp đến hạn";
        }
        notificationHelper.notifyReminder(GOAL_NOTIFICATION_ID, GOAL_TITLE, text);
    }

    //Cùng cách định dạng với GoalDisplayText.formatCurrency: 45000 -> "45.000đ" (DP_02 mục 3.4)
    private static String formatCurrency(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }
}
