package com.longvuong.plix.presentation.goal;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.longvuong.plix.presentation.common.AmountTextWatcher;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.presentation.common.SnackbarHelper;
import com.longvuong.plix.presentation.common.UiState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditGoalFragment extends Fragment {
    private AddEditGoalViewModel viewModel;
    private TextInputLayout inputLayoutName;
    private TextInputEditText editName;
    private TextInputLayout inputLayoutTargetAmount;
    private TextInputEditText editTargetAmount;
    private TextInputLayout inputLayoutDeadline;
    private TextInputEditText editDeadline;
    private View layoutCurrentAmount;
    private TextInputLayout inputLayoutCurrentAmount;
    private TextInputEditText editCurrentAmount;
    private TextView textGoalInfo;
    private MaterialButton buttonSave;
    private AmountTextWatcher targetAmountWatcher;
    private AmountTextWatcher currentAmountWatcher;
    private boolean formPopulated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddEditGoalViewModel.class);
        String goalId = getArguments() != null ? getArguments().getString(AddEditGoalViewModel.ARG_GOAL_ID) : null;
        viewModel.init(goalId);
        requireActivity().setTitle(viewModel.isEditMode() ? "Sửa mục tiêu" : "Thêm mục tiêu");
        bindViews(view);
        setupNameField();
        setupTargetAmountField();
        setupDeadlineField();
        setupCurrentAmountField();
        setupSaveButton();
        viewModel.getFormReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                populateFormOnce();
            }
        });
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
    }

    private void bindViews(View view) {
        inputLayoutName = view.findViewById(R.id.inputLayoutGoalName);
        editName = view.findViewById(R.id.editGoalName);
        inputLayoutTargetAmount = view.findViewById(R.id.inputLayoutTargetAmount);
        editTargetAmount = view.findViewById(R.id.editTargetAmount);
        inputLayoutDeadline = view.findViewById(R.id.inputLayoutDeadline);
        editDeadline = view.findViewById(R.id.editDeadline);
        layoutCurrentAmount = view.findViewById(R.id.layoutCurrentAmount);
        inputLayoutCurrentAmount = view.findViewById(R.id.inputLayoutCurrentAmount);
        editCurrentAmount = view.findViewById(R.id.editCurrentAmount);
        textGoalInfo = view.findViewById(R.id.textGoalInfo);
        buttonSave = view.findViewById(R.id.buttonSave);
    }

    private void setupNameField() {
        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setName(s.toString());
                Result<Void> validation = viewModel.validateNameField(s.toString());
                inputLayoutName.setError(validation instanceof Result.Error ? ((Result.Error<Void>) validation).message : null);
            }
        });
    }

    private void setupTargetAmountField() {
        editTargetAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        targetAmountWatcher = new AmountTextWatcher(editTargetAmount, amount -> {
            viewModel.setTargetAmount(amount);
            Result<Void> validation = viewModel.validateTargetAmountField(amount);
            inputLayoutTargetAmount.setError(validation instanceof Result.Error ? ((Result.Error<Void>) validation).message : null);
        });
    }

    private void setupDeadlineField() {
        editDeadline.setOnClickListener(v -> openDeadlinePicker());
        inputLayoutDeadline.setEndIconOnClickListener(v -> openDeadlinePicker());
    }

    private void openDeadlinePicker() {
        LocalDate current = Instant.ofEpochMilli(viewModel.getDeadline()).atZone(ZoneId.systemDefault()).toLocalDate();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), R.style.Theme_Plix_DatePicker,
                (picker, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    long newDeadline = selected.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                    Result<Void> validation = viewModel.validateDeadlineField(newDeadline);
                    if (validation instanceof Result.Error) {
                        inputLayoutDeadline.setError(((Result.Error<Void>) validation).message);
                    } else {
                        inputLayoutDeadline.setError(null);
                        viewModel.setDeadline(newDeadline);
                        editDeadline.setText(formatDate(newDeadline));
                    }
                },
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        dialog.show();
    }

    private String formatDate(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void setupCurrentAmountField() {
        editCurrentAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        currentAmountWatcher = new AmountTextWatcher(editCurrentAmount, amount -> {
            viewModel.setCurrentAmount(amount);
            Result<Void> validation = viewModel.validateCurrentAmountField(amount);
            inputLayoutCurrentAmount.setError(validation instanceof Result.Error ? ((Result.Error<Void>) validation).message : null);
            updatePreview();
        });
    }

    private void setupSaveButton() {
        buttonSave.setText(viewModel.isEditMode() ? "Lưu thay đổi" : "Lưu mục tiêu");
        buttonSave.setOnClickListener(v -> {
            hideKeyboard();
            boolean valid = true;
            if (!viewModel.isEditMode()) {
                Result<Void> nameValidation = viewModel.validateNameField(viewModel.getName());
                if (nameValidation instanceof Result.Error) {
                    inputLayoutName.setError(((Result.Error<Void>) nameValidation).message);
                    valid = false;
                }
                Result<Void> targetValidation = viewModel.validateTargetAmountField(viewModel.getTargetAmount());
                if (targetValidation instanceof Result.Error) {
                    inputLayoutTargetAmount.setError(((Result.Error<Void>) targetValidation).message);
                    valid = false;
                }
                Result<Void> deadlineValidation = viewModel.validateDeadlineField(viewModel.getDeadline());
                if (deadlineValidation instanceof Result.Error) {
                    inputLayoutDeadline.setError(((Result.Error<Void>) deadlineValidation).message);
                    valid = false;
                }
            } else {
                Result<Void> currentValidation = viewModel.validateCurrentAmountField(viewModel.getCurrentAmount());
                if (currentValidation instanceof Result.Error) {
                    inputLayoutCurrentAmount.setError(((Result.Error<Void>) currentValidation).message);
                    valid = false;
                }
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

        editName.setText(viewModel.getName());
        targetAmountWatcher.setAmount(viewModel.getTargetAmount());
        editDeadline.setText(formatDate(viewModel.getDeadline()));

        if (viewModel.isEditMode()) {
            editName.setEnabled(false);
            editTargetAmount.setEnabled(false);
            editDeadline.setEnabled(false);
            editDeadline.setOnClickListener(null);
            inputLayoutDeadline.setEndIconOnClickListener(null);

            layoutCurrentAmount.setVisibility(View.VISIBLE);
            currentAmountWatcher.setAmount(viewModel.getCurrentAmount());
            updatePreview();
        }
    }

    private void updatePreview() {
        textGoalInfo.setText(viewModel.getRequiredMonthlyText() + " (tính lại tự động mỗi lần mở màn hình)");
    }

    private void renderSaveState(UiState<Void> state) {
        if (state instanceof UiState.Loading) {
            buttonSave.setEnabled(false);
        } else if (state instanceof UiState.Success) {
            buttonSave.setEnabled(true);
            SnackbarHelper.showSuccess(this, viewModel.isEditMode() ? "Đã lưu thay đổi" : "Đã tạo mục tiêu");
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