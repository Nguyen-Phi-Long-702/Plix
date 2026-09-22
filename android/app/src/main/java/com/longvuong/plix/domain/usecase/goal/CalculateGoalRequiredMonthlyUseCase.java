package com.longvuong.plix.domain.usecase.goal;

import com.longvuong.plix.data.local.entity.GoalEntity;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

public class CalculateGoalRequiredMonthlyUseCase {
    @Inject
    public CalculateGoalRequiredMonthlyUseCase() {
    }

    public GoalRequiredMonthly execute(GoalEntity goal) {
        long remainingAmount = goal.targetAmount - goal.currentAmount;
        if (remainingAmount == 0) {
            return new GoalRequiredMonthly(GoalRequiredMonthly.Case.ACHIEVED, 0L);
        }
        if (remainingAmount < 0) {
            return new GoalRequiredMonthly(GoalRequiredMonthly.Case.EXCEEDED, 0L);
        }

        long monthsRemaining = monthsRemaining(goal.deadline);
        if (monthsRemaining <= 0) {
            return new GoalRequiredMonthly(GoalRequiredMonthly.Case.DUE_NOW, remainingAmount);
        }

        long monthlyAmount = (long) Math.ceil(remainingAmount / (double) monthsRemaining);
        return new GoalRequiredMonthly(GoalRequiredMonthly.Case.NORMAL, monthlyAmount);
    }

    private long monthsRemaining(long deadlineEpochMs) {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth currentYearMonth = YearMonth.now(zone);
        YearMonth deadlineYearMonth = YearMonth.from(Instant.ofEpochMilli(deadlineEpochMs).atZone(zone));
        long months = ChronoUnit.MONTHS.between(currentYearMonth, deadlineYearMonth);
        return Math.max(months, 0);
    }
}