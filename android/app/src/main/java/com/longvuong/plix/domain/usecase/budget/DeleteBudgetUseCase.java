package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.repository.BudgetRepository;

import javax.inject.Inject;

public class DeleteBudgetUseCase {
    private final BudgetRepository budgetRepository;

    @Inject
    public DeleteBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    //Xoá mềm: không bao giờ xoá vật lý, đánh dấu pending để SyncWorker đẩy tombstone lên máy chủ
    public void execute(BudgetEntity entity, String currentUserId, RepositoryCallback<Void> callback) {
        if (!entity.userId.equals(currentUserId)) {
            callback.onResult(new Result.Error<>(ErrorType.VALIDATION,
                    "Bạn không có quyền xoá ngân sách này", null));
            return;
        }
        entity.isDeleted = true;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        budgetRepository.update(entity, callback);
    }
}