package com.longvuong.plix.data.repository;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.GoalEntity;

public interface GoalRepository {
    void insert(GoalEntity entity, RepositoryCallback<Void> callback);

    void update(GoalEntity entity, RepositoryCallback<Void> callback);

    void getById(String id, RepositoryCallback<GoalEntity> callback);
}