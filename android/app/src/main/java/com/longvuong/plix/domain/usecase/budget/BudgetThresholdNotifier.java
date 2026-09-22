package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.data.local.entity.BudgetEntity;

public interface BudgetThresholdNotifier {
    void notifyThresholdCrossed(BudgetEntity budget, long spentAfterAmount);
}