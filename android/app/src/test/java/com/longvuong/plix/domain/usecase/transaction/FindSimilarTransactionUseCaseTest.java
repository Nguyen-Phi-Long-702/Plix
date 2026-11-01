package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class FindSimilarTransactionUseCaseTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 29);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private FakeTransactionRepository fakeRepository;
    private FindSimilarTransactionUseCase useCase;
    private List<Result<TransactionEntity>> results;

    @Before
    public void setUp() {
        fakeRepository = new FakeTransactionRepository();
        useCase = new FindSimilarTransactionUseCase(fakeRepository);
        results = new ArrayList<>();
    }

    private long millisOf(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli();
    }

    @Test
    public void execute_passesAmountCategoryAndWholeDayRangeToRepository() {
        useCase.execute(45_000L, "sys_an_uong", millisOf(DAY, 14, 30), results::add);

        assertTrue(fakeRepository.findSimilarCalled);
        assertEquals(45_000L, fakeRepository.lastSimilarAmount);
        assertEquals("sys_an_uong", fakeRepository.lastSimilarCategoryId);
        assertEquals(millisOf(DAY, 0, 0), fakeRepository.lastSimilarDayStart);
        assertEquals(millisOf(DAY.plusDays(1), 0, 0), fakeRepository.lastSimilarDayEnd);
    }

    @Test
    public void execute_justBeforeMidnight_stillUsesSameDay() {
        useCase.execute(45_000L, "sys_an_uong", millisOf(DAY, 23, 59), results::add);

        assertEquals(millisOf(DAY, 0, 0), fakeRepository.lastSimilarDayStart);
        assertEquals(millisOf(DAY.plusDays(1), 0, 0), fakeRepository.lastSimilarDayEnd);
    }

    @Test
    public void execute_repositoryFindsSimilar_returnsThatTransaction() {
        TransactionEntity similar = new TransactionEntity();
        similar.id = "tx-similar";
        fakeRepository.findSimilarResult = new Result.Success<>(similar);

        useCase.execute(45_000L, "sys_an_uong", millisOf(DAY, 9, 0), results::add);

        assertEquals(1, results.size());
        assertSame(similar, ((Result.Success<TransactionEntity>) results.get(0)).data);
    }

    @Test
    public void execute_noSimilarTransaction_returnsSuccessWithNull() {
        useCase.execute(45_000L, "sys_an_uong", millisOf(DAY, 9, 0), results::add);

        assertEquals(1, results.size());
        assertNull(((Result.Success<TransactionEntity>) results.get(0)).data);
    }

    @Test
    public void execute_repositoryError_passesErrorThrough() {
        fakeRepository.findSimilarResult = new Result.Error<>(ErrorType.UNKNOWN, "Lỗi đọc giả lập", null);

        useCase.execute(45_000L, "sys_an_uong", millisOf(DAY, 9, 0), results::add);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof Result.Error);
    }

    @Test
    public void execute_nullCategory_returnsNullWithoutQueryingRepository() {
        useCase.execute(45_000L, null, millisOf(DAY, 9, 0), results::add);

        assertFalse(fakeRepository.findSimilarCalled);
        assertEquals(1, results.size());
        assertNull(((Result.Success<TransactionEntity>) results.get(0)).data);
    }
}