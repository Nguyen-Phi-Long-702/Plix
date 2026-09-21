package com.longvuong.plix.presentation.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.ChipGroup;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.domain.usecase.analytics.AnalyticsSummary;
import com.longvuong.plix.domain.usecase.analytics.CategoryExpense;
import com.longvuong.plix.domain.usecase.analytics.MonthlyTrend;
import com.longvuong.plix.presentation.common.UiState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AnalyticsFragment extends Fragment {
    private AnalyticsViewModel viewModel;
    private ChipGroup chipGroupRange;
    private ProgressBar progressLoading;
    private TextView textEmptyState;
    private View contentContainer;
    private BarChart barChartCategory;
    private LineChart lineChartTrend;
    private TextView textTotalIncome;
    private TextView textTotalExpense;
    private TextView textSavingsRate;

    private Map<String, String> categoryNamesById = new HashMap<>();
    private AnalyticsSummary latestSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        bindViews(view);
        setupCharts();
        setupRangeChips();

        viewModel.getAnalyticsState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(), this::onCategoriesChanged);
    }

    private void bindViews(View view) {
        chipGroupRange = view.findViewById(R.id.chipGroupRange);
        progressLoading = view.findViewById(R.id.progressLoading);
        textEmptyState = view.findViewById(R.id.textEmptyState);
        contentContainer = view.findViewById(R.id.contentContainer);
        barChartCategory = view.findViewById(R.id.barChartCategory);
        lineChartTrend = view.findViewById(R.id.lineChartTrend);
        textTotalIncome = view.findViewById(R.id.textTotalIncome);
        textTotalExpense = view.findViewById(R.id.textTotalExpense);
        textSavingsRate = view.findViewById(R.id.textSavingsRate);
    }

    private void setupRangeChips() {
        chipGroupRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            viewModel.setRangePreset(presetForChipId(checkedIds.get(0)));
        });
    }

    private String presetForChipId(int chipId) {
        if (chipId == R.id.chipRangeWeek) {
            return AnalyticsViewModel.RANGE_WEEK;
        } else if (chipId == R.id.chipRange3Months) {
            return AnalyticsViewModel.RANGE_3_MONTHS;
        } else if (chipId == R.id.chipRange6Months) {
            return AnalyticsViewModel.RANGE_6_MONTHS;
        }
        return AnalyticsViewModel.RANGE_MONTH;
    }

    private void setupCharts() {
        barChartCategory.getDescription().setEnabled(false);
        barChartCategory.getAxisRight().setEnabled(false);
        barChartCategory.getLegend().setEnabled(false);
        barChartCategory.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartCategory.getXAxis().setGranularity(1f);
        barChartCategory.setFitBars(true);

        lineChartTrend.getDescription().setEnabled(false);
        lineChartTrend.getAxisRight().setEnabled(false);
        lineChartTrend.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartTrend.getXAxis().setGranularity(1f);
    }

    private void onCategoriesChanged(List<CategoryEntity> categories) {
        Map<String, String> map = new HashMap<>();
        if (categories != null) {
            for (CategoryEntity category : categories) {
                map.put(category.id, category.name);
            }
        }
        categoryNamesById = map;
        if (latestSummary != null) {
            renderCharts(latestSummary);
        }
    }

    private void renderState(UiState<AnalyticsSummary> state) {
        progressLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textEmptyState.setVisibility((state instanceof UiState.Empty || state instanceof UiState.Error) ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            latestSummary = ((UiState.Success<AnalyticsSummary>) state).data;
            renderCharts(latestSummary);
        } else if (state instanceof UiState.Error) {
            textEmptyState.setText(((UiState.Error<AnalyticsSummary>) state).message);
        } else if (state instanceof UiState.Empty) {
            textEmptyState.setText("Chưa đủ dữ liệu để hiển thị thống kê");
        }
    }

    private void renderCharts(AnalyticsSummary summary) {
        renderCategoryBarChart(summary.categoryExpenses);
        renderMonthlyLineChart(summary.monthlyTrend);
        textTotalIncome.setText("Thu: " + formatCurrency(summary.totalIncome));
        textTotalExpense.setText("Chi: " + formatCurrency(summary.totalExpense));
        textSavingsRate.setText("Tỷ lệ tiết kiệm: " + summary.savingsRatePercent + "%");
    }

    private void renderCategoryBarChart(List<CategoryExpense> categoryExpenses) {
        Map<String, Long> totalByLabel = new LinkedHashMap<>();
        for (CategoryExpense item : categoryExpenses) {
            String name = item.categoryId != null ? categoryNamesById.get(item.categoryId) : null;
            String label = name != null ? name : "Chưa phân loại";
            totalByLabel.merge(label, item.totalAmount, Long::sum);
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(totalByLabel.entrySet());
        Collections.sort(sorted, (a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            entries.add(new BarEntry(i, sorted.get(i).getValue()));
            labels.add(sorted.get(i).getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu theo danh mục");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.color_expense));

        barChartCategory.setData(new BarData(dataSet));
        barChartCategory.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartCategory.invalidate();
    }

    private void renderMonthlyLineChart(List<MonthlyTrend> monthlyTrend) {
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < monthlyTrend.size(); i++) {
            MonthlyTrend item = monthlyTrend.get(i);
            incomeEntries.add(new Entry(i, item.incomeAmount));
            expenseEntries.add(new Entry(i, item.expenseAmount));
            labels.add(item.yearMonth);
        }

        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Thu nhập");
        incomeDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.color_income));
        incomeDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.color_income));

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Chi tiêu");
        expenseDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.color_expense));
        expenseDataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.color_expense));

        lineChartTrend.setData(new LineData(incomeDataSet, expenseDataSet));
        lineChartTrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChartTrend.invalidate();
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }
}