package com.longvuong.plix.domain.usecase.goal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.GoalEntity;

import org.junit.Test;

public class GoalUseCaseTest {
    private final FakeGoalRepository fakeRepository = new FakeGoalRepository();
    private final GoalUseCase useCase = new GoalUseCase(fakeRepository);

    private GoalEntity goal(String userId) {
        GoalEntity entity = new GoalEntity();
        entity.id = "goal-1";
        entity.userId = userId;
        entity.name = "Mua xe";
        entity.targetAmount = 100000000;
        entity.currentAmount = 0;
        entity.deadline = 1767225600000L;
        entity.updatedAt = 1735500000000L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void deleteGoal_ownedByCurrentUser_softDeletesAndReturnsSuccess() {
        GoalEntity entity = goal("user-1");
        useCase.deleteGoal(entity, "user-1", result -> assertTrue(result instanceof Result.Success));
        assertTrue(fakeRepository.updateCalled);
        assertTrue(entity.isDeleted);
    }

    @Test
    public void deleteGoal_ownedByAnotherUser_rejectsBeforeUpdate() {
        GoalEntity entity = goal("user-2");
        useCase.deleteGoal(entity, "user-1", result -> assertTrue(result instanceof Result.Error));
        assertFalse(fakeRepository.updateCalled);
    }
}