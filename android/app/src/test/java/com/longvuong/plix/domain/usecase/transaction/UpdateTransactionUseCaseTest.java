package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import org.junit.Test;

public class UpdateTransactionUseCaseTest {

    private final FakeTransactionRepository fakeRepository = new FakeTransactionRepository();
    private final FormValidator formValidator = new FormValidator();
    private final UpdateTransactionUseCase useCase = new UpdateTransactionUseCase(fakeRepository, formValidator);

    private TransactionEntity existingEntity() {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = "user-1";
        entity.amount = 50000;
        entity.type = "expense";
        entity.note = "Ăn trưa";
        entity.occurredAt = System.currentTimeMillis();
        entity.updatedAt = 1_000_000L; //giá trị cũ, phải bị ghi đè sau khi update
        entity.syncStatus = "synced"; //giả lập bản ghi đã đồng bộ trước đó
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_validEntity_setsUpdatedAtAndPendingStatus() {
        TransactionEntity entity = existingEntity();

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Success));

        assertTrue(fakeRepository.updateCalled);
        assertEquals("pending", fakeRepository.lastUpdated.syncStatus);
        assertTrue(fakeRepository.lastUpdated.updatedAt > 1_000_000L);
    }

    @Test
    public void execute_invalidAmount_rejectsBeforeReachingRepository() {
        TransactionEntity entity = existingEntity();
        entity.amount = -100;

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Error));

        assertFalse(fakeRepository.updateCalled);
    }
}