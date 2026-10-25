package com.longvuong.plix.presentation.budget;

//Mức cảnh báo của 1 ngân sách trên danh sách: quyết định màu thanh tiến độ, màu phần trăm và dòng "Đã vượt ngưỡng cảnh báo"
public enum BudgetProgressLevel {
    NORMAL,
    WARNING,
    DANGER;

    public static BudgetProgressLevel of(int percent, int thresholdPercent) {
        if (percent >= 100) {
            return DANGER;
        }
        if (percent >= thresholdPercent) {
            return WARNING;
        }
        return NORMAL;
    }
}