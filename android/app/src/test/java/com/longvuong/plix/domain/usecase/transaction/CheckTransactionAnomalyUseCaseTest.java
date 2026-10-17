package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AnomalyResult;

import org.junit.Before;
import org.junit.Test;

public class CheckTransactionAnomalyUseCaseTest {
    private FakeAiRepository fakeAiRepository;
    private FakeTransactionRepository fakeTransactionRepository;
    private CheckTransactionAnomalyUseCase useCase;
    private Result<AnomalyCheckOutcome> captured;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository = new FakeTransactionRepository();
        useCase = new CheckTransactionAnomalyUseCase(fakeAiRepository, fakeTransactionRepository);
        captured = null;
    }

    private void run() {
        useCase.execute("user-1", "tx-new", "cat-an-uong", 350000L, result -> captured = result);
    }

    private AnomalyCheckOutcome outcome() {
        assertTrue(captured instanceof Result.Success);
        return ((Result.Success<AnomalyCheckOutcome>) captured).data;
    }

    @Test
    public void high_withOtherPending_setsPendingFlag() {
        fakeAiRepository.anomalyResult = new Result.Success<>(new AnomalyResult(AnomalyResult.Level.HIGH, "Cao"));
        fakeTransactionRepository.countPendingResult = new Result.Success<>(2);

        run();

        assertEquals(AnomalyResult.Level.HIGH, outcome().result.level);
        assertTrue(outcome().hasOtherPendingInCategory);
        assertEquals("cat-an-uong", fakeAiRepository.lastCategoryId);
        assertEquals(350000L, fakeAiRepository.lastAmount);
        assertEquals("tx-new", fakeTransactionRepository.lastExcludedTransactionId);
    }

    @Test
    public void low_withoutOtherPending_noPendingFlag() {
        fakeAiRepository.anomalyResult = new Result.Success<>(new AnomalyResult(AnomalyResult.Level.LOW, "Thấp"));
        fakeTransactionRepository.countPendingResult = new Result.Success<>(0);

        run();

        assertEquals(AnomalyResult.Level.LOW, outcome().result.level);
        assertFalse(outcome().hasOtherPendingInCategory);
    }

    @Test
    public void normal_doesNotQueryPending() {
        fakeAiRepository.anomalyResult = new Result.Success<>(new AnomalyResult(AnomalyResult.Level.NORMAL, null));

        run();

        assertEquals(AnomalyResult.Level.NORMAL, outcome().result.level);
        assertFalse(outcome().hasOtherPendingInCategory);
        assertFalse(fakeTransactionRepository.countPendingCalled);
    }

    @Test
    public void insufficientData_doesNotQueryPending() {
        fakeAiRepository.anomalyResult = new Result.Success<>(new AnomalyResult(AnomalyResult.Level.INSUFFICIENT_DATA, null));

        run();

        assertEquals(AnomalyResult.Level.INSUFFICIENT_DATA, outcome().result.level);
        assertFalse(fakeTransactionRepository.countPendingCalled);
    }

    @Test
    public void pendingCountFails_stillReturnsResultWithoutFlag() {
        fakeAiRepository.anomalyResult = new Result.Success<>(new AnomalyResult(AnomalyResult.Level.HIGH, "Cao"));
        fakeTransactionRepository.countPendingResult = new Result.Error<>(ErrorType.UNKNOWN, "Loi DB", null);

        run();

        assertEquals(AnomalyResult.Level.HIGH, outcome().result.level);
        assertFalse(outcome().hasOtherPendingInCategory);
    }

    @Test
    public void apiError_isPropagated() {
        fakeAiRepository.anomalyResult = new Result.Error<>(ErrorType.NETWORK, "Mat mang", null);

        run();

        assertTrue(captured instanceof Result.Error);
        assertEquals(ErrorType.NETWORK, ((Result.Error<AnomalyCheckOutcome>) captured).type);
        assertFalse(fakeTransactionRepository.countPendingCalled);
    }
}