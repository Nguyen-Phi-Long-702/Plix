package com.longvuong.plix.domain.usecase.category;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.CategoryRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FakeCategoryRepository implements CategoryRepository {

    boolean insertCalled;
    boolean updateCalled;
    CategoryEntity lastInserted;
    CategoryEntity lastUpdated;

    private final Map<String, CategoryEntity> systemCategories = new HashMap<>();

    void seedSystemCategory(CategoryEntity entity) {
        systemCategories.put(key(entity.name, entity.type), entity);
    }

    private String key(String name, String type) {
        return name + "|" + type;
    }

    @Override
    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return null; //Không dùng trong test usecase
    }

    @Override
    public void insert(CategoryEntity entity, RepositoryCallback<Void> callback) {
        insertCalled = true;
        lastInserted = entity;
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(CategoryEntity entity, RepositoryCallback<Void> callback) {
        updateCalled = true;
        lastUpdated = entity;
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void findSystemCategoryByNameAndType(String name, String type, RepositoryCallback<CategoryEntity> callback) {
        callback.onResult(new Result.Success<>(systemCategories.get(key(name, type))));
    }
}