package com.longvuong.plix.domain.usecase.goal;

import com.longvuong.plix.data.local.entity.GoalEntity;

import javax.inject.Inject;

public class GoalStatusUseCase {
    @Inject
    public GoalStatusUseCase() {
    }

    public GoalStatus execute(GoalEntity goal) {
        return execute(goal, System.currentTimeMillis());
    }

    public GoalStatus execute(GoalEntity goal, long nowEpochMs) {
        if (goal.currentAmount >= goal.targetAmount) {
            return GoalStatus.ACHIEVED;
        }
        if (nowEpochMs > goal.deadline) {
            return GoalStatus.EXPIRED;
        }
        return GoalStatus.ACTIVE;
    }
}