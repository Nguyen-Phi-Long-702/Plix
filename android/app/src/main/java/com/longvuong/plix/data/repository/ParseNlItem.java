package com.longvuong.plix.data.repository;

public class ParseNlItem {
    public final long amount; //VNĐ nguyên đã quy đổi, vd "50k" -> 50000
    public final String categoryId; //category_id do bộ phân tích trả về
    public final float confidence;

    public ParseNlItem(long amount, String categoryId, float confidence) {
        this.amount = amount;
        this.categoryId = categoryId;
        this.confidence = confidence;
    }
}