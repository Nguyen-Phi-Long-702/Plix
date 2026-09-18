package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import org.junit.Before;
import org.junit.Test;

public class AddBudgetUseCaseTest {
    private FakeBudgetRepository fakeBudgetRepository;
    private AddBudgetUseCase addBudgetUseCase;

    @Before
    public void setUp() {
        fakeBudgetRepository = new FakeBudgetRepository();
        addBudgetUseCase = new AddBudgetUseCase(fakeBudgetRepository, new FormValidator());
    }

    private BudgetEntity budget(String id, String period, long limitAmount) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.period = period;
        entity.categoryId = null;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = 80;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_firstOverallBudgetOfMonth_insertsSuccessfully() {
        addBudgetUseCase.execute(budget("budget-1", "2026-09", 5000000), result -> assertTrue(result instanceof Result.Success));
        assertEquals(1, fakeBudgetRepository.getInsertedBudgets().size());
    }

    @Test
    public void execute_secondOverallBudgetSameMonth_rejectsWithCorrectMessage() {
        addBudgetUseCase.execute(budget("budget-1", "2026-09", 5000000), result -> {});
        addBudgetUseCase.execute(budget("budget-2", "2026-09", 3000000), result -> {
            assertTrue(result instanceof Result.Error);
            assertEquals("Đã có ngân sách tổng cho tháng này.", ((Result.Error<Void>) result).message);
        });
        assertEquals(1, fakeBudgetRepository.getInsertedBudgets().size()); //không insert thêm
    }

    @Test
    public void execute_overallBudgetDifferentMonth_insertsSuccessfully() {
        addBudgetUseCase.execute(budget("budget-1", "2026-09", 5000000), result -> {});
        addBudgetUseCase.execute(budget("budget-2", "2026-10", 3000000), result -> assertTrue(result instanceof Result.Success));
        assertEquals(2, fakeBudgetRepository.getInsertedBudgets().size());
    }

    @Test
    public void execute_categoryBudget_insertsWithoutDuplicateCheckEvenIfOverallBudgetExists() {
        addBudgetUseCase.execute(budget("budget-1", "2026-09", 5000000), result -> {});
        BudgetEntity categoryBudget = budget("budget-2", "2026-09", 2000000);
        categoryBudget.categoryId = "sys_an_uong";
        addBudgetUseCase.execute(categoryBudget, result -> assertTrue(result instanceof Result.Success));
        assertEquals(2, fakeBudgetRepository.getInsertedBudgets().size());
    }

    @Test
    public void execute_invalidLimitAmount_rejectsAndDoesNotInsert() {
        addBudgetUseCase.execute(budget("budget-1", "2026-09", 0), result -> assertTrue(result instanceof Result.Error));
        assertEquals(0, fakeBudgetRepository.getInsertedBudgets().size());
    }

    @Test
    public void execute_invalidPeriod_rejectsAndDoesNotInsert() {
        addBudgetUseCase.execute(budget("budget-1", "2026-13", 5000000), result -> assertTrue(result instanceof Result.Error));
        assertEquals(0, fakeBudgetRepository.getInsertedBudgets().size());
    }
}