package com.longvuong.plix.domain.usecase.budget;

import javax.inject.Inject;

public class BudgetThresholdChecker {
    @Inject
    public BudgetThresholdChecker() {
    }

    public boolean justCrossedThreshold(long limitAmount, int thresholdPercent, long spentBeforeAmount, long spentAfterAmount) {
        if (limitAmount <= 0) {
            return false;
        }
        int percentBefore = (int) Math.round(spentBeforeAmount * 100.0 / limitAmount);
        int percentAfter = (int) Math.round(spentAfterAmount * 100.0 / limitAmount);
        return percentBefore < thresholdPercent && percentAfter >= thresholdPercent;
    }
}