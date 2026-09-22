package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

public class DeleteTransactionUseCaseTest {

    private final FakeTransactionRepository fakeRepository = new FakeTransactionRepository();
    private final DeleteTransactionUseCase useCase = new DeleteTransactionUseCase(fakeRepository);

    @Test
    public void execute_marksSoftDeleteFieldsAndUpdates() {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = "user-1";
        entity.amount = 50000;
        entity.type = "expense";
        entity.occurredAt = System.currentTimeMillis();
        entity.updatedAt = 1_000_000L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;

        useCase.execute(entity, result -> assertTrue(result instanceof Result.Success));

        assertTrue(fakeRepository.updateCalled);
        assertTrue(fakeRepository.lastUpdated.isDeleted);
        assertEquals("pending", fakeRepository.lastUpdated.syncStatus);
        assertTrue(fakeRepository.lastUpdated.updatedAt > 1_000_000L);
    }
}