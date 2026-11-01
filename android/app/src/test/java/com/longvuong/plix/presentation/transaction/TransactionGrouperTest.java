package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionGrouperTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    private TransactionEntity transaction(String id, LocalDate date, int hour, int minute) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = 50_000;
        entity.type = "expense";
        entity.occurredAt = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        entity.updatedAt = entity.occurredAt;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private String titleAt(List<TransactionListItem> items, int index) {
        return ((TransactionListItem.Header) items.get(index)).title;
    }

    private String idAt(List<TransactionListItem> items, int index) {
        return ((TransactionListItem.Row) items.get(index)).transaction.id;
    }

    @Test
    public void group_emptyList_returnsEmptyList() {
        assertTrue(TransactionGrouper.group(Collections.emptyList(), TODAY).isEmpty());
    }

    @Test
    public void group_sameDay_returnsOneHeaderThenRowsInOriginalOrder() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("tx-1", TODAY, 18, 0));
        transactions.add(transaction("tx-2", TODAY, 8, 0));

        List<TransactionListItem> items = TransactionGrouper.group(transactions, TODAY);

        assertEquals(3, items.size());
        assertEquals("Hôm nay", titleAt(items, 0));
        assertEquals("tx-1", idAt(items, 1));
        assertEquals("tx-2", idAt(items, 2));
    }

    @Test
    public void group_todayYesterdayAndOlderDay_usesRelativeTitlesThenDate() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("tx-today", TODAY, 9, 0));
        transactions.add(transaction("tx-yesterday", TODAY.minusDays(1), 9, 0));
        transactions.add(transaction("tx-old", TODAY.minusDays(2), 9, 0));

        List<TransactionListItem> items = TransactionGrouper.group(transactions, TODAY);

        assertEquals(6, items.size());
        assertEquals("Hôm nay", titleAt(items, 0));
        assertEquals("tx-today", idAt(items, 1));
        assertEquals("Hôm qua", titleAt(items, 2));
        assertEquals("tx-yesterday", idAt(items, 3));
        assertEquals("27/10/2026", titleAt(items, 4));
        assertEquals("tx-old", idAt(items, 5));
    }

    @Test
    public void group_justBeforeAndJustAfterMidnight_areDifferentDays() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("tx-after-midnight", TODAY, 0, 1));
        transactions.add(transaction("tx-before-midnight", TODAY.minusDays(1), 23, 59));

        List<TransactionListItem> items = TransactionGrouper.group(transactions, TODAY);

        assertEquals(4, items.size());
        assertEquals("Hôm nay", titleAt(items, 0));
        assertEquals("Hôm qua", titleAt(items, 2));
    }

    @Test
    public void group_futureDay_usesDateTitle() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(transaction("tx-future", TODAY.plusDays(1), 9, 0));

        List<TransactionListItem> items = TransactionGrouper.group(transactions, TODAY);

        assertEquals("30/10/2026", titleAt(items, 0));
    }
}