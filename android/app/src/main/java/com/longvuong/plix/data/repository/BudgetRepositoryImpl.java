package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.BudgetDao;
import com.longvuong.plix.data.local.entity.BudgetEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BudgetRepositoryImpl implements BudgetRepository {
    private final BudgetDao budgetDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    @Inject
    public BudgetRepositoryImpl(BudgetDao budgetDao, AppExecutors appExecutors, ErrorMapper errorMapper) {
        this.budgetDao = budgetDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
    }

    @Override
    public LiveData<List<BudgetEntity>> getActiveByPeriod(String period) {
        return budgetDao.getActiveByPeriod(period);
    }

    @Override
    public void insert(BudgetEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                budgetDao.insert(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void update(BudgetEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                budgetDao.update(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void getById(String id, RepositoryCallback<BudgetEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                BudgetEntity found = budgetDao.getById(id);
                appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(found)));
            } catch (Exception e) {
                Result<BudgetEntity> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void findOverallBudgetByUserAndPeriod(String userId, String period, RepositoryCallback<BudgetEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                BudgetEntity found = budgetDao.findOverallBudgetByUserAndPeriod(userId, period);
                appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(found)));
            } catch (Exception e) {
                Result<BudgetEntity> error = errorMapper.mapThrowable(e);
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