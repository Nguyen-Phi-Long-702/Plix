package com.longvuong.plix.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

@HiltWorker
public class RecurringTransactionWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "recurring_transaction_worker";

    private static final String TAG = "RecurringTxWorker";
    private static final String RECURRENCE_PREFIX = "MONTHLY:";

    private final TransactionDao transactionDao;

    @AssistedInject
    public RecurringTransactionWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParameters,
            TransactionDao transactionDao) {
        super(context, workerParameters);
        this.transactionDao = transactionDao;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            generateForCurrentPeriod(transactionDao, System.currentTimeMillis());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sinh giao dich dinh ky that bai", e);
            return Result.retry();
        }
    }

    static void generateForCurrentPeriod(TransactionDao transactionDao, long now) {
        List<TransactionEntity> templates = transactionDao.getActiveRecurringTemplates();
        for (TransactionEntity template : templates) {
            generateForTemplate(transactionDao, template, now);
        }
    }

    private static void generateForTemplate(TransactionDao transactionDao, TransactionEntity template, long now) {
        int dayOfMonth = parseDayOfMonth(template.recurrenceRule);
        if (dayOfMonth <= 0) {
            return; //quy tắc lặp lại k hợp lệ, bỏ qua template này
        }

        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTimeInMillis(now);
        int targetYear = nowCalendar.get(Calendar.YEAR);
        int targetMonth = nowCalendar.get(Calendar.MONTH);

        if (instanceExistsForPeriod(transactionDao, template.id, targetYear, targetMonth)) {
            return; //chống trùng: kì này đã có giao dịch sinh ra rồi
        }

        transactionDao.insert(buildInstance(template, targetYear, targetMonth, dayOfMonth, now));
    }

    private static boolean instanceExistsForPeriod(TransactionDao transactionDao, String templateId, int year, int month) {
        List<TransactionEntity> existingInstances = transactionDao.getInstancesByRecurrenceParentId(templateId);
        Calendar calendar = Calendar.getInstance();
        for (TransactionEntity instance : existingInstances) {
            calendar.setTimeInMillis(instance.occurredAt);
            if (calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month) {
                return true;
            }
        }
        return false;
    }

    private static TransactionEntity buildInstance(TransactionEntity template, int year, int month, int dayOfMonth, long now) {
        Calendar occurredCalendar = Calendar.getInstance();
        occurredCalendar.set(year, month, dayOfMonth, 0, 0, 0);
        occurredCalendar.set(Calendar.MILLISECOND, 0);

        TransactionEntity instance = new TransactionEntity();
        instance.id = UUID.randomUUID().toString();
        instance.userId = template.userId;
        instance.amount = template.amount;
        instance.type = template.type;
        instance.categoryId = template.categoryId;
        instance.note = template.note;
        instance.paymentMethod = template.paymentMethod;
        instance.occurredAt = occurredCalendar.getTimeInMillis();
        instance.isRecurring = false;
        instance.recurrenceRule = null;
        instance.recurrenceParentId = template.id;
        instance.updatedAt = now;
        instance.syncStatus = "pending";
        instance.isDeleted = false;
        return instance;
    }

    static int parseDayOfMonth(String recurrenceRule) {
        if (recurrenceRule == null || !recurrenceRule.startsWith(RECURRENCE_PREFIX)) {
            return -1;
        }
        String dayPart = recurrenceRule.substring(RECURRENCE_PREFIX.length());
        try {
            int day = Integer.parseInt(dayPart);
            return (day >= 1 && day <= 31) ? day : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}