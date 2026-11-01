package com.longvuong.plix.presentation.analytics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.CategoryRepository;

import java.util.List;

class FakeCategoryRepository implements CategoryRepository {

    //Chưa có giá trị = Room chưa trả dữ liệu lần đầu; test đặt danh sách bằng setValue
    final MutableLiveData<List<CategoryEntity>> categories = new MutableLiveData<>();

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