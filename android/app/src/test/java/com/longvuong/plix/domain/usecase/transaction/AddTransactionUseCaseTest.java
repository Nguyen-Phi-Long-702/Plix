package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import org.junit.Test;

public class AddTransactionUseCaseTest {

    private final FakeTransactionRepository fakeRepository = new FakeTransactionRepository();
    private final FormValidator formValidator = new FormValidator();
    private final AddTransactionUseCase useCase = new AddTransactionUseCase(fakeRepository, formValidator);

    private TransactionEntity validEntity() {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = "user-1";
        entity.amount = 50000;
        entity.type = "expense";
        entity.note = "Ăn trưa";
        entity.occurredAt = System.currentTimeMillis();
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_validEntity_insertsAndReturnsSuccess() {
        TransactionEntity entity = validEntity();

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Success));

        assertTrue(fakeRepository.insertCalled);
    }

    @Test
    public void execute_amountZero_rejectsBeforeReachingRepository() {
        TransactionEntity entity = validEntity();
        entity.amount = 0;

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Error));

        assertFalse(fakeRepository.insertCalled);
    }

    @Test
    public void execute_noteTooLong_rejectsBeforeReachingRepository() {
        TransactionEntity entity = validEntity();
        entity.note = "a".repeat(501);

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Error));

        assertFalse(fakeRepository.insertCalled);
    }
}