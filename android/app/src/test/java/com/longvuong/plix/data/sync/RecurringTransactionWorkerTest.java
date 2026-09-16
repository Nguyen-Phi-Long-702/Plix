package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
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
    public void generateMissingInstances_templateWithoutInstanceYet_insertsExactlyOneInstance() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, currentPeriodNow());
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(1, instances.size());
    }

    @Test
    public void generateMissingInstances_calledTwiceSamePeriod_doesNotCreateDuplicate() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        long now = currentPeriodNow();
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, now);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, now); //chạy lại lần 2, cùng kì
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(1, instances.size());
    }

    @Test
    public void generateMissingInstances_generatedInstance_isNotItselfRecurringAndLinksToTemplate() {
        fakeTransactionDao.insert(monthlyTemplate("template-1", 15));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, currentPeriodNow());
        TransactionEntity instance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals("template-1", instance.recurrenceParentId);
        assertFalse(instance.isRecurring);
    }

    @Test
    public void generateMissingInstances_invalidRecurrenceRule_doesNotInsertAnyInstance() {
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

    @Test
    public void generateMissingInstances_threeConsecutiveMonthsMissed_catchesUpExactlyThreeInstances() {
        fakeTransactionDao.insert(monthlyTemplateAt("template-1", 15, dateMillis(2026, 5, 1)));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 5, 20));
        assertEquals(1, fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").size());
        long reopenNow = dateMillis(2026, 8, 20);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, reopenNow);
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(4, instances.size());
        assertNotNull(findInstanceForPeriod(instances, 2026, 6));
        assertNotNull(findInstanceForPeriod(instances, 2026, 7));
        assertNotNull(findInstanceForPeriod(instances, 2026, 8));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, reopenNow);
        assertEquals(4, fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").size());
    }

    @Test
    public void generateMissingInstances_workerRunsEveryMonthInSequence_generatesOneInstancePerMonthNoDuplicates() {
        fakeTransactionDao.insert(monthlyTemplateAt("template-1", 5, dateMillis(2027, 1, 1)));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2027, 1, 20));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2027, 2, 20));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2027, 3, 20));
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(3, instances.size());
        assertNotNull(findInstanceForPeriod(instances, 2027, 1));
        assertNotNull(findInstanceForPeriod(instances, 2027, 2));
        assertNotNull(findInstanceForPeriod(instances, 2027, 3));
    }

    @Test
    public void generateMissingInstances_day31RuleInto30DayMonth_clampsToDay30() {
        fakeTransactionDao.insert(monthlyTemplateAt("template-1", 31, dateMillis(2026, 9, 1)));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 9, 20));
        TransactionEntity instance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals(30, dayOfMonthOf(instance.occurredAt));
    }

    @Test
    public void generateMissingInstances_day31RuleIntoNonLeapFebruary_clampsToDay28() {
        fakeTransactionDao.insert(monthlyTemplateAt("template-1", 31, dateMillis(2027, 2, 1)));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2027, 2, 20));
        TransactionEntity instance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals(28, dayOfMonthOf(instance.occurredAt));
    }

    @Test
    public void generateMissingInstances_day31RuleIntoLeapFebruary_clampsToDay29() {
        fakeTransactionDao.insert(monthlyTemplateAt("template-1", 31, dateMillis(2028, 2, 1)));
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2028, 2, 20));
        TransactionEntity instance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals(29, dayOfMonthOf(instance.occurredAt));
    }

    @Test
    public void editingRecurrenceRuleAfterInstanceGenerated_doesNotChangePastInstance() {
        TransactionEntity template = monthlyTemplateAt("template-1", 10, dateMillis(2026, 6, 1));
        fakeTransactionDao.insert(template);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 6, 20));
        TransactionEntity juneInstance = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").get(0);
        assertEquals(10, dayOfMonthOf(juneInstance.occurredAt));
        template.recurrenceRule = "MONTHLY:20";
        fakeTransactionDao.update(template);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 7, 15));
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(2, instances.size());
        assertEquals(10, dayOfMonthOf(findInstanceForPeriod(instances, 2026, 6).occurredAt));
        assertEquals(20, dayOfMonthOf(findInstanceForPeriod(instances, 2026, 7).occurredAt));
    }

    @Test
    public void softDeletingTemplate_stopsGeneratingNewInstances_butKeepsAlreadyGeneratedOnes() {
        TransactionEntity template = monthlyTemplateAt("template-1", 15, dateMillis(2026, 6, 1));
        fakeTransactionDao.insert(template);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 6, 20));
        assertEquals(1, fakeTransactionDao.getInstancesByRecurrenceParentId("template-1").size());
        template.isDeleted = true;
        fakeTransactionDao.update(template);
        RecurringTransactionWorker.generateMissingInstances(fakeTransactionDao, dateMillis(2026, 7, 20));
        List<TransactionEntity> instances = fakeTransactionDao.getInstancesByRecurrenceParentId("template-1");
        assertEquals(1, instances.size());
    }

    private static TransactionEntity monthlyTemplateAt(String id, int dayOfMonth, long occurredAt) {
        TransactionEntity template = new TransactionEntity();
        template.id = id;
        template.userId = "user-1";
        template.amount = 2000000;
        template.type = "expense";
        template.categoryId = "sys_hoa_don";
        template.note = "Tiền nhà hàng tháng";
        template.occurredAt = occurredAt;
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:" + dayOfMonth;
        template.recurrenceParentId = null;
        template.updatedAt = occurredAt;
        template.syncStatus = "synced";
        template.isDeleted = false;
        return template;
    }

    private static long dateMillis(int year, int monthOneBased, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, monthOneBased - 1, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    private static int dayOfMonthOf(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static TransactionEntity findInstanceForPeriod(List<TransactionEntity> instances, int year, int monthOneBased) {
        Calendar calendar = Calendar.getInstance();
        for (TransactionEntity instance : instances) {
            calendar.setTimeInMillis(instance.occurredAt);
            if (calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == monthOneBased - 1) {
                return instance;
            }
        }
        return null;
    }

    private static void setFakeAccessToken(AuthManager manager, String token) throws Exception {
        Field field = AuthManager.class.getDeclaredField("accessToken");
        field.setAccessible(true);
        field.set(manager, token);
    }

    @Test
    public void isEligibleToRun_noSession_returnsFalse() {
        AuthManager loggedOutAuthManager = new AuthManager(null);
        assertFalse(RecurringTransactionWorker.isEligibleToRun(loggedOutAuthManager));
    }

    @Test
    public void isEligibleToRun_hasAccessTokenInMemory_returnsTrue() throws Exception {
        AuthManager loggedInAuthManager = new AuthManager(null);
        setFakeAccessToken(loggedInAuthManager, "fake-jwt-token");
        assertTrue(RecurringTransactionWorker.isEligibleToRun(loggedInAuthManager));
    }
}