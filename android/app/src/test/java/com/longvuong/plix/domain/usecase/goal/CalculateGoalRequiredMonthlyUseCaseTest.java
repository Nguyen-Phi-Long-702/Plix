package com.longvuong.plix.domain.usecase.goal;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.GoalEntity;

import org.junit.Test;

import java.time.YearMonth;
import java.time.ZoneId;

public class CalculateGoalRequiredMonthlyUseCaseTest {
    private final CalculateGoalRequiredMonthlyUseCase useCase = new CalculateGoalRequiredMonthlyUseCase();
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

    private long monthsFromNow(int months) {
        return YearMonth.now(ZoneId.systemDefault()).plusMonths(months).atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Test
    public void execute_normalCase_dividesRemainingByMonthsRemaining() {
        GoalRequiredMonthly result = useCase.execute(goal(10_000_000L, 0L, monthsFromNow(5)));
        assertEquals(GoalRequiredMonthly.Case.NORMAL, result.caseType);
        assertEquals(2_000_000L, result.monthlyAmount);
    }

    @Test
    public void execute_afterIncreasingCurrentAmount_requiredMonthlyDecreasesImmediately() {
        GoalEntity goal = goal(10_000_000L, 0L, monthsFromNow(5));
        GoalRequiredMonthly before = useCase.execute(goal);
        assertEquals(2_000_000L, before.monthlyAmount);
        goal.currentAmount = 3_000_000L;
        GoalRequiredMonthly after = useCase.execute(goal);
        assertEquals(GoalRequiredMonthly.Case.NORMAL, after.caseType);
        assertEquals(1_400_000L, after.monthlyAmount);
    }

    @Test
    public void execute_targetEqualsCurrent_returnsAchievedWithZeroAmount() {
        GoalRequiredMonthly result = useCase.execute(goal(10_000_000L, 10_000_000L, monthsFromNow(5)));
        assertEquals(GoalRequiredMonthly.Case.ACHIEVED, result.caseType);
        assertEquals(0L, result.monthlyAmount);
    }

    @Test
    public void execute_currentExceedsTarget_returnsExceeded() {
        GoalRequiredMonthly result = useCase.execute(goal(10_000_000L, 12_000_000L, monthsFromNow(5)));
        assertEquals(GoalRequiredMonthly.Case.EXCEEDED, result.caseType);
    }

    @Test
    public void execute_noMonthsRemainingAndNotAchieved_returnsDueNowWithFullRemaining() {
        GoalRequiredMonthly result = useCase.execute(goal(10_000_000L, 4_000_000L, monthsFromNow(0)));
        assertEquals(GoalRequiredMonthly.Case.DUE_NOW, result.caseType);
        assertEquals(6_000_000L, result.monthlyAmount);
    }

    @Test
    public void execute_remainingNotDivisibleByMonths_roundsUp() {
        GoalRequiredMonthly result = useCase.execute(goal(10_000_000L, 0L, monthsFromNow(3)));
        assertEquals(GoalRequiredMonthly.Case.NORMAL, result.caseType);
        assertEquals(3_333_334L, result.monthlyAmount);
    }
}