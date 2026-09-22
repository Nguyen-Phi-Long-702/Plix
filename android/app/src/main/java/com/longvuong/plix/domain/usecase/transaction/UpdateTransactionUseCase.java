package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase;
import com.longvuong.plix.domain.validation.FormValidator;

import javax.inject.Inject;

public class UpdateTransactionUseCase {
    private final TransactionRepository transactionRepository;
    private final FormValidator formValidator;
    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;

    @Inject
    public UpdateTransactionUseCase(TransactionRepository transactionRepository, FormValidator formValidator, CheckBudgetThresholdUseCase checkBudgetThresholdUseCase) {
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
        //Đọc bản ghi cũ từ room trước khi ghi đè, để có dữ liệu trước khi sửa cho việc check ngưỡng
        transactionRepository.getById(entity.id, oldResult -> {
            TransactionEntity oldSnapshot = oldResult instanceof Result.Success ? ((Result.Success<TransactionEntity>) oldResult).data : null;
            entity.updatedAt = System.currentTimeMillis();
            entity.syncStatus = "pending";
            transactionRepository.update(entity, updateResult -> {
                callback.onResult(updateResult);
                if (updateResult instanceof Result.Success && oldSnapshot != null) {
                    checkBudgetThresholdUseCase.checkAfterUpdate(oldSnapshot, entity);
                }
            });
        });
    }
}