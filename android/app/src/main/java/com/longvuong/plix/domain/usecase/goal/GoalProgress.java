package com.longvuong.plix.domain.usecase.goal;

public class GoalProgress {
    public final int percent;
    public final GoalStatus status;
    public final GoalRequiredMonthly requiredMonthly;

    public GoalProgress(int percent, GoalStatus status, GoalRequiredMonthly requiredMonthly) {
        this.percent = percent;
        this.status = status;
        this.requiredMonthly = requiredMonthly;
    }
}