package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;

import com.longvuong.plix.data.local.entity.CategoryEntity;

public class ParseNlItem {
    public final long amount; //VNĐ nguyên đã quy đổi, vd "50k" -> 50000

    @Nullable
    public final CategoryEntity category; //danh mục đã tra từ Room; null nếu category_id không khớp danh mục nào còn dùng được (không có hoặc đã bị xoá)

    public final float confidence;

    public ParseNlItem(long amount, @Nullable CategoryEntity category, float confidence) {
        this.amount = amount;
        this.category = category;
        this.confidence = confidence;
    }
}