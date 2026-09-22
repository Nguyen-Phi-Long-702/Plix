package com.longvuong.plix.presentation.budget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditBudgetFragment extends Fragment {
    private AddEditBudgetViewModel viewModel;
    private MaterialButtonToggleGroup toggleScope;
    private TextInputLayout inputLayoutPeriod;
    private TextInputEditText editPeriod;
    private TextInputLayout inputLayoutCategory;
    private MaterialAutoCompleteTextView editCategory;
    private TextInputLayout inputLayoutLimitAmount;
    private TextInputEditText editLimitAmount;
    private TextView textThresholdLabel;
    private Slider sliderThreshold;
    private MaterialButton buttonSave;
    private List<CategoryEntity> currentCategoryOptions = new ArrayList<>();
    private boolean formPopulated = false;
    private boolean suppressLimitAmountWatcher = false;

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
        bindViews(view);
        setupScopeToggle();
        setupPeriodField();
        setupCategoryField();
        setupLimitAmountField();
        setupThresholdSlider();
        setupSaveButton();
        viewModel.getFormReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                populateFormOnce();
            }
        });
        viewModel.getExpenseCategories().observe(getViewLifecycleOwner(), this::populateCategoryDropdown);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
    }

    private void bindViews(View view) {
        toggleScope = view.findViewById(R.id.toggleScope);
        inputLayoutPeriod = view.findViewById(R.id.inputLayoutPeriod);
        editPeriod = view.findViewById(R.id.editPeriod);
        inputLayoutCategory = view.findViewById(R.id.inputLayoutCategory);
        editCategory = view.findViewById(R.id.editCategory);
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
            inputLayoutCategory.setVisibility(overall ? View.GONE : View.VISIBLE);
            if (overall) {
                inputLayoutCategory.setError(null);
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

    private void setupCategoryField() {
        editCategory.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < currentCategoryOptions.size()) {
                viewModel.setCategoryId(currentCategoryOptions.get(position).id);
                inputLayoutCategory.setError(null);
            }
        });
    }

    private void setupLimitAmountField() {
        editLimitAmount.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editLimitAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        editLimitAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressLimitAmountWatcher) {
                    return;
                }
                long amount = parseAmount(s.toString());
                viewModel.setLimitAmount(amount);
                Result<Void> validation = viewModel.validateLimitAmountField(amount);
                inputLayoutLimitAmount.setError(validation instanceof Result.Error
                        ? ((Result.Error<Void>) validation).message : null);
            }
        });
    }

    private long parseAmount(String raw) {
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setupThresholdSlider() {
        sliderThreshold.addOnChangeListener((slider, value, fromUser) -> {
            int percent = (int) value;
            textThresholdLabel.setText("Ngưỡng cảnh báo: " + percent + "%");
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
                inputLayoutCategory.setError("Vui lòng chọn danh mục cho ngân sách");
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
        inputLayoutCategory.setVisibility(viewModel.isOverall() ? View.GONE : View.VISIBLE);
        editPeriod.setText(formatPeriod(YearMonth.parse(viewModel.getPeriod())));
        suppressLimitAmountWatcher = true;
        editLimitAmount.setText(viewModel.getLimitAmount() == 0 ? "" : String.valueOf(viewModel.getLimitAmount()));
        suppressLimitAmountWatcher = false;
        sliderThreshold.setValue(viewModel.getThresholdPercent());
        textThresholdLabel.setText("Ngưỡng cảnh báo: " + viewModel.getThresholdPercent() + "%");
        if (viewModel.isEditMode()) {
            for (int i = 0; i < toggleScope.getChildCount(); i++) {
                toggleScope.getChildAt(i).setEnabled(false);
            }
            inputLayoutPeriod.setEndIconOnClickListener(null);
            editPeriod.setOnClickListener(null);
            editPeriod.setEnabled(false);
            editCategory.setEnabled(false);
        }
    }

    private void populateCategoryDropdown(List<CategoryEntity> categories) {
        currentCategoryOptions = categories != null ? categories : new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (CategoryEntity category : currentCategoryOptions) {
            names.add(category.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
        editCategory.setAdapter(adapter);
        String selectedCategoryId = viewModel.getCategoryId();
        String selectedName = "";
        for (CategoryEntity category : currentCategoryOptions) {
            if (category.id.equals(selectedCategoryId)) {
                selectedName = category.name;
                break;
            }
        }
        editCategory.setText(selectedName, false);
    }

    private void renderSaveState(UiState<Void> state) {
        if (state instanceof UiState.Loading) {
            buttonSave.setEnabled(false);
        } else if (state instanceof UiState.Success) {
            buttonSave.setEnabled(true);
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Đã lưu ngân sách", Snackbar.LENGTH_SHORT).show();
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
}