package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.domain.validation.FormValidator;

import javax.inject.Inject;

public class AddBudgetUseCase {
    private final BudgetRepository budgetRepository;
    private final FormValidator formValidator;

    @Inject
    public AddBudgetUseCase(BudgetRepository budgetRepository, FormValidator formValidator) {
        this.budgetRepository = budgetRepository;
        this.formValidator = formValidator;
    }

    public void execute(BudgetEntity entity, RepositoryCallback<Void> callback) {
        Result<Void> periodValidation = formValidator.validatePeriod(entity.period);
        if (periodValidation instanceof Result.Error) {
            callback.onResult(periodValidation);
            return;
        }
        Result<Void> limitValidation = formValidator.validateAmount(entity.limitAmount);
        if (limitValidation instanceof Result.Error) {
            callback.onResult(limitValidation);
            return;
        }

        if (entity.categoryId != null) {
            //Ngân sách theo danh mục: ràng buộc unique ở db đã đủ chặn trùng nên insert thẳng
            budgetRepository.insert(entity, callback);
            return;
        }

        budgetRepository.findOverallBudgetByUserAndPeriod(entity.userId, entity.period, result -> {
            if (result instanceof Result.Error) {
                callback.onResult(propagateError((Result.Error<BudgetEntity>) result));
                return;
            }
            BudgetEntity existingOverallBudget = ((Result.Success<BudgetEntity>) result).data;
            if (existingOverallBudget != null) {
                callback.onResult(new Result.Error<>(ErrorType.VALIDATION,
                        "Đã có ngân sách tổng cho tháng này.", null));
                return;
            }
            budgetRepository.insert(entity, callback);
        });
    }

    private static <T, R> Result<R> propagateError(Result.Error<T> error) {
        return new Result.Error<>(error.type, error.message, error.cause);
    }
}