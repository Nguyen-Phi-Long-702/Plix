package com.longvuong.plix.presentation.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.analytics.AggregateAnalyticsUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AnalyticsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeTransactionRepository fakeTransactionRepository;
    private FakeCategoryRepository fakeCategoryRepository;
    private AnalyticsViewModel viewModel;

    @Before
    public void setUp() {
        fakeTransactionRepository = new FakeTransactionRepository();
        fakeCategoryRepository = new FakeCategoryRepository();
        viewModel = new AnalyticsViewModel(fakeTransactionRepository, fakeCategoryRepository,
                new FilterTransactionsUseCase(), new AggregateAnalyticsUseCase());
        viewModel.getAnalyticsState().observeForever(state -> { }); //MediatorLiveData chỉ cập nhật khi có người quan sát
    }

    @Test
    public void formatRangeLabel_month() {
        String label = AnalyticsViewModel.formatRangeLabel(AnalyticsViewModel.RANGE_MONTH,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals("01/09/2026 – 30/09/2026 (Tháng 9)", label);
    }

    @Test
    public void formatRangeLabel_week() {
        String label = AnalyticsViewModel.formatRangeLabel(AnalyticsViewModel.RANGE_WEEK,
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 6));

        assertEquals("31/08/2026 – 06/09/2026 (Tuần này)", label);
    }

    @Test
    public void formatRangeLabel_threeMonths() {
        String label = AnalyticsViewModel.formatRangeLabel(AnalyticsViewModel.RANGE_3_MONTHS,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

        assertEquals("01/07/2026 – 30/09/2026 (3 tháng gần nhất)", label);
    }

    @Test
    public void formatRangeLabel_sixMonths() {
        String label = AnalyticsViewModel.formatRangeLabel(AnalyticsViewModel.RANGE_6_MONTHS,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));

        assertEquals("01/04/2026 – 30/09/2026 (6 tháng gần nhất)", label);
    }

    @Test
    public void rangeLabel_initially_isCurrentMonth() {
        YearMonth month = YearMonth.now();
        String expected = String.format(Locale.ROOT, "01/%02d/%d – %02d/%02d/%d (Tháng %d)",
                month.getMonthValue(), month.getYear(), month.lengthOfMonth(), month.getMonthValue(),
                month.getYear(), month.getMonthValue());

        assertEquals(AnalyticsViewModel.RANGE_MONTH, viewModel.getSelectedRangePreset());
        assertEquals(expected, viewModel.getRangeLabel().getValue());
    }

    @Test
    public void setRangePreset_updatesSelectedPresetAndLabel() {
        viewModel.setRangePreset(AnalyticsViewModel.RANGE_3_MONTHS);

        assertEquals(AnalyticsViewModel.RANGE_3_MONTHS, viewModel.getSelectedRangePreset());
        assertTrue(viewModel.getRangeLabel().getValue().endsWith("(3 tháng gần nhất)"));
    }

    @Test
    public void state_beforeAnySourceLoaded_isLoading() {
        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void state_transactionsLoadedButCategoriesNot_staysLoading() {
        fakeTransactionRepository.allTransactions.setValue(new ArrayList<>());

        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void state_setRangePresetBeforeDataLoaded_staysLoading() {
        viewModel.setRangePreset(AnalyticsViewModel.RANGE_WEEK);

        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void state_noTransactions_isEmpty() {
        loadData(Collections.emptyList());

        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void state_onlyTransactionsOutsideRange_isEmptyUntilRangeWidened() {
        loadData(Collections.singletonList(transaction("t1", "expense", 100_000L, "sys_an_uong", lastMonth())));

        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Empty);

        viewModel.setRangePreset(AnalyticsViewModel.RANGE_3_MONTHS);

        assertTrue(viewModel.getAnalyticsState().getValue() instanceof UiState.Success);
    }

    @Test
    public void success_categorySlices_useNamesPercentAndColorOrder() {
        loadData(Arrays.asList(
                transaction("t1", "income", 10_000_000L, "sys_luong", thisMonth()),
                transaction("t2", "expense", 600_000L, "sys_an_uong", thisMonth()),
                transaction("t3", "expense", 300_000L, "sys_di_chuyen", thisMonth()),
                transaction("t4", "expense", 100_000L, "sys_an_uong", thisMonth())));

        AnalyticsUiModel model = successModel();

        assertEquals(2, model.categorySlices.size());
        assertSlice(model.categorySlices.get(0), "Ăn uống", 700_000L, 70, 0);
        assertSlice(model.categorySlices.get(1), "Di chuyển", 300_000L, 30, 1);
    }

    @Test
    public void success_unknownOrMissingCategory_mergedIntoUncategorized() {
        loadData(Arrays.asList(
                transaction("t1", "expense", 200_000L, null, thisMonth()),
                transaction("t2", "expense", 300_000L, "da_xoa", thisMonth()),
                transaction("t3", "expense", 100_000L, "sys_an_uong", thisMonth())));

        AnalyticsUiModel model = successModel();

        assertEquals(2, model.categorySlices.size());
        assertSlice(model.categorySlices.get(0), AnalyticsViewModel.UNCATEGORIZED_LABEL, 500_000L, 83, 0);
        assertSlice(model.categorySlices.get(1), "Ăn uống", 100_000L, 17, 1);
    }

    @Test
    public void success_exactlyFiveCategories_allShownWithoutGrouping() {
        List<TransactionEntity> transactions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            transactions.add(transaction("t" + i, "expense", i * 100_000L, "c" + i, thisMonth()));
        }
        loadData(transactions);

        AnalyticsUiModel model = successModel();

        assertEquals(5, model.categorySlices.size());
        assertSlice(model.categorySlices.get(0), "Danh mục 5", 500_000L, 33, 0);
        assertSlice(model.categorySlices.get(4), "Danh mục 1", 100_000L, 7, 4);
    }

    @Test
    public void success_moreThanFiveCategories_groupsTheRestIntoOneSlice() {
        List<TransactionEntity> transactions = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            transactions.add(transaction("t" + i, "expense", i * 100_000L, "c" + i, thisMonth()));
        }
        loadData(transactions);

        AnalyticsUiModel model = successModel();

        assertEquals(5, model.categorySlices.size());
        assertSlice(model.categorySlices.get(0), "Danh mục 7", 700_000L, 25, 0);
        assertSlice(model.categorySlices.get(3), "Danh mục 4", 400_000L, 14, 3);
        assertSlice(model.categorySlices.get(4), AnalyticsViewModel.OTHER_CATEGORIES_LABEL, 600_000L, 21, 4);
    }

    @Test
    public void success_onlyIncome_hasNoCategorySlices() {
        loadData(Collections.singletonList(transaction("t1", "income", 5_000_000L, "sys_luong", thisMonth())));

        AnalyticsUiModel model = successModel();

        assertTrue(model.categorySlices.isEmpty());
        assertEquals(1, model.monthBars.size());
    }

    @Test
    public void success_monthBar_splitsIncomeIntoExpenseAndRemaining() {
        loadData(Arrays.asList(
                transaction("t1", "income", 10_000_000L, "sys_luong", thisMonth()),
                transaction("t2", "expense", 1_000_000L, "sys_an_uong", thisMonth())));

        AnalyticsUiModel.MonthBar bar = successModel().monthBars.get(0);

        assertEquals("T" + YearMonth.now().getMonthValue(), bar.label);
        assertEquals(1_000_000L, bar.expenseAmount);
        assertEquals(9_000_000L, bar.remainingAmount);
    }

    @Test
    public void success_monthBar_expenseOverIncome_remainingIsZero() {
        loadData(Arrays.asList(
                transaction("t1", "income", 1_000_000L, "sys_luong", thisMonth()),
                transaction("t2", "expense", 1_500_000L, "sys_an_uong", thisMonth())));

        AnalyticsUiModel.MonthBar bar = successModel().monthBars.get(0);

        assertEquals(1_500_000L, bar.expenseAmount);
        assertEquals(0L, bar.remainingAmount);
    }

    @Test
    public void success_threeMonths_monthBarsAreSortedByMonth() {
        loadData(Arrays.asList(
                transaction("t1", "expense", 100_000L, "sys_an_uong", thisMonth()),
                transaction("t2", "expense", 200_000L, "sys_an_uong", lastMonth())));
        viewModel.setRangePreset(AnalyticsViewModel.RANGE_3_MONTHS);

        List<AnalyticsUiModel.MonthBar> bars = successModel().monthBars;

        assertEquals(2, bars.size());
        assertEquals(200_000L, bars.get(0).expenseAmount);
        assertEquals(100_000L, bars.get(1).expenseAmount);
    }

    @Test
    public void success_totalsSavingsRateAndCenterLabel() {
        loadData(Arrays.asList(
                transaction("t1", "income", 10_000_000L, "sys_luong", thisMonth()),
                transaction("t2", "expense", 1_000_000L, "sys_an_uong", thisMonth())));

        AnalyticsUiModel model = successModel();

        assertEquals(10_000_000L, model.totalIncome);
        assertEquals(1_000_000L, model.totalExpense);
        assertEquals(90, model.savingsRatePercent);
        assertEquals("Tháng " + YearMonth.now().getMonthValue(), model.centerLabel);
    }

    @Test
    public void success_centerLabel_followsPreset() {
        loadData(Collections.singletonList(transaction("t1", "expense", 100_000L, "sys_an_uong", thisMonth())));

        viewModel.setRangePreset(AnalyticsViewModel.RANGE_6_MONTHS);

        assertEquals("6 tháng", successModel().centerLabel);
    }

    private void loadData(List<TransactionEntity> transactions) {
        fakeCategoryRepository.categories.setValue(Arrays.asList(
                category("sys_an_uong", "Ăn uống", "expense"),
                category("sys_di_chuyen", "Di chuyển", "expense"),
                category("sys_luong", "Lương", "income"),
                category("c1", "Danh mục 1", "expense"),
                category("c2", "Danh mục 2", "expense"),
                category("c3", "Danh mục 3", "expense"),
                category("c4", "Danh mục 4", "expense"),
                category("c5", "Danh mục 5", "expense"),
                category("c6", "Danh mục 6", "expense"),
                category("c7", "Danh mục 7", "expense")));
        fakeTransactionRepository.allTransactions.setValue(transactions);
    }

    private AnalyticsUiModel successModel() {
        UiState<AnalyticsUiModel> state = viewModel.getAnalyticsState().getValue();
        assertTrue(state instanceof UiState.Success);
        return ((UiState.Success<AnalyticsUiModel>) state).data;
    }

    private static void assertSlice(AnalyticsUiModel.CategorySlice slice, String label, long amount, int percent, int colorIndex) {
        assertEquals(label, slice.label);
        assertEquals(amount, slice.amount);
        assertEquals(percent, slice.percent);
        assertEquals(colorIndex, slice.colorIndex);
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
}