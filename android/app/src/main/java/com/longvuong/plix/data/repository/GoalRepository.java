package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.GoalEntity;

import java.util.List;

public interface GoalRepository {
    LiveData<List<GoalEntity>> getActiveGoals();

    void insert(GoalEntity entity, RepositoryCallback<Void> callback);

    void update(GoalEntity entity, RepositoryCallback<Void> callback);

    void getById(String id, RepositoryCallback<GoalEntity> callback);
}