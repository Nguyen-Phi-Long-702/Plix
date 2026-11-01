package com.longvuong.plix.presentation.analytics;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.ChipGroup;
import com.longvuong.plix.R;
import com.longvuong.plix.presentation.common.EmptyStateView;
import com.longvuong.plix.presentation.common.UiState;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AnalyticsFragment extends Fragment {
    private static final String FORECAST_INSUFFICIENT_TITLE = "Chưa đủ dữ liệu để dự báo.";
    private static final String FORECAST_INSUFFICIENT_HINT = "Cần tối thiểu 3–4 tuần có cả thu lẫn chi. Cứ tiếp tục ghi chép nhé!";
    private static final float SPARKLINE_PADDING_DP = 4f;
    private static final float PIE_HOLE_RADIUS_PERCENT = 67f;
    private static final int MIN_BAR_SLOTS = 5; //ít tháng thì vẫn chia đủ 5 ô để cột không bị phình to
    private static final float BAR_WIDTH = 0.4f;
    private static final int[] CATEGORY_COLOR_RES = {
            R.color.chart_category_1,
            R.color.chart_category_2,
            R.color.chart_category_3,
            R.color.chart_category_4,
            R.color.chart_category_5
    };

    private AnalyticsViewModel viewModel;
    private ForecastViewModel forecastViewModel;

    private ChipGroup chipGroupRange;
    private TextView textRangeLabel;

    private View cardForecast;
    private ProgressBar progressForecast;
    private TextView textForecastMessage;
    private TextView textForecastHint;
    private View layoutForecastContent;
    private TextView textForecastAmount;
    private LineChart lineChartForecast;

    private View layoutAnalyticsLoading;
    private EmptyStateView emptyAnalytics;
    private View contentContainer;
    private View cardCategory;
    private PieChart pieChartCategory;
    private LinearLayout layoutCategoryLegend;
    private BarChart barChartTrend;
    private TextView textTotalIncome;
    private TextView textTotalExpense;
    private TextView textSavingsRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        forecastViewModel = new ViewModelProvider(this).get(ForecastViewModel.class);

        bindViews(view);
        setupCharts();
        setupRangeChips();

        viewModel.getAnalyticsState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getRangeLabel().observe(getViewLifecycleOwner(), label -> textRangeLabel.setText(label));
        forecastViewModel.getForecastVisible().observe(getViewLifecycleOwner(), visible -> cardForecast.setVisibility(visible ? View.VISIBLE : View.GONE));
        forecastViewModel.getForecastState().observe(getViewLifecycleOwner(), this::renderForecastState);
        forecastViewModel.loadForecast();
    }

    private void bindViews(View view) {
        chipGroupRange = view.findViewById(R.id.chipGroupRange);
        textRangeLabel = view.findViewById(R.id.textRangeLabel);
        cardForecast = view.findViewById(R.id.cardForecast);
        progressForecast = view.findViewById(R.id.progressForecast);
        textForecastMessage = view.findViewById(R.id.textForecastMessage);
        textForecastHint = view.findViewById(R.id.textForecastHint);
        layoutForecastContent = view.findViewById(R.id.layoutForecastContent);
        textForecastAmount = view.findViewById(R.id.textForecastAmount);
        lineChartForecast = view.findViewById(R.id.lineChartForecast);
        layoutAnalyticsLoading = view.findViewById(R.id.layoutAnalyticsLoading);
        emptyAnalytics = view.findViewById(R.id.emptyAnalytics);
        contentContainer = view.findViewById(R.id.contentContainer);
        cardCategory = view.findViewById(R.id.cardCategory);
        pieChartCategory = view.findViewById(R.id.pieChartCategory);
        layoutCategoryLegend = view.findViewById(R.id.layoutCategoryLegend);
        barChartTrend = view.findViewById(R.id.barChartTrend);
        textTotalIncome = view.findViewById(R.id.textTotalIncome);
        textTotalExpense = view.findViewById(R.id.textTotalExpense);
        textSavingsRate = view.findViewById(R.id.textSavingsRate);
    }

    private void setupRangeChips() {
        chipGroupRange.check(chipIdForPreset(viewModel.getSelectedRangePreset()));
        chipGroupRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            viewModel.setRangePreset(presetForChipId(checkedIds.get(0)));
        });
    }

    private int chipIdForPreset(String preset) {
        switch (preset) {
            case AnalyticsViewModel.RANGE_WEEK:
                return R.id.chipRangeWeek;
            case AnalyticsViewModel.RANGE_3_MONTHS:
                return R.id.chipRange3Months;
            case AnalyticsViewModel.RANGE_6_MONTHS:
                return R.id.chipRange6Months;
            default:
                return R.id.chipRangeMonth;
        }
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
        pieChartCategory.getDescription().setEnabled(false);
        pieChartCategory.getLegend().setEnabled(false);
        pieChartCategory.setTouchEnabled(false);
        pieChartCategory.setDrawEntryLabels(false);
        pieChartCategory.setHoleRadius(PIE_HOLE_RADIUS_PERCENT);
        pieChartCategory.setHoleColor(ContextCompat.getColor(requireContext(), R.color.surface));
        pieChartCategory.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        pieChartCategory.setCenterTextSize(11f);
        pieChartCategory.setCenterTextTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold));

        barChartTrend.getDescription().setEnabled(false);
        barChartTrend.getLegend().setEnabled(false);
        barChartTrend.setTouchEnabled(false);
        barChartTrend.setFitBars(true);
        barChartTrend.getAxisLeft().setEnabled(false);
        barChartTrend.getAxisLeft().setAxisMinimum(0f);
        barChartTrend.getAxisRight().setEnabled(false);
        XAxis barXAxis = barChartTrend.getXAxis();
        barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barXAxis.setGranularity(1f);
        barXAxis.setDrawGridLines(false);
        barXAxis.setDrawAxisLine(false);
        barXAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
        barXAxis.setTextSize(10f);
        barXAxis.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_semibold));

        lineChartForecast.getDescription().setEnabled(false);
        lineChartForecast.getLegend().setEnabled(false);
        lineChartForecast.getAxisLeft().setEnabled(false);
        lineChartForecast.getAxisRight().setEnabled(false);
        lineChartForecast.getXAxis().setEnabled(false);
        lineChartForecast.setTouchEnabled(false);
        lineChartForecast.setMinOffset(0f);
        lineChartForecast.setExtraOffsets(SPARKLINE_PADDING_DP, SPARKLINE_PADDING_DP, SPARKLINE_PADDING_DP, SPARKLINE_PADDING_DP);
    }

    private void renderState(UiState<AnalyticsUiModel> state) {
        layoutAnalyticsLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        emptyAnalytics.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            renderContent(((UiState.Success<AnalyticsUiModel>) state).data);
        }
    }

    private void renderContent(AnalyticsUiModel model) {
        boolean hasExpense = !model.categorySlices.isEmpty();
        cardCategory.setVisibility(hasExpense ? View.VISIBLE : View.GONE);
        if (hasExpense) {
            renderCategoryPie(model);
            renderCategoryLegend(model.categorySlices);
        }
        renderTrendBars(model.monthBars);
        textTotalIncome.setText(formatCurrency(model.totalIncome));
        textTotalExpense.setText(formatCurrency(model.totalExpense));
        textSavingsRate.setText(model.savingsRatePercent + "%");
    }

    private void renderCategoryPie(AnalyticsUiModel model) {
        List<PieEntry> entries = new ArrayList<>();
        int[] colors = new int[model.categorySlices.size()];
        for (int i = 0; i < colors.length; i++) {
            AnalyticsUiModel.CategorySlice slice = model.categorySlices.get(i);
            entries.add(new PieEntry(slice.amount));
            colors[i] = categoryColor(slice.colorIndex);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        pieChartCategory.setData(new PieData(dataSet));
        pieChartCategory.setCenterText(model.centerLabel);
        pieChartCategory.invalidate();
    }

    private void renderCategoryLegend(List<AnalyticsUiModel.CategorySlice> slices) {
        layoutCategoryLegend.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AnalyticsUiModel.CategorySlice slice : slices) {
            View row = inflater.inflate(R.layout.item_analytics_legend, layoutCategoryLegend, false);
            row.findViewById(R.id.viewLegendDot).setBackgroundTintList(ColorStateList.valueOf(categoryColor(slice.colorIndex)));
            ((TextView) row.findViewById(R.id.textLegendName)).setText(slice.label);
            ((TextView) row.findViewById(R.id.textLegendPercent)).setText(slice.percent + "%");
            layoutCategoryLegend.addView(row);
        }
    }

    private int categoryColor(int colorIndex) {
        return ContextCompat.getColor(requireContext(), CATEGORY_COLOR_RES[colorIndex]);
    }

    private void renderTrendBars(List<AnalyticsUiModel.MonthBar> bars) {
        int slots = Math.max(bars.size(), MIN_BAR_SLOTS);
        int firstSlot = (slots - bars.size()) / 2; //đặt các cột vào giữa; ô trống không có nhãn
        String[] labels = new String[slots];
        Arrays.fill(labels, "");

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < bars.size(); i++) {
            AnalyticsUiModel.MonthBar bar = bars.get(i);
            entries.add(new BarEntry(firstSlot + i, new float[]{bar.expenseAmount, bar.remainingAmount}));
            labels[firstSlot + i] = bar.label;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.color_expense),
                ContextCompat.getColor(requireContext(), R.color.color_income));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(BAR_WIDTH);

        XAxis xAxis = barChartTrend.getXAxis();
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(slots - 0.5f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartTrend.setData(barData);
        barChartTrend.invalidate();
    }

    private void renderForecastState(UiState<ForecastUiModel> state) {
        progressForecast.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textForecastMessage.setVisibility((state instanceof UiState.Empty || state instanceof UiState.Error) ? View.VISIBLE : View.GONE);
        textForecastHint.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        layoutForecastContent.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            renderForecastChart(((UiState.Success<ForecastUiModel>) state).data);
        } else if (state instanceof UiState.Error) {
            textForecastMessage.setText(((UiState.Error<ForecastUiModel>) state).message);
        } else if (state instanceof UiState.Empty) {
            textForecastMessage.setText(FORECAST_INSUFFICIENT_TITLE);
            textForecastHint.setText(FORECAST_INSUFFICIENT_HINT);
        }
    }

    private void renderForecastChart(ForecastUiModel forecast) {
        textForecastAmount.setText(forecast.forecastNetAmountText);

        int lastActualDay = forecast.actualNetByDay.size(); //ngày hôm nay trong tháng
        List<Entry> actualEntries = new ArrayList<>();
        for (int i = 0; i < lastActualDay; i++) {
            actualEntries.add(new Entry(i + 1, forecast.actualNetByDay.get(i)));
        }

        //Nét đứt: từ điểm thực tế của hôm nay tới điểm dự báo cuối tháng
        List<Entry> projectedEntries = new ArrayList<>();
        if (forecast.daysInMonth > lastActualDay) {
            projectedEntries.add(new Entry(lastActualDay, forecast.actualNetByDay.get(lastActualDay - 1)));
        }
        projectedEntries.add(new Entry(forecast.daysInMonth, forecast.forecastNetAmount));

        int lineColor = ContextCompat.getColor(requireContext(), R.color.white);
        LineDataSet actualDataSet = new LineDataSet(actualEntries, "Thực tế");
        styleForecastDataSet(actualDataSet, lineColor, false);
        LineDataSet projectedDataSet = new LineDataSet(projectedEntries, "Dự kiến");
        styleForecastDataSet(projectedDataSet, lineColor, true);

        lineChartForecast.getXAxis().setAxisMinimum(1f);
        lineChartForecast.getXAxis().setAxisMaximum(forecast.daysInMonth);
        lineChartForecast.setData(new LineData(actualDataSet, projectedDataSet));
        lineChartForecast.invalidate();
    }

    private void styleForecastDataSet(LineDataSet dataSet, int color, boolean dashed) {
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(2.5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(dashed); //nét liền có nhiều điểm nên không vẽ chấm; nét đứt chỉ có 1–2 điểm nên vẽ chấm
        if (dashed) {
            dataSet.enableDashedLine(12f, 8f, 0f);
        }
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }
}