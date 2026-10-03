package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncTablesTest {
    private SyncTables syncTables;

    @Before
    public void setUp() {
        //Không gọi DAO/api nên truyền null
        syncTables = new SyncTables(
                new CategorySyncableEntity(null, null),
                new TransactionSyncableEntity(null, null),
                new BudgetSyncableEntity(null, null),
                new GoalSyncableEntity(null, null),
                new CorrectionSyncableEntity(null, null));
    }

    @Test
    public void inSyncOrder_isCategoriesTransactionsBudgetsGoalsCorrections() {
        List<String> names = new ArrayList<>();
        for (SyncableEntity<?, ?> table : syncTables.inSyncOrder()) {
            names.add(table.tableName());
        }
        //5 tên bảng khác nhau -> mỗi bảng có con trỏ riêng (SyncPreferences khoá theo user_id + tên bảng)
        assertEquals(Arrays.asList("categories", "transactions", "budgets", "goals", "corrections"), names);
    }
}