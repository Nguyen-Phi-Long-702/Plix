package com.longvuong.plix.domain.usecase.analytics;

public class CategoryExpense {
    public final String categoryId;
    public final long totalAmount;

    public CategoryExpense(String categoryId, long totalAmount) {
        this.categoryId = categoryId;
        this.totalAmount = totalAmount;
    }
}