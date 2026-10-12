package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddSuggestedTransactionsUseCaseTest {
    private final FakeTransactionRepository fakeRepository = new FakeTransactionRepository();
    private final FormValidator formValidator = new FormValidator();
    private final AddTransactionUseCase addTransactionUseCase = new AddTransactionUseCase(
            fakeRepository, formValidator, fakeCheckBudgetThresholdUseCase());
    private final AddSuggestedTransactionsUseCase useCase =
            new AddSuggestedTransactionsUseCase(addTransactionUseCase, formValidator);

    private com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase fakeCheckBudgetThresholdUseCase() {
        return new com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase(
                new FakeBudgetRepository(),
                fakeRepository,
                new com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase(),
                new com.longvuong.plix.domain.usecase.budget.BudgetThresholdChecker(),
                (budget, spentAfterAmount) -> { });
    }

    private TransactionEntity entity(String id, long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = "expense";
        entity.categoryId = "sys_an_uong";
        entity.note = "";
        entity.paymentMethod = "cash";
        entity.occurredAt = System.currentTimeMillis();
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void execute_allValid_savesEveryEntityInOrderAndReturnsCount() {
        List<Result<Integer>> results = new ArrayList<>();

        useCase.execute(Arrays.asList(entity("tx-1", 50_000L), entity("tx-2", 35_000L)), results::add);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof Result.Success);
        assertEquals(2, (int) ((Result.Success<Integer>) results.get(0)).data);
        assertEquals(2, fakeRepository.insertCount);
        assertEquals("tx-1", fakeRepository.insertedIds.get(0));
        assertEquals("tx-2", fakeRepository.insertedIds.get(1));
    }

    @Test
    public void execute_oneInvalidEntity_savesNothingAndReturnsError() {
        List<Result<Integer>> results = new ArrayList<>();

        useCase.execute(Arrays.asList(entity("tx-1", 50_000L), entity("tx-2", 0L)), results::add);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof Result.Error);
        assertEquals("Số tiền phải lớn hơn 0", ((Result.Error<Integer>) results.get(0)).message);
        assertFalse(fakeRepository.insertCalled); //giao dịch hợp lệ đứng trước cũng không được lưu
        assertEquals(0, fakeRepository.insertCount);
    }

    @Test
    public void execute_emptyList_returnsSuccessWithZeroAndSavesNothing() {
        List<Result<Integer>> results = new ArrayList<>();

        useCase.execute(new ArrayList<>(), results::add);

        assertTrue(results.get(0) instanceof Result.Success);
        assertEquals(0, (int) ((Result.Success<Integer>) results.get(0)).data);
        assertEquals(0, fakeRepository.insertCount);
    }

    @Test
    public void execute_repositoryFailsOnSecondInsert_stopsAndReturnsThatError() {
        fakeRepository.failInsertAtCall = 2;
        List<Result<Integer>> results = new ArrayList<>();

        useCase.execute(Arrays.asList(entity("tx-1", 50_000L), entity("tx-2", 35_000L), entity("tx-3", 10_000L)), results::add);

        assertTrue(results.get(0) instanceof Result.Error);
        assertEquals(2, fakeRepository.insertCount); //dừng ngay sau lần lỗi, không lưu tx-3
    }
}