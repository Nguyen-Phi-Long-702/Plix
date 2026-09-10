package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

public interface CategoryRepository {
    LiveData<List<CategoryEntity>> getActiveCategories();
}