package com.longvuong.plix.domain.usecase.goal;

public class GoalRequiredMonthly {
    public enum Case {
        NORMAL,   //còn thiếu tiền, còn tháng
        ACHIEVED, //đã đạt mục tiêu
        EXCEEDED, //đã vượt mục tiêu
        DUE_NOW   //hết tháng còn lại nhưng chưa đạt, cần bổ sung ngay toàn bộ phần còn thiếu
    }

    public final Case caseType;
    public final long monthlyAmount;

    public GoalRequiredMonthly(Case caseType, long monthlyAmount) {
        this.caseType = caseType;
        this.monthlyAmount = monthlyAmount;
    }
}