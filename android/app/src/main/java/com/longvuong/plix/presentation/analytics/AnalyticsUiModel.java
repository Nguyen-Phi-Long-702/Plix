package com.longvuong.plix.presentation.analytics;

import java.util.List;

//Dữ liệu màn Thống kê khi khoảng thời gian đang chọn có giao dịch; đã chuẩn bị sẵn cho biểu đồ tròn, cột chồng và các thẻ tổng
public class AnalyticsUiModel {
    public static class CategorySlice {
        public final String label;
        public final long amount;
        public final int percent;
        public final int colorIndex; //thứ tự màu trong bảng màu biểu đồ (0–4); lát thứ 5 dùng màu trung tính

        public CategorySlice(String label, long amount, int percent, int colorIndex) {
            this.label = label;
            this.amount = amount;
            this.percent = percent;
            this.colorIndex = colorIndex;
        }
    }

    public static class MonthBar {
        public final String label;
        public final long expenseAmount; //phần chi, vẽ ở dưới
        public final long remainingAmount; //phần thu còn lại sau khi chi, vẽ ở trên; bằng 0 khi chi ≥ thu

        public MonthBar(String label, long expenseAmount, long remainingAmount) {
            this.label = label;
            this.expenseAmount = expenseAmount;
            this.remainingAmount = remainingAmount;
        }
    }

    public final List<CategorySlice> categorySlices; //rỗng khi khoảng thời gian này không có khoản chi nào
    public final List<MonthBar> monthBars;
    public final long totalIncome;
    public final long totalExpense;
    public final int savingsRatePercent;
    public final String centerLabel;

    public AnalyticsUiModel(List<CategorySlice> categorySlices, List<MonthBar> monthBars, long totalIncome,
                            long totalExpense, int savingsRatePercent, String centerLabel) {
        this.categorySlices = categorySlices;
        this.monthBars = monthBars;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.savingsRatePercent = savingsRatePercent;
        this.centerLabel = centerLabel;
    }
}