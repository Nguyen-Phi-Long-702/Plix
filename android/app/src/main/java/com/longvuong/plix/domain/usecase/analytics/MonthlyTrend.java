package com.longvuong.plix.domain.usecase.analytics;

public class MonthlyTrend {
    public final String yearMonth;
    public final long incomeAmount;
    public final long expenseAmount;

    public MonthlyTrend(String yearMonth, long incomeAmount, long expenseAmount) {
        this.yearMonth = yearMonth;
        this.incomeAmount = incomeAmount;
        this.expenseAmount = expenseAmount;
    }
}