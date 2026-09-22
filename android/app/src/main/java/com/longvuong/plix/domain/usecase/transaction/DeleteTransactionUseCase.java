package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import javax.inject.Inject;

public class DeleteTransactionUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public DeleteTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void execute(TransactionEntity entity, RepositoryCallback<Void> callback) {
        entity.isDeleted = true;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        transactionRepository.update(entity, callback);
    }
}