package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CalculateBudgetProgressUseCaseTest {
    private final CalculateBudgetProgressUseCase useCase = new CalculateBudgetProgressUseCase();
    private static final long START = CalculateBudgetProgressUseCase.periodStartAtMillis("2026-09");
    private static final long END = CalculateBudgetProgressUseCase.periodEndAtMillis("2026-09");

    private BudgetEntity budget(String categoryId, long limitAmount) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = "user-1";
        entity.period = "2026-09";
        entity.categoryId = categoryId;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = 80;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private TransactionEntity transaction(String type, String categoryId, long amount, long occurredAt, boolean isDeleted) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-" + occurredAt + "-" + amount;
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = type;
        entity.categoryId = categoryId;
        entity.occurredAt = occurredAt;
        entity.isRecurring = false;
        entity.updatedAt = occurredAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    @Test
    public void execute_categoryBudget_onlySumsMatchingCategoryExpense() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", "sys_an_uong", 200000, START + 1000, false));
        transactions.add(transaction("expense", "sys_di_chuyen", 100000, START + 1000, false));
        BudgetProgress progress = useCase.execute(budget("sys_an_uong", 1000000), transactions, START, END);
        assertEquals(200000, progress.spentAmount);
        assertEquals(20, progress.percent);
    }

    @Test
    public void execute_overallBudget_includesUncategorizedTransactions() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", "sys_an_uong", 200000, START + 1000, false));
        transactions.add(transaction("expense", null, 100000, START + 1000, false));
        BudgetProgress progress = useCase.execute(budget(null, 1000000), transactions, START, END);
        assertEquals(300000, progress.spentAmount);
        assertEquals(30, progress.percent);
    }

    @Test
    public void execute_excludesDeletedTransactions() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", null, 200000, START + 1000, true));
        BudgetProgress progress = useCase.execute(budget(null, 1000000), transactions, START, END);
        assertEquals(0, progress.spentAmount);
    }

    @Test
    public void execute_excludesIncomeTransactions() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", null, 5000000, START + 1000, false));
        BudgetProgress progress = useCase.execute(budget(null, 1000000), transactions, START, END);
        assertEquals(0, progress.spentAmount);
    }

    @Test
    public void execute_excludesTransactionsOutsidePeriod() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", null, 200000, END + 1000, false));
        BudgetProgress progress = useCase.execute(budget(null, 1000000), transactions, START, END);
        assertEquals(0, progress.spentAmount);
    }

    @Test
    public void execute_limitAmountZero_doesNotDivideByZero() {
        BudgetProgress progress = useCase.execute(budget(null, 0), new ArrayList<>(), START, END);
        assertEquals(0, progress.percent);
    }

    @Test
    public void periodStartAndEndAtMillis_coverWholeMonth() {
        long start = CalculateBudgetProgressUseCase.periodStartAtMillis("2026-02");
        long end = CalculateBudgetProgressUseCase.periodEndAtMillis("2026-02");
        assertEquals(1, Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth());
        assertEquals(28, Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth());
    }
}