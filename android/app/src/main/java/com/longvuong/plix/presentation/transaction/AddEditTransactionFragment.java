package com.longvuong.plix.presentation.transaction;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditTransactionFragment extends Fragment {
    private static final Map<String, String> PAYMENT_METHOD_LABELS = new LinkedHashMap<>();

    static {
        PAYMENT_METHOD_LABELS.put("cash", "Tiền mặt");
        PAYMENT_METHOD_LABELS.put("bank_transfer", "Chuyển khoản");
        PAYMENT_METHOD_LABELS.put("e_wallet", "Ví điện tử");
        PAYMENT_METHOD_LABELS.put("credit_card", "Thẻ tín dụng");
        PAYMENT_METHOD_LABELS.put("other", "Khác");
    }

    private AddEditTransactionViewModel viewModel;

    private MaterialButtonToggleGroup toggleType;
    private TextInputLayout inputLayoutAmount;
    private TextInputEditText editAmount;
    private TextInputLayout inputLayoutCategory;
    private MaterialAutoCompleteTextView editCategory;
    private TextInputLayout inputLayoutDate;
    private TextInputEditText editDate;
    private TextInputLayout inputLayoutPaymentMethod;
    private MaterialAutoCompleteTextView editPaymentMethod;
    private TextInputLayout inputLayoutNote;
    private TextInputEditText editNote;
    private MaterialButton buttonSave;

    private List<CategoryEntity> currentCategoryOptions = new ArrayList<>();
    private boolean formPopulated = false;
    private boolean suppressAmountWatcher = false;
    private boolean suppressNoteWatcher = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AddEditTransactionViewModel.class);

        bindViews(view);
        setupTypeToggle();
        setupAmountField();
        setupCategoryField();
        setupDateField();
        setupPaymentMethodField();
        setupNoteField();
        setupSaveButton();

        viewModel.getFormReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                populateFormOnce();
            }
        });
        viewModel.getFilteredCategories().observe(getViewLifecycleOwner(), this::populateCategoryDropdown);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
    }

    private void bindViews(View view) {
        toggleType = view.findViewById(R.id.toggleType);
        inputLayoutAmount = view.findViewById(R.id.inputLayoutAmount);
        editAmount = view.findViewById(R.id.editAmount);
        inputLayoutCategory = view.findViewById(R.id.inputLayoutCategory);
        editCategory = view.findViewById(R.id.editCategory);
        inputLayoutDate = view.findViewById(R.id.inputLayoutDate);
        editDate = view.findViewById(R.id.editDate);
        inputLayoutPaymentMethod = view.findViewById(R.id.inputLayoutPaymentMethod);
        editPaymentMethod = view.findViewById(R.id.editPaymentMethod);
        inputLayoutNote = view.findViewById(R.id.inputLayoutNote);
        editNote = view.findViewById(R.id.editNote);
        buttonSave = view.findViewById(R.id.buttonSave);
    }

    private void setupTypeToggle() {
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            viewModel.setType(checkedId == R.id.btnIncome ? "income" : "expense");
        });
    }

    private void setupAmountField() {
        editAmount.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        editAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressAmountWatcher) {
                    return;
                }
                long amount = parseAmount(s.toString());
                viewModel.setAmount(amount);
                Result<Void> validation = viewModel.validateAmountField(amount);
                inputLayoutAmount.setError(validation instanceof Result.Error
                        ? ((Result.Error<Void>) validation).message : null);
            }
        });
    }

    private long parseAmount(String raw) {
        String digitsOnly = raw.replaceAll("[^0-9-]", "");
        if (digitsOnly.isEmpty() || digitsOnly.equals("-")) {
            return 0L;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setupCategoryField() {
        editCategory.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < currentCategoryOptions.size()) {
                viewModel.setCategoryId(currentCategoryOptions.get(position).id);
                inputLayoutCategory.setError(null);
            }
        });
    }

    private void setupDateField() {
        editDate.setOnClickListener(v -> openDatePicker());
        inputLayoutDate.setEndIconOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        ZonedDateTime current = Instant.ofEpochMilli(viewModel.getOccurredAt())
                .atZone(ZoneId.systemDefault());

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    LocalDate newDate = LocalDate.of(year, month + 1, dayOfMonth);
                    LocalTime existingTime = current.toLocalTime();
                    long newOccurredAt = ZonedDateTime.of(newDate, existingTime, ZoneId.systemDefault())
                            .toInstant().toEpochMilli();

                    Result<Void> validation = viewModel.validateOccurredAtField(newOccurredAt);
                    if (validation instanceof Result.Error) {
                        inputLayoutDate.setError(((Result.Error<Void>) validation).message);
                    } else {
                        inputLayoutDate.setError(null);
                        viewModel.setOccurredAt(newOccurredAt);
                        editDate.setText(formatDate(newOccurredAt));
                    }
                },
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        dialog.show();
    }

    private String formatDate(long epochMs) {
        return Instant.ofEpochMilli(epochMs)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void setupPaymentMethodField() {
        List<String> labels = new ArrayList<>(PAYMENT_METHOD_LABELS.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, labels);
        editPaymentMethod.setAdapter(adapter);
        editPaymentMethod.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedLabel = labels.get(position);
            for (Map.Entry<String, String> entry : PAYMENT_METHOD_LABELS.entrySet()) {
                if (entry.getValue().equals(selectedLabel)) {
                    viewModel.setPaymentMethod(entry.getKey());
                    break;
                }
            }
        });
    }

    private void setupNoteField() {
        editNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressNoteWatcher) {
                    return;
                }
                String note = s.toString();
                viewModel.setNote(note);
                Result<Void> validation = viewModel.validateNoteField(note);
                inputLayoutNote.setError(validation instanceof Result.Error
                        ? ((Result.Error<Void>) validation).message : null);
            }
        });
    }

    private void setupSaveButton() {
        buttonSave.setText(viewModel.isEditMode() ? "Lưu thay đổi" : "Lưu giao dịch");
        buttonSave.setOnClickListener(v -> {
            hideKeyboard();

            boolean valid = true;

            Result<Void> amountValidation = viewModel.validateAmountField(viewModel.getAmount());
            if (amountValidation instanceof Result.Error) {
                inputLayoutAmount.setError(((Result.Error<Void>) amountValidation).message);
                valid = false;
            }

            Result<Void> noteValidation = viewModel.validateNoteField(viewModel.getNote());
            if (noteValidation instanceof Result.Error) {
                inputLayoutNote.setError(((Result.Error<Void>) noteValidation).message);
                valid = false;
            }

            Result<Void> dateValidation = viewModel.validateOccurredAtField(viewModel.getOccurredAt());
            if (dateValidation instanceof Result.Error) {
                inputLayoutDate.setError(((Result.Error<Void>) dateValidation).message);
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

        toggleType.check("income".equals(viewModel.getType()) ? R.id.btnIncome : R.id.btnExpense);

        suppressAmountWatcher = true;
        editAmount.setText(viewModel.getAmount() == 0 ? "" : String.valueOf(viewModel.getAmount()));
        suppressAmountWatcher = false;

        editDate.setText(formatDate(viewModel.getOccurredAt()));

        String paymentMethodLabel = PAYMENT_METHOD_LABELS.get(viewModel.getPaymentMethod());
        editPaymentMethod.setText(paymentMethodLabel != null ? paymentMethodLabel : "", false);

        suppressNoteWatcher = true;
        editNote.setText(viewModel.getNote());
        suppressNoteWatcher = false;
    }

    private void populateCategoryDropdown(List<CategoryEntity> categories) {
        currentCategoryOptions = categories != null ? categories : new ArrayList<>();

        List<String> names = new ArrayList<>();
        for (CategoryEntity category : currentCategoryOptions) {
            names.add(category.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, names);
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
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    "Đã lưu giao dịch", Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        } else if (state instanceof UiState.Error) {
            buttonSave.setEnabled(true);
            String message = ((UiState.Error<Void>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = requireActivity().getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }
}