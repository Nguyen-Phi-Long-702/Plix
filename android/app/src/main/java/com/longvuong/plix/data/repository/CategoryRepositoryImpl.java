package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryDao categoryDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    @Inject
    public CategoryRepositoryImpl(CategoryDao categoryDao, AppExecutors appExecutors, ErrorMapper errorMapper) {
        this.categoryDao = categoryDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
    }

    @Override
    public LiveData<List<CategoryEntity>> getActiveCategories() {
        //Room trả thẳng livedata cho truy vấn đọc
        return categoryDao.getActiveCategories();
    }

    @Override
    public void insert(CategoryEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                categoryDao.insert(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void update(CategoryEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                categoryDao.update(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void findSystemCategoryByNameAndType(String name, String type, RepositoryCallback<CategoryEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                CategoryEntity found = categoryDao.findSystemCategoryByNameAndType(name, type);
                appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(found)));
            } catch (Exception e) {
                Result<CategoryEntity> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    private void notifySuccess(RepositoryCallback<Void> callback) {
        appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(null)));
    }

    private void notifyError(RepositoryCallback<Void> callback, Exception e) {
        Result<Void> error = errorMapper.mapThrowable(e);
        appExecutors.mainThread().execute(() -> callback.onResult(error));
    }
}