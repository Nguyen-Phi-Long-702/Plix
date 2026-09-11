package com.longvuong.plix.presentation.transaction;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.presentation.common.UiState;
import android.app.DatePickerDialog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransactionListFragment extends Fragment {
    private TransactionListViewModel viewModel;
    private TransactionAdapter adapter;
    private RecyclerView recyclerTransactions;
    private ProgressBar progressLoading;
    private TextView textEmptyState;
    private ChipGroup chipGroupCategory;
    private Chip chipCategoryAll;
    private Chip chipDateRange;
    private Long selectedStartDate;
    private Long selectedEndDate;

    private final Map<Integer, String> chipIdToCategoryId = new HashMap<>();
    private boolean categoryChipsBuilt = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TransactionListViewModel.class);
        bindViews(view);
        setupRecyclerView();
        setupCategoryChips();
        setupDateRangeChip();

        viewModel.getTransactionListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.submitCategories(categories);
            buildCategoryChips(categories);
        });
    }

    private void bindViews(View view) {
        recyclerTransactions = view.findViewById(R.id.recyclerTransactions);
        progressLoading = view.findViewById(R.id.progressLoading);
        textEmptyState = view.findViewById(R.id.textEmptyState);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipCategoryAll = view.findViewById(R.id.chipCategoryAll);
        chipDateRange = view.findViewById(R.id.chipDateRange);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupCategoryChips() {
        chipIdToCategoryId.put(chipCategoryAll.getId(), null);
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            viewModel.setSelectedCategory(chipIdToCategoryId.get(checkedIds.get(0)));
        });
    }

    private void buildCategoryChips(List<CategoryEntity> categories) {
        if (categoryChipsBuilt || categories == null) {
            return;
        }
        categoryChipsBuilt = true;

        for (CategoryEntity category : categories) {
            Chip chip = new Chip(new ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice));
            chip.setId(View.generateViewId());
            chip.setText(category.name);
            chip.setCheckable(true);
            chipGroupCategory.addView(chip);
            chipIdToCategoryId.put(chip.getId(), category.id);
        }
    }

    private void setupDateRangeChip() {
        chipDateRange.setOnClickListener(v -> openStartDatePicker());
        chipDateRange.setOnCloseIconClickListener(v -> clearDateRange());
    }

    private void openStartDatePicker() {
        LocalDate initial = selectedStartDate != null ? toLocalDate(selectedStartDate) : LocalDate.now();

        new DatePickerDialog(requireContext(), (picker, year, month, dayOfMonth) -> {
            LocalDate startDate = LocalDate.of(year, month + 1, dayOfMonth);
            openEndDatePicker(startDate);
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openEndDatePicker(LocalDate startDate) {
        new DatePickerDialog(requireContext(), (picker, year, month, dayOfMonth) -> {
            LocalDate endDate = LocalDate.of(year, month + 1, dayOfMonth);
            applyDateRange(startDate, endDate);
        }, startDate.getYear(), startDate.getMonthValue() - 1, startDate.getDayOfMonth()).show();
    }

    private void applyDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate actualStart = startDate.isAfter(endDate) ? endDate : startDate;
        LocalDate actualEnd = startDate.isAfter(endDate) ? startDate : endDate;
        ZoneId zone = ZoneId.systemDefault();
        selectedStartDate = actualStart.atStartOfDay(zone).toInstant().toEpochMilli();
        selectedEndDate = actualEnd.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        chipDateRange.setText(actualStart.format(formatter) + " - " + actualEnd.format(formatter));
        chipDateRange.setCloseIconVisible(true);
        viewModel.setDateRange(selectedStartDate, selectedEndDate);
    }

    private void clearDateRange() {
        selectedStartDate = null;
        selectedEndDate = null;
        chipDateRange.setText("Khoảng thời gian");
        chipDateRange.setCloseIconVisible(false);
        viewModel.setDateRange(null, null);
    }

    private LocalDate toLocalDate(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void renderState(UiState<List<TransactionEntity>> state) {
        progressLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textEmptyState.setVisibility(
                (state instanceof UiState.Empty || state instanceof UiState.Error) ? View.VISIBLE : View.GONE);
        recyclerTransactions.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            adapter.submitList(((UiState.Success<List<TransactionEntity>>) state).data);
        } else if (state instanceof UiState.Error) {
            textEmptyState.setText(((UiState.Error<List<TransactionEntity>>) state).message);
        } else if (state instanceof UiState.Empty) {
            textEmptyState.setText("Chưa có giao dịch nào phù hợp");
        }
    }
}