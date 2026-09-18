package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import javax.inject.Inject;

public class CalculateBudgetProgressUseCase {
    @Inject
    public CalculateBudgetProgressUseCase() {
    }

    public BudgetProgress execute(BudgetEntity budget, List<TransactionEntity> transactions, long periodStartAtMillis, long periodEndAtMillis) {
        long spentAmount = 0L;
        for (TransactionEntity transaction : transactions) {
            if (transaction.isDeleted) {
                continue;
            }
            if (!"expense".equals(transaction.type)) {
                continue;
            }
            if (transaction.occurredAt < periodStartAtMillis || transaction.occurredAt > periodEndAtMillis) {
                continue;
            }
            if (budget.categoryId != null && !budget.categoryId.equals(transaction.categoryId)) {
                continue;
            }
            spentAmount += transaction.amount;
        }
        int percent = budget.limitAmount <= 0 ? 0 : (int) Math.round(spentAmount * 100.0 / budget.limitAmount);
        return new BudgetProgress(spentAmount, percent);
    }

    public static long periodStartAtMillis(String period) {
        ZoneId zone = ZoneId.systemDefault();
        return YearMonth.parse(period).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public static long periodEndAtMillis(String period) {
        ZoneId zone = ZoneId.systemDefault();
        return YearMonth.parse(period).atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();
    }
}