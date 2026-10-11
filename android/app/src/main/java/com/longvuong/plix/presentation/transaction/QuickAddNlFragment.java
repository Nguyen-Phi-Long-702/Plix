package com.longvuong.plix.presentation.transaction;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.longvuong.plix.R;
import com.longvuong.plix.presentation.common.UiState;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuickAddNlFragment extends Fragment {
    private static final String ANALYZE_LABEL = "Phân tích bằng AI";
    private static final String ANALYZING_LABEL = "Đang phân tích...";

    private QuickAddNlViewModel viewModel;
    private TextInputEditText editQuickText;
    private MaterialButton buttonAnalyze;
    private TextView textResult;
    private TextView textError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_add_nl, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(QuickAddNlViewModel.class);

        bindViews(view);
        setupAnalyzeButton();

        viewModel.getParseState().observe(getViewLifecycleOwner(), this::renderParseState);
    }

    private void bindViews(View view) {
        editQuickText = view.findViewById(R.id.editQuickText);
        buttonAnalyze = view.findViewById(R.id.buttonAnalyze);
        textResult = view.findViewById(R.id.textResult);
        textError = view.findViewById(R.id.textError);
    }

    private void setupAnalyzeButton() {
        buttonAnalyze.setOnClickListener(v -> {
            hideKeyboard();
            String text = editQuickText.getText() != null ? editQuickText.getText().toString() : "";
            viewModel.submit(text);
        });
    }

    private void renderParseState(UiState<String> state) {
        boolean loading = state instanceof UiState.Loading;
        editQuickText.setEnabled(!loading);
        buttonAnalyze.setEnabled(!loading);
        buttonAnalyze.setText(loading ? ANALYZING_LABEL : ANALYZE_LABEL);
        textResult.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);
        textError.setVisibility(state instanceof UiState.Error ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            textResult.setText(((UiState.Success<String>) state).data);
        } else if (state instanceof UiState.Error) {
            textError.setText(((UiState.Error<String>) state).message);
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