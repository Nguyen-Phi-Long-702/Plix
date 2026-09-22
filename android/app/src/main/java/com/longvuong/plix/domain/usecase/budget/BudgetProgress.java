package com.longvuong.plix.domain.usecase.budget;

public class BudgetProgress {
    public final long spentAmount;
    public final int percent;

    public BudgetProgress(long spentAmount, int percent) {
        this.spentAmount = spentAmount;
        this.percent = percent;
    }
}