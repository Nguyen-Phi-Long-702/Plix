package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.domain.validation.FormValidator;

import javax.inject.Inject;

public class UpdateBudgetUseCase {
    private final BudgetRepository budgetRepository;
    private final FormValidator formValidator;

    @Inject
    public UpdateBudgetUseCase(BudgetRepository budgetRepository, FormValidator formValidator) {
        this.budgetRepository = budgetRepository;
        this.formValidator = formValidator;
    }

    public void execute(BudgetEntity entity, RepositoryCallback<Void> callback) {
        Result<Void> limitValidation = formValidator.validateAmount(entity.limitAmount);
        if (limitValidation instanceof Result.Error) {
            callback.onResult(limitValidation);
            return;
        }
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        budgetRepository.update(entity, callback);
    }
}