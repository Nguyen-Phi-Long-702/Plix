package com.longvuong.plix.presentation.budget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.longvuong.plix.presentation.common.AmountTextWatcher;
import com.longvuong.plix.presentation.common.CategoryIconMapper;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.SnackbarHelper;
import com.longvuong.plix.presentation.common.UiState;

import java.time.YearMonth;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditBudgetFragment extends Fragment {
    private AddEditBudgetViewModel viewModel;
    private MaterialButtonToggleGroup toggleScope;
    private TextInputLayout inputLayoutPeriod;
    private TextInputEditText editPeriod;
    private View layoutCategoryField;
    private ChipGroup chipGroupCategory;
    private TextView textCategoryError;
    private TextInputLayout inputLayoutLimitAmount;
    private TextInputEditText editLimitAmount;
    private TextView textThresholdLabel;
    private Slider sliderThreshold;
    private MaterialButton buttonSave;
    private AmountTextWatcher limitAmountWatcher;
    private final Map<Integer, String> categoryIdByChipId = new HashMap<>();
    private boolean suppressCategoryChipListener = false;
    private boolean formPopulated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddEditBudgetViewModel.class);
        String budgetId = getArguments() != null
                ? getArguments().getString(AddEditBudgetViewModel.ARG_BUDGET_ID)
                : null;
        viewModel.init(budgetId);
        requireActivity().setTitle(viewModel.isEditMode() ? "Sửa ngân sách" : "Thêm ngân sách");
        bindViews(view);
        setupScopeToggle();
        setupPeriodField();
        setupCategoryChips();
        setupLimitAmountField();
        setupThresholdSlider();
        setupSaveButton();
        viewModel.getFormReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                populateFormOnce();
            }
        });
        viewModel.getExpenseCategories().observe(getViewLifecycleOwner(), this::populateCategoryChips);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
    }

    private void bindViews(View view) {
        toggleScope = view.findViewById(R.id.toggleScope);
        inputLayoutPeriod = view.findViewById(R.id.inputLayoutPeriod);
        editPeriod = view.findViewById(R.id.editPeriod);
        layoutCategoryField = view.findViewById(R.id.layoutCategoryField);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        textCategoryError = view.findViewById(R.id.textCategoryError);
        inputLayoutLimitAmount = view.findViewById(R.id.inputLayoutLimitAmount);
        editLimitAmount = view.findViewById(R.id.editLimitAmount);
        textThresholdLabel = view.findViewById(R.id.textThresholdLabel);
        sliderThreshold = view.findViewById(R.id.sliderThreshold);
        buttonSave = view.findViewById(R.id.buttonSave);
    }

    private void setupScopeToggle() {
        toggleScope.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean overall = checkedId == R.id.btnScopeOverall;
            viewModel.setOverall(overall);
            layoutCategoryField.setVisibility(overall ? View.GONE : View.VISIBLE);
            if (overall) {
                textCategoryError.setVisibility(View.GONE);
                refreshCategoryChipSelection();
            }
        });
    }

    private void setupPeriodField() {
        editPeriod.setOnClickListener(v -> openPeriodPicker());
        inputLayoutPeriod.setEndIconOnClickListener(v -> openPeriodPicker());
    }

    private void openPeriodPicker() {
        YearMonth current = YearMonth.parse(viewModel.getPeriod());
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    YearMonth selected = YearMonth.of(year, month + 1);
                    viewModel.setPeriod(selected.toString());
                    editPeriod.setText(formatPeriod(selected));
                    inputLayoutPeriod.setError(null);
                },
                current.getYear(), current.getMonthValue() - 1, 1);
        dialog.show();
    }

    private String formatPeriod(YearMonth yearMonth) {
        return "Tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear();
    }

    private void setupCategoryChips() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressCategoryChipListener) {
                return;
            }
            viewModel.setCategoryId(checkedIds.isEmpty() ? null : categoryIdByChipId.get(checkedIds.get(0)));
            textCategoryError.setVisibility(View.GONE);
        });
    }

    private void setupLimitAmountField() {
        editLimitAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        limitAmountWatcher = new AmountTextWatcher(editLimitAmount, amount -> {
            viewModel.setLimitAmount(amount);
            Result<Void> validation = viewModel.validateLimitAmountField(amount);
            inputLayoutLimitAmount.setError(validation instanceof Result.Error
                    ? ((Result.Error<Void>) validation).message : null);
        });
    }

    private void setupThresholdSlider() {
        sliderThreshold.addOnChangeListener((slider, value, fromUser) -> {
            int percent = (int) value;
            textThresholdLabel.setText("Ngưỡng cảnh báo — " + percent + "%");
            if (fromUser) {
                viewModel.setThresholdPercent(percent);
            }
        });
    }

    private void setupSaveButton() {
        buttonSave.setText(viewModel.isEditMode() ? "Lưu thay đổi" : "Lưu ngân sách");
        buttonSave.setOnClickListener(v -> {
            hideKeyboard();
            boolean valid = true;
            Result<Void> periodValidation = viewModel.validatePeriodField(viewModel.getPeriod());
            if (periodValidation instanceof Result.Error) {
                inputLayoutPeriod.setError(((Result.Error<Void>) periodValidation).message);
                valid = false;
            }
            Result<Void> limitValidation = viewModel.validateLimitAmountField(viewModel.getLimitAmount());
            if (limitValidation instanceof Result.Error) {
                inputLayoutLimitAmount.setError(((Result.Error<Void>) limitValidation).message);
                valid = false;
            }
            if (!viewModel.isOverall() && viewModel.getCategoryId() == null) {
                textCategoryError.setVisibility(View.VISIBLE);
                valid = false;
            }
            if (!valid) {
                return;
            }
            viewModel.save();
        });
    }

    private void populateFormOnce() {
        if (formPopulated) {
            return;
        }
        formPopulated = true;
        toggleScope.check(viewModel.isOverall() ? R.id.btnScopeOverall : R.id.btnScopeCategory);
        layoutCategoryField.setVisibility(viewModel.isOverall() ? View.GONE : View.VISIBLE);
        editPeriod.setText(formatPeriod(YearMonth.parse(viewModel.getPeriod())));
        limitAmountWatcher.setAmount(viewModel.getLimitAmount());
        sliderThreshold.setValue(viewModel.getThresholdPercent());
        textThresholdLabel.setText("Ngưỡng cảnh báo — " + viewModel.getThresholdPercent() + "%");
        refreshCategoryChipSelection(); //danh mục có thể nạp xong trước khi ngân sách cần sửa được đọc ra
        if (viewModel.isEditMode()) {
            for (int i = 0; i < toggleScope.getChildCount(); i++) {
                toggleScope.getChildAt(i).setEnabled(false);
            }
            inputLayoutPeriod.setEndIconOnClickListener(null);
            editPeriod.setOnClickListener(null);
            editPeriod.setEnabled(false);
        }
    }

    private void populateCategoryChips(List<CategoryEntity> categories) {
        suppressCategoryChipListener = true;
        chipGroupCategory.removeAllViews();
        categoryIdByChipId.clear();
        if (categories != null) {
            String selectedCategoryId = viewModel.getCategoryId();
            for (CategoryEntity category : categories) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter_category, chipGroupCategory, false);
                chip.setId(View.generateViewId());
                chip.setText(category.name);
                chip.setChipIcon(ContextCompat.getDrawable(requireContext(), CategoryIconMapper.iconFor(category.id)));
                chip.setEnabled(!viewModel.isEditMode()); //sửa ngân sách không đổi được danh mục
                chipGroupCategory.addView(chip);
                categoryIdByChipId.put(chip.getId(), category.id);
                if (category.id.equals(selectedCategoryId)) {
                    chip.setChecked(true);
                }
            }
        }
        suppressCategoryChipListener = false;
    }

    private void renderSaveState(UiState<Void> state) {
        if (state instanceof UiState.Loading) {
            buttonSave.setEnabled(false);
        } else if (state instanceof UiState.Success) {
            buttonSave.setEnabled(true);
            SnackbarHelper.showSuccess(this, "Đã lưu ngân sách");
            NavHostFragment.findNavController(this).popBackStack();
        } else if (state instanceof UiState.Error) {
            buttonSave.setEnabled(true);
            String message = ((UiState.Error<Void>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = requireActivity().getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void refreshCategoryChipSelection() {
        String selectedCategoryId = viewModel.getCategoryId();
        suppressCategoryChipListener = true;
        chipGroupCategory.clearCheck();
        for (Map.Entry<Integer, String> entry : categoryIdByChipId.entrySet()) {
            if (entry.getValue().equals(selectedCategoryId)) {
                chipGroupCategory.check(entry.getKey());
                break;
            }
        }
        suppressCategoryChipListener = false;
    }
}