package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class RecurringTransactionWorkerTest {
    private FakeTransactionDao fakeTransactionDao;
    @Before
    public void setUp() {
        fakeTransactionDao = new FakeTransactionDao();
    }
    private TransactionEntity monthlyTemplate(String id, int dayOfMonth) {
        TransactionEntity template = new TransactionEntity();
        template.id = id;
        template.userId = "user-1";
        template.amount = 2000000;
        template.type = "expense";
        template.categoryId = "sys_hoa_don";
        template.note = "Tiền nhà hàng tháng";
        template.occurredAt = System.currentTimeMillis();
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:" + dayOfMonth;
        template.recurrenceParentId = null;
        template.updatedAt = System.currentTimeMillis();
        template.syncStatus = "synced";
        template.isDeleted = false;
        return template;
    }
    private long currentPeriodNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 15);
        return calendar.getTimeInMillis();
    }
    @Test
    public void generateForCurrentPeriod_templateWithoutInstanceYet_insertsExactlyOneInstance() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, currentPeriodNow());
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(1, instances.size());
    }
    @Test
    public void generateForCurrentPeriod_calledTwiceSamePeriod_doesNotCreateDuplicate() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        long now = currentPeriodNow();
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, now);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, now); //chạy lại lần 2, cùng kì
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(1, instances.size());
    }
    @Test
    public void generateForCurrentPeriod_generatedInstance_isNotItselfRecurringAndLinksToTemplate() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, currentPeriodNow());
        TransactionEntity instance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals("template-1", instance.recurrenceParentId);
        assertFalse(instance.isRecurring);
    }
    @Test
    public void generateForCurrentPeriod_invalidRecurrenceRule_doesNotInsertAnyInstance() {
        TransactionEntity invalidTemplate = monthlyTemplate("template-2", 15);
        invalidTemplate.recurrenceRule = "WEEKLY:2";
        fakeTransactionDao.insert(invalidTemplate);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, currentPeriodNow());
        assertEquals(0, fakeTransactionDao.getInstancesByRecurrenceParentId("template-2").size());
    }
    @Test
    public void parseDayOfMonth_validRule_returnsDay() {
        assertEquals(15, RecurringTransactionWorker.parseDayOfMonth("MONTHLY:15"));
    }
    @Test
    public void parseDayOfMonth_nullOrWrongPrefix_returnsInvalid() {
        assertEquals(-1, RecurringTransactionWorker.parseDayOfMonth(null));
        assertEquals(-1, RecurringTransactionWorker.parseDayOfMonth("WEEKLY:2"));
    }
}