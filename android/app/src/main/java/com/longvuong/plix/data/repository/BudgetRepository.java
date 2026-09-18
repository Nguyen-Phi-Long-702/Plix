package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.BudgetEntity;

import java.util.List;

public interface BudgetRepository {
    LiveData<List<BudgetEntity>> getActiveByPeriod(String period);

    void insert(BudgetEntity entity, RepositoryCallback<Void> callback);

    void update(BudgetEntity entity, RepositoryCallback<Void> callback);

    void getById(String id, RepositoryCallback<BudgetEntity> callback);

    void findOverallBudgetByUserAndPeriod(String userId, String period, RepositoryCallback<BudgetEntity> callback);
}