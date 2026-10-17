package com.longvuong.plix.domain.usecase.analytics;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CalculateMonthToDateNetUseCaseTest {
    private final CalculateMonthToDateNetUseCase useCase = new CalculateMonthToDateNetUseCase();
    private final LocalDate today = LocalDate.of(2026, 10, 17);

    private long millisOf(String isoDate) {
        return LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private TransactionEntity transaction(String type, long amount, String date, boolean isDeleted) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-" + date + "-" + type + "-" + amount;
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = type;
        entity.occurredAt = millisOf(date);
        entity.updatedAt = entity.occurredAt;
        entity.syncStatus = "pending";
        entity.isDeleted = isDeleted;
        return entity;
    }

    @Test
    public void execute_emptyList_returnsZeroSeriesUpToToday() {
        List<Long> series = useCase.execute(new ArrayList<>(), today);

        assertEquals(17, series.size());
        assertEquals(0L, (long) series.get(0));
        assertEquals(0L, (long) series.get(16));
    }

    @Test
    public void execute_nullList_returnsZeroSeriesUpToToday() {
        List<Long> series = useCase.execute(null, today);

        assertEquals(17, series.size());
        assertEquals(0L, (long) series.get(16));
    }

    @Test
    public void execute_incomeAndExpense_accumulatesNetPerDay() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", 10_000_000L, "2026-10-01", false));
        transactions.add(transaction("expense", 200_000L, "2026-10-03", false));
        transactions.add(transaction("expense", 100_000L, "2026-10-03", false));
        transactions.add(transaction("expense", 50_000L, "2026-10-17", false));

        List<Long> series = useCase.execute(transactions, today);

        assertEquals(17, series.size());
        assertEquals(10_000_000L, (long) series.get(0)); //ngày 1
        assertEquals(10_000_000L, (long) series.get(1)); //ngày 2: không có giao dịch → giữ nguyên
        assertEquals(9_700_000L, (long) series.get(2)); //ngày 3: trừ 200k + 100k
        assertEquals(9_700_000L, (long) series.get(15)); //ngày 16
        assertEquals(9_650_000L, (long) series.get(16)); //ngày 17 (hôm nay) vẫn được tính
    }

    @Test
    public void execute_deletedTransaction_isIgnored() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", 999_000L, "2026-10-05", true));

        List<Long> series = useCase.execute(transactions, today);

        assertEquals(0L, (long) series.get(16));
    }

    @Test
    public void execute_transactionsOfOtherMonths_areIgnored() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("expense", 5_000L, "2026-09-30", false));
        transactions.add(transaction("income", 7_000L, "2026-11-01", false));

        List<Long> series = useCase.execute(transactions, today);

        assertEquals(0L, (long) series.get(16));
    }

    @Test
    public void execute_futureDaysOfSameMonth_areIgnored() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", 9_000L, "2026-10-20", false));

        List<Long> series = useCase.execute(transactions, today);

        assertEquals(17, series.size());
        assertEquals(0L, (long) series.get(16));
    }

    @Test
    public void execute_firstDayOfMonth_returnsSingleElement() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("income", 10_000_000L, "2026-10-01", false));

        List<Long> series = useCase.execute(transactions, LocalDate.of(2026, 10, 1));

        assertEquals(1, series.size());
        assertEquals(10_000_000L, (long) series.get(0));
    }
}