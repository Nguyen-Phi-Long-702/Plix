package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FilterTransactionsUseCaseTest {
    private static final String CATEGORY_FOOD = "cat-food";
    private static final String CATEGORY_TRANSPORT = "cat-transport";
    private final FilterTransactionsUseCase useCase = new FilterTransactionsUseCase();
    private List<TransactionEntity> transactions;

    @Before
    public void setUp() {
        transactions = new ArrayList<>();
        transactions.add(newTransaction("tx-1", CATEGORY_FOOD, 1_000L, false));
        transactions.add(newTransaction("tx-2", CATEGORY_TRANSPORT, 2_000L, false));
        transactions.add(newTransaction("tx-3", CATEGORY_FOOD, 3_000L, false));
        transactions.add(newTransaction("tx-4", CATEGORY_FOOD, 4_000L, true)); // đã xoá mềm
        transactions.add(newTransaction("tx-5", CATEGORY_TRANSPORT, 5_000L, false));
    }

    private TransactionEntity newTransaction(String id, String categoryId, long occurredAt, boolean isDeleted) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = 50_000;
        entity.type = "expense";
        entity.categoryId = categoryId;
        entity.occurredAt = occurredAt;
        entity.updatedAt = occurredAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    @Test
    public void execute_noFilters_returnsAllNonDeletedTransactions() {
        List<TransactionEntity> result = useCase.execute(transactions, null, null, null);
        assertEquals(4, result.size());
    }

    @Test
    public void execute_filterByCategory_returnsOnlyMatchingCategoryTransactions() {
        List<TransactionEntity> result = useCase.execute(transactions, CATEGORY_FOOD, null, null);
        assertEquals(2, result.size());
        assertEquals("tx-1", result.get(0).id);
        assertEquals("tx-3", result.get(1).id);
    }

    @Test
    public void execute_filterByDateRange_returnsOnlyTransactionsWithinRange() {
        List<TransactionEntity> result = useCase.execute(transactions, null, 1_500L, 3_500L);
        assertEquals(2, result.size());
        assertEquals("tx-2", result.get(0).id);
        assertEquals("tx-3", result.get(1).id);
    }

    @Test
    public void execute_filterByCategoryAndDateRange_returnsIntersectionOfBothConditions() {
        List<TransactionEntity> result = useCase.execute(transactions, CATEGORY_FOOD, 1_500L, 3_500L);
        assertEquals(1, result.size());
        assertEquals("tx-3", result.get(0).id);
    }

    @Test
    public void execute_alwaysExcludesSoftDeletedTransactions_regardlessOfOtherFilters() {
        List<TransactionEntity> result = useCase.execute(transactions, CATEGORY_FOOD, null, null);
        for (TransactionEntity transaction : result) {
            assertFalse("tx-4".equals(transaction.id));
        }
    }
}