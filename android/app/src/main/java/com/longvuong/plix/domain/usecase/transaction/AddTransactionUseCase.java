package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.validation.FormValidator;

import javax.inject.Inject;

public class AddTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final FormValidator formValidator;

    @Inject
    public AddTransactionUseCase(TransactionRepository transactionRepository, FormValidator formValidator) {
        this.transactionRepository = transactionRepository;
        this.formValidator = formValidator;
    }

    public void execute(TransactionEntity entity, RepositoryCallback<Void> callback) {
        Result<Void> validation = formValidator.validateTransaction(entity.amount, entity.note, entity.occurredAt);
        if (validation instanceof Result.Error) {
            callback.onResult(validation);
            return;
        }
        transactionRepository.insert(entity, callback);
    }
}