package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.List;
import java.util.Map;

//Dữ liệu màn Home khi đã có giao dịch: tổng quan tháng hiện tại, giao dịch gần đây và ngân sách đang dùng nhiều nhất
public class HomeUiModel {
    public enum BudgetLevel {
        NORMAL,
        WARNING, //đã chạm ngưỡng cảnh báo
        DANGER //đã chi từ 100% hạn mức trở lên
    }

    public static class BudgetSummary {
        public final String name;
        public final int percent;
        public final long spentAmount;
        public final long limitAmount;
        public final BudgetLevel level;

        public BudgetSummary(String name, int percent, long spentAmount, long limitAmount, BudgetLevel level) {
            this.name = name;
            this.percent = percent;
            this.spentAmount = spentAmount;
            this.limitAmount = limitAmount;
            this.level = level;
        }
    }

    public final String monthLabel; //vd "Tháng 9/2026"
    public final long totalIncome;
    public final long totalExpense;
    public final long netBalance; //thu - chi, có thể âm
    public final List<TransactionEntity> recentTransactions;
    public final Map<String, String> categoryNamesById;
    @Nullable
    public final BudgetSummary budget; //null khi tháng này chưa có ngân sách nào
    @Nullable
    public final Long overallBudgetRemaining; //hạn mức ngân sách tổng - đã chi; âm khi vượt; null khi tháng này chưa có ngân sách tổng

    public HomeUiModel(String monthLabel, long totalIncome, long totalExpense, long netBalance,
                       List<TransactionEntity> recentTransactions, Map<String, String> categoryNamesById,
                       @Nullable BudgetSummary budget, @Nullable Long overallBudgetRemaining) {
        this.monthLabel = monthLabel;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netBalance = netBalance;
        this.recentTransactions = recentTransactions;
        this.categoryNamesById = categoryNamesById;
        this.budget = budget;
        this.overallBudgetRemaining = overallBudgetRemaining;
    }
}