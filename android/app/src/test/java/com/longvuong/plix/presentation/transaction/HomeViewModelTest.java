package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.analytics.AggregateAnalyticsUseCase;
import com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private FakeTransactionRepository fakeTransactionRepository;
    private FakeBudgetRepository fakeBudgetRepository;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository = new FakeTransactionRepository();
        fakeBudgetRepository = new FakeBudgetRepository();
        FakeCategoryRepository fakeCategoryRepository = new FakeCategoryRepository(Arrays.asList(
                category("sys_an_uong", "Ăn uống", "expense"),
                category("sys_di_chuyen", "Di chuyển", "expense")));
        viewModel = new HomeViewModel(fakeAiRepository, fakeTransactionRepository, fakeBudgetRepository,
                fakeCategoryRepository, new FilterTransactionsUseCase(), new AggregateAnalyticsUseCase(),
                new CalculateBudgetProgressUseCase());
        viewModel.getQuickAddVisible().observeForever(visible -> { }); //MediatorLiveData chỉ cập nhật khi có người quan sát
        viewModel.getHomeState().observeForever(state -> { });
    }

    @Test
    public void quickAddVisible_whenEnabledAndOnline_isTrue() {
        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenDisabledInSettings_isFalse() {
        fakeAiRepository.setQuickAddEnabled(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenOffline_isFalse() {
        fakeAiRepository.setConnected(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_returnsWhenBackOnline() {
        fakeAiRepository.setConnected(false);
        fakeAiRepository.setConnected(true);

        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_staysFalseWhenBackOnlineButDisabled() {
        fakeAiRepository.setQuickAddEnabled(false);
        fakeAiRepository.setConnected(false);
        fakeAiRepository.setConnected(true);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void homeState_beforeTransactionsLoaded_isLoading() {
        assertTrue(viewModel.getHomeState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void homeState_noTransactions_isEmpty() {
        fakeTransactionRepository.allTransactions.setValue(new ArrayList<>());

        assertTrue(viewModel.getHomeState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void homeState_summarizesOnlyCurrentMonth() {
        fakeTransactionRepository.allTransactions.setValue(Arrays.asList(
                transaction("t1", "income", 10_000_000L, "sys_an_uong", thisMonth()),
                transaction("t2", "expense", 3_000_000L, "sys_an_uong", thisMonth()),
                transaction("t3", "expense", 500_000L, "sys_an_uong", lastMonth())));

        HomeUiModel model = successModel();

        assertEquals(currentMonthLabel(), model.monthLabel);
        assertEquals(10_000_000L, model.totalIncome);
        assertEquals(3_000_000L, model.totalExpense);
        assertEquals(7_000_000L, model.netBalance);
    }

    @Test
    public void homeState_expenseHigherThanIncome_netBalanceIsNegative() {
        fakeTransactionRepository.allTransactions.setValue(Arrays.asList(
                transaction("t1", "income", 1_000_000L, "sys_an_uong", thisMonth()),
                transaction("t2", "expense", 1_500_000L, "sys_an_uong", thisMonth())));

        assertEquals(-500_000L, successModel().netBalance);
    }

    @Test
    public void homeState_recentTransactions_keepsFirstFourInGivenOrder() {
        List<TransactionEntity> transactions = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            transactions.add(transaction("t" + i, "expense", 1_000L, "sys_an_uong", thisMonth()));
        }
        fakeTransactionRepository.allTransactions.setValue(transactions);

        HomeUiModel model = successModel();

        assertEquals(4, model.recentTransactions.size());
        assertEquals("t1", model.recentTransactions.get(0).id);
        assertEquals("t4", model.recentTransactions.get(3).id);
    }

    @Test
    public void homeState_includesCategoryNames() {
        fakeTransactionRepository.allTransactions.setValue(Collections.singletonList(
                transaction("t1", "expense", 1_000L, "sys_an_uong", thisMonth())));

        assertEquals("Ăn uống", successModel().categoryNamesById.get("sys_an_uong"));
    }

    @Test
    public void homeState_noBudgetThisMonth_budgetIsNull() {
        fakeTransactionRepository.allTransactions.setValue(Collections.singletonList(
                transaction("t1", "expense", 1_000L, "sys_an_uong", thisMonth())));

        assertNull(successModel().budget);
    }

    @Test
    public void homeState_picksBudgetWithHighestPercent() {
        fakeBudgetRepository.activeBudgets.setValue(Arrays.asList(
                budget("b1", null, 5_000_000L, 80),
                budget("b2", "sys_an_uong", 1_000_000L, 80)));
        fakeTransactionRepository.allTransactions.setValue(Arrays.asList(
                transaction("t1", "expense", 800_000L, "sys_an_uong", thisMonth()),
                transaction("t2", "expense", 200_000L, "sys_di_chuyen", thisMonth())));

        HomeUiModel.BudgetSummary budget = successModel().budget;

        assertNotNull(budget);
        assertEquals("Ăn uống", budget.name);
        assertEquals(80, budget.percent);
        assertEquals(800_000L, budget.spentAmount);
        assertEquals(1_000_000L, budget.limitAmount);
        assertEquals(HomeUiModel.BudgetLevel.WARNING, budget.level);
    }

    @Test
    public void homeState_budgetBelowThreshold_levelIsNormal() {
        fakeBudgetRepository.activeBudgets.setValue(Collections.singletonList(budget("b1", null, 1_000_000L, 80)));
        fakeTransactionRepository.allTransactions.setValue(Collections.singletonList(
                transaction("t1", "expense", 100_000L, "sys_an_uong", thisMonth())));

        HomeUiModel.BudgetSummary budget = successModel().budget;

        assertEquals("Ngân sách tổng", budget.name);
        assertEquals(HomeUiModel.BudgetLevel.NORMAL, budget.level);
    }

    @Test
    public void homeState_budgetSpentOverLimit_levelIsDanger() {
        fakeBudgetRepository.activeBudgets.setValue(Collections.singletonList(budget("b1", null, 500_000L, 80)));
        fakeTransactionRepository.allTransactions.setValue(Collections.singletonList(
                transaction("t1", "expense", 600_000L, "sys_an_uong", thisMonth())));

        HomeUiModel.BudgetSummary budget = successModel().budget;

        assertEquals(120, budget.percent);
        assertEquals(HomeUiModel.BudgetLevel.DANGER, budget.level);
    }

    private HomeUiModel successModel() {
        UiState<HomeUiModel> state = viewModel.getHomeState().getValue();
        assertTrue(state instanceof UiState.Success);
        return ((UiState.Success<HomeUiModel>) state).data;
    }

    private static long thisMonth() {
        return YearMonth.now().atDay(1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static long lastMonth() {
        return YearMonth.now().minusMonths(1).atDay(15).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static TransactionEntity transaction(String id, String type, long amount, String categoryId, long occurredAt) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.type = type;
        entity.amount = amount;
        entity.categoryId = categoryId;
        entity.occurredAt = occurredAt;
        entity.updatedAt = occurredAt;
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    private static CategoryEntity category(String id, String name, String type) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.name = name;
        entity.type = type;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private static BudgetEntity budget(String id, String categoryId, long limitAmount, int thresholdPercent) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.period = YearMonth.now().toString();
        entity.categoryId = categoryId;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = thresholdPercent;
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    private static String currentMonthLabel() {
        YearMonth month = YearMonth.now();
        return "Tháng " + month.getMonthValue() + "/" + month.getYear();
    }
}