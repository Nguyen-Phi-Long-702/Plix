package com.longvuong.plix.presentation.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.domain.usecase.analytics.AggregateAnalyticsUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

public class AnalyticsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AnalyticsViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new AnalyticsViewModel(new FakeTransactionRepository(), new FakeCategoryRepository(),
                new FilterTransactionsUseCase(), new AggregateAnalyticsUseCase());
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
}