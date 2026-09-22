package com.longvuong.plix.domain.usecase.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.repository.BudgetRepository;

import java.util.ArrayList;
import java.util.List;

class FakeBudgetRepository implements BudgetRepository {
    private final List<BudgetEntity> insertedBudgets = new ArrayList<>();
    private final List<BudgetEntity> updatedBudgets = new ArrayList<>();

    @Override
    public LiveData<List<BudgetEntity>> getActiveByPeriod(String period) {
        return new MutableLiveData<>(new ArrayList<>());
    }

    @Override
    public void insert(BudgetEntity entity, RepositoryCallback<Void> callback) {
        insertedBudgets.add(entity);
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(BudgetEntity entity, RepositoryCallback<Void> callback) {
        updatedBudgets.add(entity);
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<BudgetEntity> callback) {
        for (BudgetEntity budget : insertedBudgets) {
            if (budget.id.equals(id)) {
                callback.onResult(new Result.Success<>(budget));
                return;
            }
        }
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void findOverallBudgetByUserAndPeriod(String userId, String period, RepositoryCallback<BudgetEntity> callback) {
        for (BudgetEntity budget : insertedBudgets) {
            if (budget.userId.equals(userId) && budget.period.equals(period) && budget.categoryId == null) {
                callback.onResult(new Result.Success<>(budget));
                return;
            }
        }
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void findCategoryBudgetByUserAndPeriod(String userId, String period, String categoryId, RepositoryCallback<BudgetEntity> callback) {
        for (BudgetEntity budget : insertedBudgets) {
            if (budget.userId.equals(userId) && budget.period.equals(period) && categoryId.equals(budget.categoryId)) {
                callback.onResult(new Result.Success<>(budget));
                return;
            }
        }
        callback.onResult(new Result.Success<>(null));
    }

    List<BudgetEntity> getInsertedBudgets() {
        return insertedBudgets;
    }

    List<BudgetEntity> getUpdatedBudgets() {
        return updatedBudgets;
    }
}