package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryDao categoryDao;

    @Inject
    public CategoryRepositoryImpl(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    public LiveData<List<CategoryEntity>> getActiveCategories() {
        //Room trả thẳng livedata cho truy vấn đọc
        return categoryDao.getActiveCategories();
    }
}