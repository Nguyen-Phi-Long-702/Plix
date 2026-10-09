package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;

import org.junit.Test;

public class DeleteBudgetUseCaseTest {
    private final FakeBudgetRepository fakeRepository = new FakeBudgetRepository();
    private final DeleteBudgetUseCase useCase = new DeleteBudgetUseCase(fakeRepository);

    private BudgetEntity budget(String userId) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = userId;
        entity.period = "2026-10";
        entity.categoryId = "sys_an_uong";
        entity.limitAmount = 3000000;
        entity.thresholdPercent = 80;
        entity.updatedAt = 1_000_000L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_ownedByCurrentUser_marksSoftDeleteFieldsAndUpdates() {
        BudgetEntity entity = budget("user-1");

        useCase.execute(entity, "user-1", result -> assertTrue(result instanceof Result.Success));

        assertEquals(1, fakeRepository.getUpdatedBudgets().size());
        BudgetEntity updated = fakeRepository.getUpdatedBudgets().get(0);
        assertTrue(updated.isDeleted);
        assertEquals("pending", updated.syncStatus);
        assertTrue(updated.updatedAt > 1_000_000L);
    }

    @Test
    public void execute_ownedByAnotherUser_rejectsBeforeUpdate() {
        BudgetEntity entity = budget("user-2");

        useCase.execute(entity, "user-1", result -> assertTrue(result instanceof Result.Error));

        assertTrue(fakeRepository.getUpdatedBudgets().isEmpty());
    }
}