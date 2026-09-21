package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RecurringTransactionWorkerRoomTest {
    private AppDatabase database;
    private TransactionDao transactionDao;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).addCallback(AppDatabase.SEED_CATEGORIES_CALLBACK).allowMainThreadQueries().build();
        transactionDao = database.transactionDao();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    private TransactionEntity monthlyTemplate(String id, int dayOfMonth, long occurredAt) {
        TransactionEntity template = new TransactionEntity();
        template.id = id;
        template.userId = "user-1";
        template.amount = 2000000;
        template.type = "expense";
        template.categoryId = "sys_hoa_don";
        template.occurredAt = occurredAt;
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:" + dayOfMonth;
        template.recurrenceParentId = null;
        template.updatedAt = occurredAt;
        template.syncStatus = "pending";
        template.isDeleted = false;
        return template;
    }

    @Test
    public void generateMissingInstances_withRealRoomDatabase_insertsExactlyOneInstanceForCurrentPeriod() {
        long templateCreatedAt = 1725379200000L; 

        transactionDao.insert(monthlyTemplate("tpl-1", 15, templateCreatedAt));

        RecurringTransactionWorker.generateMissingInstances(transactionDao, templateCreatedAt);

        List<TransactionEntity> instances = transactionDao.getInstancesByRecurrenceParentId("tpl-1");
        assertEquals(1, instances.size());
        assertEquals("tpl-1", instances.get(0).recurrenceParentId);
        assertFalse(instances.get(0).isDeleted);
    }

    @Test
    public void generateMissingInstances_calledTwiceSameKyWithRealRoomDatabase_doesNotDuplicate() {
        long now = 1725379200000L;
        transactionDao.insert(monthlyTemplate("tpl-2", 15, now));

        RecurringTransactionWorker.generateMissingInstances(transactionDao, now);
        RecurringTransactionWorker.generateMissingInstances(transactionDao, now);

        List<TransactionEntity> instances = transactionDao.getInstancesByRecurrenceParentId("tpl-2");
        assertEquals(1, instances.size());
    }

    @Test
    public void generateMissingInstances_softDeletedTemplateWithRealRoomDatabase_isExcludedByActiveRecurringTemplatesQuery() {
        long now = 1725379200000L;
        TransactionEntity template = monthlyTemplate("tpl-3", 15, now);
        template.isDeleted = true;
        transactionDao.insert(template);

        RecurringTransactionWorker.generateMissingInstances(transactionDao, now);

        List<TransactionEntity> instances = transactionDao.getInstancesByRecurrenceParentId("tpl-3");
        assertEquals(0, instances.size());
    }
}