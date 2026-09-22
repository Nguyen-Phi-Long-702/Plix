package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.CategoryRepository;

import java.util.List;

class FakeCategoryRepository implements CategoryRepository {
    private final MutableLiveData<List<CategoryEntity>> categories;

    FakeCategoryRepository(List<CategoryEntity> initial) {
        categories = new MutableLiveData<>(initial);
    }

    @Override
    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categories;
    }

    @Override
    public void insert(CategoryEntity entity, RepositoryCallback<Void> callback) {
    }

    @Override
    public void update(CategoryEntity entity, RepositoryCallback<Void> callback) {
    }

    @Override
    public void findSystemCategoryByNameAndType(String name, String type, RepositoryCallback<CategoryEntity> callback) {
    }
}