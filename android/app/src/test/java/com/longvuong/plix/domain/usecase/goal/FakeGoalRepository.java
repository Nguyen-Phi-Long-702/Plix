package com.longvuong.plix.domain.usecase.goal;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.repository.GoalRepository;

import java.util.List;

class FakeGoalRepository implements GoalRepository {
    boolean updateCalled;
    GoalEntity lastUpdated;

    @Override
    public LiveData<List<GoalEntity>> getActiveGoals() {
        return null; //Không dùng trong test usecase
    }

    @Override
    public void insert(GoalEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(GoalEntity entity, RepositoryCallback<Void> callback) {
        updateCalled = true;
        lastUpdated = entity;
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<GoalEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }
}