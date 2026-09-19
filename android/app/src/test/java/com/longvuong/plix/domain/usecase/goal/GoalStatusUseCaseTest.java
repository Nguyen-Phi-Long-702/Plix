package com.longvuong.plix.domain.usecase.goal;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.GoalEntity;

import org.junit.Test;

public class GoalStatusUseCaseTest {
    private final GoalStatusUseCase useCase = new GoalStatusUseCase();
    private static final long ONE_DAY_MS = 1000L * 60 * 60 * 24;
    private GoalEntity goal(long targetAmount, long currentAmount, long deadline) {
        GoalEntity entity = new GoalEntity();
        entity.id = "goal-1";
        entity.userId = "user-1";
        entity.name = "Mua xe máy";
        entity.targetAmount = targetAmount;
        entity.currentAmount = currentAmount;
        entity.deadline = deadline;
        entity.updatedAt = 0L;
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_currentBelowTargetAndDeadlineInFuture_returnsActive() {
        long now = System.currentTimeMillis();
        assertEquals(GoalStatus.ACTIVE, useCase.execute(goal(10_000_000L, 3_000_000L, now + 30 * ONE_DAY_MS), now));
    }

    @Test
    public void execute_currentEqualsTarget_returnsAchieved() {
        long now = System.currentTimeMillis();
        assertEquals(GoalStatus.ACHIEVED, useCase.execute(goal(10_000_000L, 10_000_000L, now + 30 * ONE_DAY_MS), now));
    }

    @Test
    public void execute_currentExceedsTarget_returnsAchieved() {
        long now = System.currentTimeMillis();
        assertEquals(GoalStatus.ACHIEVED, useCase.execute(goal(10_000_000L, 12_000_000L, now + 30 * ONE_DAY_MS), now));
    }

    @Test
    public void execute_deadlinePassedAndNotAchieved_returnsExpired() {
        long now = System.currentTimeMillis();
        assertEquals(GoalStatus.EXPIRED, useCase.execute(goal(10_000_000L, 3_000_000L, now - ONE_DAY_MS), now));
    }

    @Test
    public void execute_deadlinePassedButAchieved_stillReturnsAchieved() {
        long now = System.currentTimeMillis();
        assertEquals(GoalStatus.ACHIEVED, useCase.execute(goal(10_000_000L, 10_000_000L, now - ONE_DAY_MS), now));
    }

    @Test
    public void execute_currentReducedBelowTargetAfterAchieved_returnsActiveAgain() {
        long now = System.currentTimeMillis();
        GoalEntity goal = goal(10_000_000L, 10_000_000L, now + 30 * ONE_DAY_MS);
        assertEquals(GoalStatus.ACHIEVED, useCase.execute(goal, now));

        goal.currentAmount = 5_000_000L;
        assertEquals(GoalStatus.ACTIVE, useCase.execute(goal, now));
    }
}