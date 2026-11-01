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
import com.longvuong.plix.domain.usecase.analytics.CategoryExpense;
import com.longvuong.plix.domain.usecase.analytics.MonthlyTrend;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AnalyticsViewModel extends ViewModel {
    public static final String RANGE_WEEK = "week";
    public static final String RANGE_MONTH = "month";
    public static final String RANGE_3_MONTHS = "3m";
    public static final String RANGE_6_MONTHS = "6m";

    static final String UNCATEGORIZED_LABEL = "Chưa phân loại";
    static final String OTHER_CATEGORIES_LABEL = "Còn lại";
    private static final int MAX_CATEGORY_SLICES = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FilterTransactionsUseCase filterTransactionsUseCase;
    private final AggregateAnalyticsUseCase aggregateAnalyticsUseCase;

    private final MediatorLiveData<UiState<AnalyticsUiModel>> analyticsState = new MediatorLiveData<>();
    //Chú thích khoảng ngày đang xem, vd "01/09/2026 – 30/09/2026 (Tháng 9)"
    private final MutableLiveData<String> rangeLabel = new MutableLiveData<>();

    //null = nguồn dữ liệu đó chưa trả kết quả lần đầu
    private List<TransactionEntity> latestRawTransactions;
    private Map<String, String> categoryNamesById;
    private String selectedRangePreset = RANGE_MONTH;

    @Inject
    public AnalyticsViewModel(TransactionRepository transactionRepository, CategoryRepository categoryRepository, FilterTransactionsUseCase filterTransactionsUseCase, AggregateAnalyticsUseCase aggregateAnalyticsUseCase) {
        this.filterTransactionsUseCase = filterTransactionsUseCase;
        this.aggregateAnalyticsUseCase = aggregateAnalyticsUseCase;

        rangeLabel.setValue(currentRangeLabel(resolveRangeMillis(selectedRangePreset)));
        analyticsState.setValue(new UiState.Loading<>());
        analyticsState.addSource(transactionRepository.getAll(), transactions -> {
            latestRawTransactions = transactions != null ? transactions : new ArrayList<>();
            recomputeAndPublish();
        });
        analyticsState.addSource(categoryRepository.getActiveCategories(), categories -> {
            Map<String, String> names = new HashMap<>();
            if (categories != null) {
                for (CategoryEntity category : categories) {
                    names.put(category.id, category.name);
                }
            }
            categoryNamesById = names;
            recomputeAndPublish();
        });
    }

    public LiveData<UiState<AnalyticsUiModel>> getAnalyticsState() {
        return analyticsState;
    }

    public LiveData<String> getRangeLabel() {
        return rangeLabel;
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
        if (latestRawTransactions == null || categoryNamesById == null) {
            return; //chờ đủ cả giao dịch và danh mục để biểu đồ không nháy tên danh mục thiếu
        }

        List<TransactionEntity> filtered = filterTransactionsUseCase.execute(
                latestRawTransactions, null, range[0], range[1]);

        if (filtered.isEmpty()) {
            analyticsState.setValue(new UiState.Empty<>());
            return;
        }

        AnalyticsSummary summary = aggregateAnalyticsUseCase.execute(filtered);
        String centerLabel = shortRangeName(selectedRangePreset, toLocalDate(range[1]));
        analyticsState.setValue(new UiState.Success<>(new AnalyticsUiModel(
                buildCategorySlices(summary),
                buildMonthBars(summary.monthlyTrend),
                summary.totalIncome,
                summary.totalExpense,
                summary.savingsRatePercent,
                centerLabel)));
    }

    //Gộp theo tên (danh mục không tìm thấy gộp vào "Chưa phân loại"), sắp giảm dần; quá 5 danh mục thì 4 danh mục lớn nhất + 1 lát "Còn lại"
    private List<AnalyticsUiModel.CategorySlice> buildCategorySlices(AnalyticsSummary summary) {
        Map<String, Long> totalByLabel = new LinkedHashMap<>();
        for (CategoryExpense item : summary.categoryExpenses) {
            String name = item.categoryId != null ? categoryNamesById.get(item.categoryId) : null;
            totalByLabel.merge(name != null ? name : UNCATEGORIZED_LABEL, item.totalAmount, Long::sum);
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(totalByLabel.entrySet());
        Collections.sort(sorted, (a, b) -> Long.compare(b.getValue(), a.getValue()));

        boolean groupRest = sorted.size() > MAX_CATEGORY_SLICES;
        int namedCount = groupRest ? MAX_CATEGORY_SLICES - 1 : sorted.size();

        List<AnalyticsUiModel.CategorySlice> slices = new ArrayList<>();
        for (int i = 0; i < namedCount; i++) {
            slices.add(newSlice(sorted.get(i).getKey(), sorted.get(i).getValue(), summary.totalExpense, i));
        }
        if (groupRest) {
            long restAmount = 0L;
            for (int i = namedCount; i < sorted.size(); i++) {
                restAmount += sorted.get(i).getValue();
            }
            slices.add(newSlice(OTHER_CATEGORIES_LABEL, restAmount, summary.totalExpense, namedCount));
        }
        return slices;
    }

    private AnalyticsUiModel.CategorySlice newSlice(String label, long amount, long totalExpense, int colorIndex) {
        int percent = totalExpense > 0 ? (int) Math.round(amount * 100.0 / totalExpense) : 0;
        return new AnalyticsUiModel.CategorySlice(label, amount, percent, colorIndex);
    }

    //Mỗi cột cao bằng max(thu, chi): phần dưới là chi, phần trên là thu còn lại (0 nếu chi vượt thu)
    private List<AnalyticsUiModel.MonthBar> buildMonthBars(List<MonthlyTrend> monthlyTrend) {
        List<AnalyticsUiModel.MonthBar> bars = new ArrayList<>();
        for (MonthlyTrend item : monthlyTrend) {
            bars.add(new AnalyticsUiModel.MonthBar(
                    "T" + YearMonth.parse(item.yearMonth).getMonthValue(),
                    item.expenseAmount,
                    Math.max(item.incomeAmount - item.expenseAmount, 0L)));
        }
        return bars;
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