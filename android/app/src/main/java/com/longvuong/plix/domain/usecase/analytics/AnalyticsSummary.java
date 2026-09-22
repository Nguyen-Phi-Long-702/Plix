package com.longvuong.plix.domain.usecase.analytics;

import java.util.List;

public class AnalyticsSummary {
    public final List<CategoryExpense> categoryExpenses;
    public final List<MonthlyTrend> monthlyTrend;
    public final long totalIncome;
    public final long totalExpense;
    public final int savingsRatePercent;

    public AnalyticsSummary(List<CategoryExpense> categoryExpenses, List<MonthlyTrend> monthlyTrend, long totalIncome, long totalExpense, int savingsRatePercent) {
        this.categoryExpenses = categoryExpenses;
        this.monthlyTrend = monthlyTrend;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.savingsRatePercent = savingsRatePercent;
    }
}