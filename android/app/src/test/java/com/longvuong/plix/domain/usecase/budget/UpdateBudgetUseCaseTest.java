package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import org.junit.Before;
import org.junit.Test;

public class UpdateBudgetUseCaseTest {
    private FakeBudgetRepository fakeBudgetRepository;
    private UpdateBudgetUseCase updateBudgetUseCase;

    @Before
    public void setUp() {
        fakeBudgetRepository = new FakeBudgetRepository();
        updateBudgetUseCase = new UpdateBudgetUseCase(fakeBudgetRepository, new FormValidator());
    }

    private BudgetEntity existingBudget(long limitAmount) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = "user-1";
        entity.period = "2026-09";
        entity.categoryId = null;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = 80;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_validLimitAmount_updatesSuccessfully() {
        updateBudgetUseCase.execute(existingBudget(5000000), result -> assertTrue(result instanceof Result.Success));
        assertEquals(1, fakeBudgetRepository.getUpdatedBudgets().size());
    }

    @Test
    public void execute_validLimitAmount_setsUpdatedAtAndSyncStatusPending() {
        BudgetEntity entity = existingBudget(5000000);
        updateBudgetUseCase.execute(entity, result -> {});
        assertEquals("pending", entity.syncStatus);
        assertTrue(entity.updatedAt > 0L);
    }

    @Test
    public void execute_invalidLimitAmount_rejectsAndDoesNotUpdate() {
        updateBudgetUseCase.execute(existingBudget(0), result -> assertTrue(result instanceof Result.Error));
        assertEquals(0, fakeBudgetRepository.getUpdatedBudgets().size());
    }
}