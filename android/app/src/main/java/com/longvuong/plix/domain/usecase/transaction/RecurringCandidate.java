package com.longvuong.plix.domain.usecase.transaction;

public class RecurringCandidate {
    public final String type;
    public final String categoryId;
    public final long amount;
    public final int dayOfMonth;
    public final String note;
    public final int consecutiveMonths;
    public final long lastOccurredAt;

    public RecurringCandidate(String type, String categoryId, long amount, int dayOfMonth, String note,
                              int consecutiveMonths, long lastOccurredAt) {
        this.type = type;
        this.categoryId = categoryId;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.note = note;
        this.consecutiveMonths = consecutiveMonths;
        this.lastOccurredAt = lastOccurredAt;
    }

    //Khoá nhóm dạng type|categoryId, dùng cho danh sách "Bỏ qua" (FIN-19)
    public String getKey() {
        return buildKey(type, categoryId);
    }

    static String buildKey(String type, String categoryId) {
        return type + "|" + categoryId;
    }
}