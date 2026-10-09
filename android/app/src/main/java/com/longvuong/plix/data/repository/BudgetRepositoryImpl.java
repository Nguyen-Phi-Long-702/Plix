package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.BudgetDao;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.sync.SyncScheduler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BudgetRepositoryImpl implements BudgetRepository {
    private final BudgetDao budgetDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;
    private final SyncScheduler syncScheduler;

    @Inject
    public BudgetRepositoryImpl(BudgetDao budgetDao, AppExecutors appExecutors, ErrorMapper errorMapper, SyncScheduler syncScheduler) {
        this.budgetDao = budgetDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
        this.syncScheduler = syncScheduler;
    }

    @Override
    public LiveData<List<BudgetEntity>> getActiveByPeriod(String period) {
        return budgetDao.getActiveByPeriod(period);
    }

    @Override
    public void insert(BudgetEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                BudgetEntity deletedWithSameKey = entity.categoryId != null
                        ? budgetDao.findDeletedCategoryBudget(entity.userId, entity.period, entity.categoryId) : null;
                if (deletedWithSameKey != null) {
                    //Dòng cũ chỉ xoá mềm nên vẫn chiếm khoá duy nhất (user, kỳ, danh mục): dùng lại đúng dòng đó (cùng id) thay vì thêm dòng mới
                    entity.id = deletedWithSameKey.id;
                    budgetDao.update(entity);
                } else {
                    budgetDao.insert(entity);
                }
                syncScheduler.requestSync();
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
                syncScheduler.requestSync();
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

    @Override
    public void findCategoryBudgetByUserAndPeriod(String userId, String period, String categoryId, RepositoryCallback<BudgetEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                BudgetEntity found = budgetDao.findCategoryBudgetByUserAndPeriod(userId, period, categoryId);
                appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(found)));
            } catch (Exception e) {
                Result<BudgetEntity> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }
}