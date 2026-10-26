package com.longvuong.plix.presentation.analytics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.analytics.AggregateAnalyticsUseCase;
import com.longvuong.plix.domain.usecase.analytics.AnalyticsSummary;
import com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.common.UiState;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AnalyticsViewModel extends ViewModel {
    public static final String RANGE_WEEK = "week";
    public static final String RANGE_MONTH = "month";
    public static final String RANGE_3_MONTHS = "3m";
    public static final String RANGE_6_MONTHS = "6m";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CategoryRepository categoryRepository;
    private final FilterTransactionsUseCase filterTransactionsUseCase;
    private final AggregateAnalyticsUseCase aggregateAnalyticsUseCase;

    private final MediatorLiveData<UiState<AnalyticsSummary>> analyticsState = new MediatorLiveData<>();
    //Chú thích khoảng ngày đang xem, vd "01/09/2026 – 30/09/2026 (Tháng 9)"
    private final MutableLiveData<String> rangeLabel = new MutableLiveData<>();
    private List<TransactionEntity> latestRawTransactions = new ArrayList<>();
    private String selectedRangePreset = RANGE_MONTH;

    @Inject
    public AnalyticsViewModel(TransactionRepository transactionRepository, CategoryRepository categoryRepository, FilterTransactionsUseCase filterTransactionsUseCase, AggregateAnalyticsUseCase aggregateAnalyticsUseCase) {
        this.categoryRepository = categoryRepository;
        this.filterTransactionsUseCase = filterTransactionsUseCase;
        this.aggregateAnalyticsUseCase = aggregateAnalyticsUseCase;

        analyticsState.setValue(new UiState.Loading<>());
        rangeLabel.setValue(currentRangeLabel(resolveRangeMillis(selectedRangePreset)));
        analyticsState.addSource(transactionRepository.getAll(), transactions -> {
            latestRawTransactions = transactions != null ? transactions : new ArrayList<>();
            recomputeAndPublish();
        });
    }

    public LiveData<UiState<AnalyticsSummary>> getAnalyticsState() {
        return analyticsState;
    }

    public LiveData<String> getRangeLabel() {
        return rangeLabel;
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categoryRepository.getActiveCategories();
    }

    public String getSelectedRangePreset() {
        return selectedRangePreset;
    }

    public void setRangePreset(String preset) {
        this.selectedRangePreset = preset;
        recomputeAndPublish();
    }

    private void recomputeAndPublish() {
        long[] range = resolveRangeMillis(selectedRangePreset);
        rangeLabel.setValue(currentRangeLabel(range));
        List<TransactionEntity> filtered = filterTransactionsUseCase.execute(
                latestRawTransactions, null, range[0], range[1]);

        if (filtered.isEmpty()) {
            analyticsState.setValue(new UiState.Empty<>());
            return;
        }

        AnalyticsSummary summary = aggregateAnalyticsUseCase.execute(filtered);
        analyticsState.setValue(new UiState.Success<>(summary));
    }

    private String currentRangeLabel(long[] range) {
        return formatRangeLabel(selectedRangePreset, toLocalDate(range[0]), toLocalDate(range[1]));
    }

    private static LocalDate toLocalDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static String formatRangeLabel(String preset, LocalDate start, LocalDate end) {
        String name = shortRangeName(preset, end);
        if (RANGE_3_MONTHS.equals(preset) || RANGE_6_MONTHS.equals(preset)) {
            name += " gần nhất";
        }
        return DATE_FORMATTER.format(start) + " – " + DATE_FORMATTER.format(end) + " (" + name + ")";
    }

    static String shortRangeName(String preset, LocalDate end) {
        switch (preset) {
            case RANGE_WEEK:
                return "Tuần này";
            case RANGE_3_MONTHS:
                return "3 tháng";
            case RANGE_6_MONTHS:
                return "6 tháng";
            default:
                return "Tháng " + end.getMonthValue();
        }
    }

    private long[] resolveRangeMillis(String preset) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        YearMonth currentMonth = YearMonth.from(today);

        switch (preset) {
            case RANGE_WEEK: {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                LocalDate sunday = monday.plusDays(6);
                long start = monday.atStartOfDay(zone).toInstant().toEpochMilli();
                long end = sunday.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli();
                return new long[]{start, end};
            }
            case RANGE_3_MONTHS: {
                YearMonth startMonth = currentMonth.minusMonths(2);
                return new long[]{
                        CalculateBudgetProgressUseCase.periodStartAtMillis(startMonth.toString()),
                        CalculateBudgetProgressUseCase.periodEndAtMillis(currentMonth.toString())};
            }
            case RANGE_6_MONTHS: {
                YearMonth startMonth = currentMonth.minusMonths(5);
                return new long[]{
                        CalculateBudgetProgressUseCase.periodStartAtMillis(startMonth.toString()),
                        CalculateBudgetProgressUseCase.periodEndAtMillis(currentMonth.toString())};
            }
            case RANGE_MONTH:
            default: {
                return new long[]{
                        CalculateBudgetProgressUseCase.periodStartAtMillis(currentMonth.toString()),
                        CalculateBudgetProgressUseCase.periodEndAtMillis(currentMonth.toString())};
            }
        }
    }
}