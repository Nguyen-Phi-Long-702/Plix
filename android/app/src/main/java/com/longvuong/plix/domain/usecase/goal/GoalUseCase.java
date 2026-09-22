package com.longvuong.plix.domain.usecase.goal;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.repository.GoalRepository;

import javax.inject.Inject;

public class GoalUseCase {
    private final GoalRepository goalRepository;

    @Inject
    public GoalUseCase(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public void deleteGoal(GoalEntity entity, String currentUserId, RepositoryCallback<Void> callback) {
        if (entity.userId == null || !entity.userId.equals(currentUserId)) {
            callback.onResult(new Result.Error<>(ErrorType.VALIDATION,
                    "Bạn không có quyền xoá mục tiêu này", null));
            return;
        }
        entity.isDeleted = true;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        goalRepository.update(entity, callback);
    }
}