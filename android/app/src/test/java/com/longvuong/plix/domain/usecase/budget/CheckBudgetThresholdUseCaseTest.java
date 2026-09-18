package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

public class CheckBudgetThresholdUseCaseTest {
    private static final String PERIOD = "2026-09";
    private static final long TS = CalculateBudgetProgressUseCase.periodStartAtMillis(PERIOD) + 1000;
    private FakeBudgetRepository fakeBudgetRepository;
    private FakeTransactionRepository fakeTransactionRepository;
    private FakeBudgetThresholdNotifier fakeNotifier;
    private CheckBudgetThresholdUseCase useCase;

    @Before
    public void setUp() {
        fakeBudgetRepository = new FakeBudgetRepository();
        fakeTransactionRepository = new FakeTransactionRepository();
        fakeNotifier = new FakeBudgetThresholdNotifier();
        useCase = new CheckBudgetThresholdUseCase(fakeBudgetRepository, fakeTransactionRepository, new CalculateBudgetProgressUseCase(), new BudgetThresholdChecker(), fakeNotifier);
    }

    private BudgetEntity budget(String id, String categoryId, long limitAmount) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.period = PERIOD;
        entity.categoryId = categoryId;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = 80;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private TransactionEntity transaction(String id, String categoryId, long amount, long occurredAt) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = "expense";
        entity.categoryId = categoryId;
        entity.occurredAt = occurredAt;
        entity.updatedAt = occurredAt;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void checkAfterAdd_pushesOverallBudgetOverThreshold_notifies() {
        fakeBudgetRepository.insert(budget("budget-1", null, 1_000_000), r -> {});
        fakeTransactionRepository.seed(transaction("tx-old", null, 700_000, TS));
        TransactionEntity newTx = transaction("tx-new", null, 150_000, TS);
        fakeTransactionRepository.seed(newTx); //Giao dịch đã insert thành công trước khi gọi check
        useCase.checkAfterAdd(newTx);
        assertEquals(1, fakeNotifier.notifiedBudgets.size());
        assertEquals("budget-1", fakeNotifier.notifiedBudgets.get(0).id);
        assertEquals(850_000L, (long) fakeNotifier.notifiedSpentAmounts.get(0));
    }

    @Test
    public void checkAfterAdd_stillBelowThreshold_doesNotNotify() {
        fakeBudgetRepository.insert(budget("budget-1", null, 1_000_000), r -> {});
        TransactionEntity newTx = transaction("tx-new", null, 500_000, TS);
        fakeTransactionRepository.seed(newTx);
        useCase.checkAfterAdd(newTx);
        assertEquals(0, fakeNotifier.notifiedBudgets.size());
    }

    @Test
    public void checkAfterAdd_categoryBudgetCrossed_notifiesOnlyThatBudget() {
        fakeBudgetRepository.insert(budget("budget-overall", null, 10_000_000), r -> {});
        fakeBudgetRepository.insert(budget("budget-food", "sys_an_uong", 500_000), r -> {});
        fakeTransactionRepository.seed(transaction("tx-old", "sys_an_uong", 350_000, TS));
        TransactionEntity newTx = transaction("tx-new", "sys_an_uong", 100_000, TS);
        fakeTransactionRepository.seed(newTx);
        useCase.checkAfterAdd(newTx);
        assertEquals(1, fakeNotifier.notifiedBudgets.size());
        assertEquals("budget-food", fakeNotifier.notifiedBudgets.get(0).id);
    }

    @Test
    public void checkAfterAdd_noBudgetForScope_doesNotNotifyOrCrash() {
        TransactionEntity newTx = transaction("tx-new", null, 5_000_000, TS);
        fakeTransactionRepository.seed(newTx);
        useCase.checkAfterAdd(newTx);
        assertEquals(0, fakeNotifier.notifiedBudgets.size());
    }

    @Test
    public void checkAfterUpdate_editAmountPushesOverThreshold_notifies() {
        fakeBudgetRepository.insert(budget("budget-1", null, 1_000_000), r -> {});
        TransactionEntity oldTx = transaction("tx-1", null, 100_000, TS);
        TransactionEntity newTx = transaction("tx-1", null, 850_000, TS); //sửa số tiền 100k thành 850k
        fakeTransactionRepository.seed(newTx); //Db hiện đang lưu bản đã sửa
        useCase.checkAfterUpdate(oldTx, newTx);
        assertEquals(1, fakeNotifier.notifiedBudgets.size());
        assertEquals(850_000L, (long) fakeNotifier.notifiedSpentAmounts.get(0));
    }

    @Test
    public void checkAfterUpdate_editStillBelowThreshold_doesNotNotify() {
        fakeBudgetRepository.insert(budget("budget-1", null, 1_000_000), r -> {});
        TransactionEntity oldTx = transaction("tx-1", null, 100_000, TS);
        TransactionEntity newTx = transaction("tx-1", null, 200_000, TS);
        fakeTransactionRepository.seed(newTx);
        useCase.checkAfterUpdate(oldTx, newTx);
        assertEquals(0, fakeNotifier.notifiedBudgets.size());
    }

    @Test
    public void checkAfterUpdate_categoryChangedPushesNewCategoryBudgetOverThreshold_notifies() {
        fakeBudgetRepository.insert(budget("budget-food", "sys_an_uong", 500_000), r -> {});
        fakeBudgetRepository.insert(budget("budget-transport", "sys_di_chuyen", 500_000), r -> {});
        fakeTransactionRepository.seed(transaction("tx-other", "sys_an_uong", 350_000, TS));
        TransactionEntity oldTx = transaction("tx-1", "sys_di_chuyen", 100_000, TS);
        TransactionEntity newTx = transaction("tx-1", "sys_an_uong", 100_000, TS); //đổi category
        fakeTransactionRepository.seed(newTx);
        useCase.checkAfterUpdate(oldTx, newTx);
        assertEquals(1, fakeNotifier.notifiedBudgets.size());
        assertEquals("budget-food", fakeNotifier.notifiedBudgets.get(0).id);
    }
}