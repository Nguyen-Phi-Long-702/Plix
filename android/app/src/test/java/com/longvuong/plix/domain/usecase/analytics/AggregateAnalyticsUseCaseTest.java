package com.longvuong.plix.domain.usecase.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AggregateAnalyticsUseCaseTest {
    private final AggregateAnalyticsUseCase useCase = new AggregateAnalyticsUseCase();

    private long millisOf(String isoDate) {
        return LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private TransactionEntity transaction(String type, String categoryId, long amount, String date, boolean isDeleted) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-" + date + "-" + type + "-" + amount;
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = type;
        entity.categoryId = categoryId;
        entity.occurredAt = millisOf(date);
        entity.updatedAt = entity.occurredAt;
        entity.syncStatus = "pending";
        entity.isDeleted = isDeleted;
        return entity;
    }

    @Test
    public void execute_emptyList_returnsZeroedSummaryWithoutCrash() {
        AnalyticsSummary summary = useCase.execute(new ArrayList<>());
        assertTrue(summary.categoryExpenses.isEmpty());
        assertTrue(summary.monthlyTrend.isEmpty());
        assertEquals(0, summary.totalIncome);
        assertEquals(0, summary.totalExpense);
        assertEquals(0, summary.savingsRatePercent);
    }

    @Test
    public void execute_nullList_returnsZeroedSummaryWithoutCrash() {
        AnalyticsSummary summary = useCase.execute(null);
        assertTrue(summary.categoryExpenses.isEmpty());
        assertEquals(0, summary.totalIncome);
    }

    @Test
    public void execute_groupsExpenseByCategory_sortedDescendingByAmount() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", "sys_an_uong", 200_000L, "2026-09-05", false));
        transactions.add(transaction("expense", "sys_an_uong", 100_000L, "2026-09-10", false));
        transactions.add(transaction("expense", "sys_di_chuyen", 150_000L, "2026-09-12", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(2, summary.categoryExpenses.size());
        assertEquals("sys_an_uong", summary.categoryExpenses.get(0).categoryId);
        assertEquals(300_000L, summary.categoryExpenses.get(0).totalAmount);
        assertEquals("sys_di_chuyen", summary.categoryExpenses.get(1).categoryId);
        assertEquals(150_000L, summary.categoryExpenses.get(1).totalAmount);
    }

    @Test
    public void execute_expenseWithoutCategory_groupedUnderNullKey() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", null, 50_000L, "2026-09-05", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(1, summary.categoryExpenses.size());
        assertNull(summary.categoryExpenses.get(0).categoryId);
        assertEquals(50_000L, summary.categoryExpenses.get(0).totalAmount);
    }

    @Test
    public void execute_excludesDeletedTransactions() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", "sys_an_uong", 200_000L, "2026-09-05", true));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertTrue(summary.categoryExpenses.isEmpty());
        assertEquals(0, summary.totalExpense);
    }

    @Test
    public void execute_monthlyTrend_sortedChronologicallyWithIncomeAndExpenseSeparated() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", null, 10_000_000L, "2026-08-01", false));
        transactions.add(transaction("expense", "sys_an_uong", 3_000_000L, "2026-08-15", false));
        transactions.add(transaction("income", null, 12_000_000L, "2026-09-01", false));
        transactions.add(transaction("expense", "sys_di_chuyen", 4_000_000L, "2026-09-20", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(2, summary.monthlyTrend.size());
        assertEquals("2026-08", summary.monthlyTrend.get(0).yearMonth);
        assertEquals(10_000_000L, summary.monthlyTrend.get(0).incomeAmount);
        assertEquals(3_000_000L, summary.monthlyTrend.get(0).expenseAmount);
        assertEquals("2026-09", summary.monthlyTrend.get(1).yearMonth);
        assertEquals(12_000_000L, summary.monthlyTrend.get(1).incomeAmount);
        assertEquals(4_000_000L, summary.monthlyTrend.get(1).expenseAmount);
    }

    @Test
    public void execute_totalIncomeAndExpense_summedAcrossWholeList() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", null, 13_500_000L, "2026-09-01", false));
        transactions.add(transaction("expense", "sys_an_uong", 4_170_000L, "2026-09-05", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(13_500_000L, summary.totalIncome);
        assertEquals(4_170_000L, summary.totalExpense);
    }

    @Test
    public void execute_savingsRate_calculatedCorrectly() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", null, 13_500_000L, "2026-09-01", false));
        transactions.add(transaction("expense", "sys_an_uong", 4_170_000L, "2026-09-05", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(69, summary.savingsRatePercent);
    }

    @Test
    public void execute_savingsRate_zeroIncome_doesNotDivideByZero() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", "sys_an_uong", 100_000L, "2026-09-05", false));
        AnalyticsSummary summary = useCase.execute(transactions);
        assertEquals(0, summary.savingsRatePercent);
    }
}