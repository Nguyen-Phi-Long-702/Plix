package com.longvuong.plix.domain.usecase.analytics;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

//So sánh chi tiêu tháng này (từ ngày 1 đến hôm nay) với cùng kỳ tháng trước (FIN-14). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 5.4: lọc, hai cửa sổ cùng số ngày, gom theo danh mục bằng AggregateAnalyticsUseCase, chênh lệch và phần trăm
@Singleton
public class CompareMonthlyExpenseUseCase {
    private static final Comparator<String> CATEGORY_ID_ORDER = Comparator.nullsFirst(Comparator.naturalOrder());

    //Thứ tự dòng: |chênh lệch| giảm dần, đồng hạng theo categoryId tăng dần (categoryId null xếp trước)
    private static final Comparator<CategoryComparison> ROW_ORDER = (a, b) -> {
        long absA = Math.abs(a.deltaAmount);
        long absB = Math.abs(b.deltaAmount);
        if (absA != absB) {
            return Long.compare(absB, absA);
        }
        return CATEGORY_ID_ORDER.compare(a.categoryId, b.categoryId);
    };

    private final AggregateAnalyticsUseCase aggregateAnalyticsUseCase;

    @Inject
    public CompareMonthlyExpenseUseCase(AggregateAnalyticsUseCase aggregateAnalyticsUseCase) {
        this.aggregateAnalyticsUseCase = aggregateAnalyticsUseCase;
    }

    public MonthComparison execute(List<TransactionEntity> all, String userId, LocalDate today, ZoneId zone) {
        //Cửa sổ tháng này: từ ngày 1 đến today. Cửa sổ tháng trước: từ ngày 1 đến min(ngày hôm nay, số ngày của tháng trước)
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);
        LocalDate currentFrom = today.withDayOfMonth(1);
        LocalDate previousFrom = previousMonth.atDay(1);
        LocalDate previousTo = previousMonth.atDay(Math.min(today.getDayOfMonth(), previousMonth.lengthOfMonth()));

        List<TransactionEntity> currentWindow = new ArrayList<>();
        List<TransactionEntity> previousWindow = new ArrayList<>();
        if (all != null) {
            for (TransactionEntity transaction : all) {
                if (!"expense".equals(transaction.type)
                        || transaction.isDeleted
                        || !Objects.equals(userId, transaction.userId)) {
                    continue; //chỉ lấy giao dịch chi, chưa xoá, của đúng người dùng
                }
                LocalDate date = Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate();
                if (!date.isBefore(currentFrom) && !date.isAfter(today)) {
                    currentWindow.add(transaction);
                } else if (!date.isBefore(previousFrom) && !date.isAfter(previousTo)) {
                    previousWindow.add(transaction);
                }
            }
        }

        AnalyticsSummary current = aggregateAnalyticsUseCase.execute(currentWindow);
        AnalyticsSummary previous = aggregateAnalyticsUseCase.execute(previousWindow);

        Map<String, Long> currentByCategory = toAmountMap(current.categoryExpenses);
        Map<String, Long> previousByCategory = toAmountMap(previous.categoryExpenses);
        Set<String> categoryIds = new HashSet<>(currentByCategory.keySet());
        categoryIds.addAll(previousByCategory.keySet());

        List<CategoryComparison> rows = new ArrayList<>();
        for (String categoryId : categoryIds) {
            long currentAmount = currentByCategory.getOrDefault(categoryId, 0L);
            long previousAmount = previousByCategory.getOrDefault(categoryId, 0L);
            long delta = currentAmount - previousAmount;
            rows.add(new CategoryComparison(categoryId, currentAmount, previousAmount, delta, percentOf(delta, previousAmount)));
        }
        Collections.sort(rows, ROW_ORDER);

        long deltaTotal = current.totalExpense - previous.totalExpense;
        return new MonthComparison(current.totalExpense, previous.totalExpense, deltaTotal,
                percentOf(deltaTotal, previous.totalExpense), rows, currentFrom, today, previousFrom, previousTo);
    }

    //Gom danh sách CategoryExpense thành map categoryId -> tổng chi (categoryId có thể là null)
    private static Map<String, Long> toAmountMap(List<CategoryExpense> expenses) {
        Map<String, Long> amounts = new HashMap<>();
        for (CategoryExpense expense : expenses) {
            amounts.put(expense.categoryId, expense.totalAmount);
        }
        return amounts;
    }

    //Làm tròn nửa lên của delta * 100 / previous (floorDiv(delta * 200 + previous, previous * 2)); null khi previous = 0
    private static Integer percentOf(long delta, long previous) {
        if (previous == 0L) {
            return null;
        }
        return (int) Math.floorDiv(delta * 200L + previous, previous * 2L);
    }
}