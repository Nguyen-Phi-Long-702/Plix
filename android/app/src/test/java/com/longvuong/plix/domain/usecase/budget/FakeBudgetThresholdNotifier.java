package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.data.local.entity.BudgetEntity;

import java.util.ArrayList;
import java.util.List;

class FakeBudgetThresholdNotifier implements BudgetThresholdNotifier {
    final List<BudgetEntity> notifiedBudgets = new ArrayList<>();
    final List<Long> notifiedSpentAmounts = new ArrayList<>();

    @Override
    public void notifyThresholdCrossed(BudgetEntity budget, long spentAfterAmount) {
        notifiedBudgets.add(budget);
        notifiedSpentAmounts.add(spentAfterAmount);
    }
}