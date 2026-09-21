package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.GoalDao;
import com.longvuong.plix.data.local.entity.GoalEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GoalRepositoryImpl implements GoalRepository {
    private final GoalDao goalDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    @Inject
    public GoalRepositoryImpl(GoalDao goalDao, AppExecutors appExecutors, ErrorMapper errorMapper) {
        this.goalDao = goalDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
    }

    @Override
    public LiveData<List<GoalEntity>> getActiveGoals() {
        //Room trả thẳng livedata cho truy vấn đọc
        return goalDao.getActiveGoals();
    }

    @Override
    public void insert(GoalEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                goalDao.insert(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void update(GoalEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                goalDao.update(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void getById(String id, RepositoryCallback<GoalEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                GoalEntity found = goalDao.getById(id);
                appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(found)));
            } catch (Exception e) {
                Result<GoalEntity> error = errorMapper.mapThrowable(e);
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