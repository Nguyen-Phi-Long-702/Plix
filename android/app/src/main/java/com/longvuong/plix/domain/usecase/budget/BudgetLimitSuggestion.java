package com.longvuong.plix.domain.usecase.budget;

//Kết quả gợi ý hạn mức ngân sách theo trung bình chi tiêu các tháng liền trước (FIN-16)
public class BudgetLimitSuggestion {
    public final long suggestedAmount;
    public final int monthsUsed; //số tháng thực sự có dữ liệu chi (1 đến 3), để giao diện ghi "Trung bình N tháng gần nhất"

    public BudgetLimitSuggestion(long suggestedAmount, int monthsUsed) {
        this.suggestedAmount = suggestedAmount;
        this.monthsUsed = monthsUsed;
    }
}