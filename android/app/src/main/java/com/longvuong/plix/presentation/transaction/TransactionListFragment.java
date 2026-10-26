package com.longvuong.plix.presentation.transaction;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.presentation.common.CategoryIconMapper;
import com.longvuong.plix.presentation.common.ExpandableFabView;
import com.longvuong.plix.presentation.common.UiState;

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
    private View layoutStateLoading;
    private View viewEmptyState;
    private ExpandableFabView fabExpandable;
    private ChipGroup chipGroupCategory;
    private Chip chipCategoryAll;
    private Chip chipDateRange;
    private TextInputEditText editSearch;
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
        setupSearchField();
        setupFab();

        viewModel.getTransactionListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.submitCategories(categories);
            buildCategoryChips(categories);
        });
        viewModel.getDeleteState().observe(getViewLifecycleOwner(), this::renderDeleteState);
    }

    private void bindViews(View view) {
        recyclerTransactions = view.findViewById(R.id.recyclerTransactions);
        layoutStateLoading = view.findViewById(R.id.layoutStateLoading);
        viewEmptyState = view.findViewById(R.id.viewEmptyState);
        fabExpandable = view.findViewById(R.id.fabExpandable);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipCategoryAll = view.findViewById(R.id.chipCategoryAll);
        chipDateRange = view.findViewById(R.id.chipDateRange);
        editSearch = view.findViewById(R.id.editSearch);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new TransactionAdapter.OnTransactionActionListener() {
            @Override
            public void onEditTransaction(TransactionEntity transaction) {
                navigateToEdit(transaction);
            }

            @Override
            public void onDeleteTransaction(TransactionEntity transaction) {
                showDeleteConfirmationDialog(transaction);
            }
        });
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupFab() {
        fabExpandable.setOnAddTransactionClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_transactionListFragment_to_addEditTransactionFragment));
        //nav_graph.xml chưa có action từ danh sách sang Nhập nhanh (file này chỉ Long sửa) nên đi thẳng tới màn đích theo id
        fabExpandable.setOnQuickAddClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.quickAddNlFragment));
        viewModel.getQuickAddVisible().observe(getViewLifecycleOwner(), visible ->
                fabExpandable.setQuickAddVisible(Boolean.TRUE.equals(visible)));
    }

    private void navigateToEdit(TransactionEntity transaction) {
        Bundle args = new Bundle();
        args.putString(AddEditTransactionViewModel.ARG_TRANSACTION_ID, transaction.id);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_transactionListFragment_to_addEditTransactionFragment, args);
    }

    private void showDeleteConfirmationDialog(TransactionEntity transaction) {
        String label = transaction.note != null && !transaction.note.isEmpty()
                ? transaction.note : "giao dịch này";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá giao dịch?")
                .setMessage("Giao dịch \"" + label + "\" sẽ bị xoá. Hành động này không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (dialog, which) -> viewModel.deleteTransaction(transaction))
                .show();
    }

    private void renderDeleteState(UiState<Void> state) {
        if (state instanceof UiState.Success) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    "Đã xoá giao dịch", Snackbar.LENGTH_SHORT).show();
        } else if (state instanceof UiState.Error) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    ((UiState.Error<Void>) state).message, Snackbar.LENGTH_LONG).show();
        }
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
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter_category, chipGroupCategory, false);
            chip.setId(View.generateViewId());
            chip.setText(category.name);
            chip.setChipIcon(ContextCompat.getDrawable(requireContext(), CategoryIconMapper.iconFor(category.id)));
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

    private void setupSearchField() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });
    }

    private void renderState(UiState<List<TransactionListItem>> state) {
        layoutStateLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        viewEmptyState.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        recyclerTransactions.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            adapter.submitList(((UiState.Success<List<TransactionListItem>>) state).data);
        }
    }
}