package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

public interface CategoryRepository {

    LiveData<List<CategoryEntity>> getActiveCategories();

    void insert(CategoryEntity entity, RepositoryCallback<Void> callback);

    void update(CategoryEntity entity, RepositoryCallback<Void> callback);

    void findSystemCategoryByNameAndType(String name, String type, RepositoryCallback<CategoryEntity> callback);
}