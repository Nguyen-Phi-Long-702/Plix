package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.repository.BudgetRepository;

import java.util.ArrayList;
import java.util.List;

class FakeBudgetRepository implements BudgetRepository {
    @Override
    public LiveData<List<BudgetEntity>> getActiveByPeriod(String period) {
        return new MutableLiveData<>(new ArrayList<>());
    }

    @Override
    public void insert(BudgetEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(BudgetEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<BudgetEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void findOverallBudgetByUserAndPeriod(String userId, String period, RepositoryCallback<BudgetEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void findCategoryBudgetByUserAndPeriod(String userId, String period, String categoryId, RepositoryCallback<BudgetEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }
}