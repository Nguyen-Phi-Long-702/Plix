package com.longvuong.plix.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.ArrayList;
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
    private static final int MONTHS_PER_YEAR = 12;

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
            generateMissingInstances(transactionDao, System.currentTimeMillis());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sinh giao dich dinh ky that bai", e);
            return Result.retry();
        }
    }

    static void generateMissingInstances(TransactionDao transactionDao, long now) {
        List<TransactionEntity> templates = transactionDao.getActiveRecurringTemplates();
        for (TransactionEntity template : templates) {
            generateMissingInstancesForTemplate(transactionDao, template, now);
        }
    }

    private static void generateMissingInstancesForTemplate(TransactionDao transactionDao, TransactionEntity template, long now) {
        int dayOfMonth = parseDayOfMonth(template.recurrenceRule);
        if (dayOfMonth <= 0) {
            return; //quy tắc lặp lại không hợp lệ, bỏ qua mẫu này
        }

        List<TransactionEntity> existingInstances = new ArrayList<>(
                transactionDao.getInstancesByRecurrenceParentId(template.id));

        int currentPeriod = periodOf(now);
        int startPeriod = firstMissingPeriod(template, existingInstances);

        for (int period = startPeriod; period <= currentPeriod; period++) {
            if (periodHasInstance(existingInstances, period)) {
                continue; //chống trùng: kì này đã có giao dịch sinh ra rồi
            }
            TransactionEntity instance = buildInstance(template, period, dayOfMonth, now);
            transactionDao.insert(instance);
            existingInstances.add(instance); //để các kỳ tiếp theo trong cùng vòng lặp nhận biết đúng
        }
    }

    private static int firstMissingPeriod(TransactionEntity template, List<TransactionEntity> existingInstances) {
        int lastGeneratedPeriod = -1;
        for (TransactionEntity instance : existingInstances) {
            int period = periodOf(instance.occurredAt);
            if (period > lastGeneratedPeriod) {
                lastGeneratedPeriod = period;
            }
        }
        if (lastGeneratedPeriod >= 0) {
            return lastGeneratedPeriod + 1;
        }
        return periodOf(template.occurredAt);
    }

    private static boolean periodHasInstance(List<TransactionEntity> instances, int period) {
        for (TransactionEntity instance : instances) {
            if (periodOf(instance.occurredAt) == period) {
                return true;
            }
        }
        return false;
    }

    private static int periodOf(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return calendar.get(Calendar.YEAR) * MONTHS_PER_YEAR + calendar.get(Calendar.MONTH);
    }

    private static TransactionEntity buildInstance(TransactionEntity template, int period, int dayOfMonth, long now) {
        int year = period / MONTHS_PER_YEAR;
        int month = period % MONTHS_PER_YEAR;
        int clampedDay = Math.min(dayOfMonth, lastDayOfMonth(year, month));

        Calendar occurredCalendar = Calendar.getInstance();
        occurredCalendar.set(year, month, clampedDay, 0, 0, 0);
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

    private static int lastDayOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
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