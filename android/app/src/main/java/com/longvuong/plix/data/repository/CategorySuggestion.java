package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;

import com.longvuong.plix.data.local.entity.CategoryEntity;

public class CategorySuggestion {

    @Nullable
    public final CategoryEntity category; //null nếu category_id trả về không khớp category nào trong Room

    public final float confidence;

    public CategorySuggestion(@Nullable CategoryEntity category, float confidence) {
        this.category = category;
        this.confidence = confidence;
    }
}