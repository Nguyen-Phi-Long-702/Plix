package com.longvuong.plix.domain.usecase.analytics;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AggregateAnalyticsUseCase {
    @Inject
    public AggregateAnalyticsUseCase() {
    }

    public AnalyticsSummary execute(List<TransactionEntity> transactions) {
        Map<String, Long> expenseByCategory = new HashMap<>();
        Map<String, long[]> monthlyTotals = new TreeMap<>(); // TreeMap để year-month tự sắp xếp theo thời gian
        long totalIncome = 0L;
        long totalExpense = 0L;

        if (transactions != null) {
            for (TransactionEntity transaction : transactions) {
                if (transaction.isDeleted) {
                    continue;
                }

                String yearMonth = YearMonth.from(
                        Instant.ofEpochMilli(transaction.occurredAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                ).toString();
                long[] monthTotals = monthlyTotals.computeIfAbsent(yearMonth, key -> new long[2]);

                if ("income".equals(transaction.type)) {
                    totalIncome += transaction.amount;
                    monthTotals[0] += transaction.amount;
                } else if ("expense".equals(transaction.type)) {
                    totalExpense += transaction.amount;
                    monthTotals[1] += transaction.amount;
                    expenseByCategory.merge(transaction.categoryId, transaction.amount, Long::sum);
                }
            }
        }

        List<CategoryExpense> categoryExpenses = new ArrayList<>();
        for (Map.Entry<String, Long> entry : expenseByCategory.entrySet()) {
            categoryExpenses.add(new CategoryExpense(entry.getKey(), entry.getValue()));
        }
        Collections.sort(categoryExpenses, (a, b) -> Long.compare(b.totalAmount, a.totalAmount));

        List<MonthlyTrend> monthlyTrend = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : monthlyTotals.entrySet()) {
            monthlyTrend.add(new MonthlyTrend(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        int savingsRatePercent = totalIncome > 0 ? (int) Math.round((totalIncome - totalExpense) * 100.0 / totalIncome) : 0;

        return new AnalyticsSummary(categoryExpenses, monthlyTrend, totalIncome, totalExpense, savingsRatePercent);
    }
}