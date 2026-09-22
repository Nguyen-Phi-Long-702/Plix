package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase;
import com.longvuong.plix.domain.validation.FormValidator;

import javax.inject.Inject;

public class AddTransactionUseCase {
    private final TransactionRepository transactionRepository;
    private final FormValidator formValidator;
    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;

    @Inject
    public AddTransactionUseCase(TransactionRepository transactionRepository, FormValidator formValidator, CheckBudgetThresholdUseCase checkBudgetThresholdUseCase) {
        this.transactionRepository = transactionRepository;
        this.formValidator = formValidator;
        this.checkBudgetThresholdUseCase = checkBudgetThresholdUseCase;
    }

    public void execute(TransactionEntity entity, RepositoryCallback<Void> callback) {
        Result<Void> validation = formValidator.validateTransaction(entity.amount, entity.note, entity.occurredAt);
        if (validation instanceof Result.Error) {
            callback.onResult(validation);
            return;
        }
        transactionRepository.insert(entity, result -> {
            callback.onResult(result);
            if (result instanceof Result.Success) {
                checkBudgetThresholdUseCase.checkAfterAdd(entity);
            }
        });
    }
}