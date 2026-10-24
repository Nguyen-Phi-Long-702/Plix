package com.longvuong.plix.presentation.transaction;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.EmptyStateView;
import com.longvuong.plix.presentation.common.SnackbarHelper;
import com.longvuong.plix.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuickAddNlFragment extends Fragment {
    private static final String ANALYZE_LABEL = "Phân tích bằng AI";
    private static final String ANALYZING_LABEL = "Đang phân tích...";

    private QuickAddNlViewModel viewModel;
    private SuggestedTransactionAdapter adapter;
    private View scrollContent;
    private EmptyStateView viewOfflineState;
    private TextInputEditText editQuickText;
    private MaterialButton buttonAnalyze;
    private View layoutAiBusy;
    private MaterialButton buttonEnterManually;
    private TextView textError;
    private TextView textEmptyResult;
    private View layoutParsing;
    private View layoutResult;
    private TextView textResultTitle;
    private RecyclerView recyclerSuggestions;
    private MaterialButton buttonSaveSuggestions;
    private View layoutPrivacyInfo;
    private List<CategoryEntity> categoryOptions = new ArrayList<>();
    private boolean parsing = false;
    private boolean saving = false;
    private boolean canSave = false;

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
        setupRecyclerView();
        setupSaveButton();
        setupEnterManuallyButton();

        viewModel.getParseState().observe(getViewLifecycleOwner(), this::renderParseState);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
        viewModel.getAiBusy().observe(getViewLifecycleOwner(), this::renderAiBusy);
        viewModel.getOffline().observe(getViewLifecycleOwner(), this::renderOffline);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(),
                categories -> categoryOptions = categories != null ? categories : new ArrayList<>());
    }

    private void bindViews(View view) {
        scrollContent = view.findViewById(R.id.scrollContent);
        viewOfflineState = view.findViewById(R.id.viewOfflineState);
        editQuickText = view.findViewById(R.id.editQuickText);
        buttonAnalyze = view.findViewById(R.id.buttonAnalyze);
        layoutAiBusy = view.findViewById(R.id.layoutAiBusy);
        buttonEnterManually = view.findViewById(R.id.buttonEnterManually);
        textError = view.findViewById(R.id.textError);
        textEmptyResult = view.findViewById(R.id.textEmptyResult);
        layoutParsing = view.findViewById(R.id.layoutParsing);
        layoutResult = view.findViewById(R.id.layoutResult);
        textResultTitle = view.findViewById(R.id.textResultTitle);
        recyclerSuggestions = view.findViewById(R.id.recyclerSuggestions);
        buttonSaveSuggestions = view.findViewById(R.id.buttonSaveSuggestions);
        layoutPrivacyInfo = view.findViewById(R.id.layoutPrivacyInfo);
    }

    private void setupAnalyzeButton() {
        buttonAnalyze.setOnClickListener(v -> {
            hideKeyboard();
            String text = editQuickText.getText() != null ? editQuickText.getText().toString() : "";
            viewModel.submit(text);
        });
    }

    private void setupRecyclerView() {
        adapter = new SuggestedTransactionAdapter(new SuggestedTransactionAdapter.OnSuggestionActionListener() {
            @Override
            public void onSelectionChanged(String itemId, boolean selected) {
                viewModel.setItemSelected(itemId, selected);
            }

            @Override
            public void onEditSuggestion(SuggestionItemUiModel item) {
                showEditDialog(item);
            }
        });
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSuggestions.setAdapter(adapter);
    }

    private void setupSaveButton() {
        buttonSaveSuggestions.setOnClickListener(v -> viewModel.saveSelected());
    }

    private void setupEnterManuallyButton() {
        buttonEnterManually.setOnClickListener(v -> navigateToManualEntry());
        viewOfflineState.setOnActionClickListener(v -> navigateToManualEntry());
    }

    private void navigateToManualEntry() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_quickAddNlFragment_to_addEditTransactionFragment);
    }

    private void renderAiBusy(Boolean aiBusy) {
        layoutAiBusy.setVisibility(Boolean.TRUE.equals(aiBusy) ? View.VISIBLE : View.GONE);
        refreshControls();
    }

    //Mất mạng: thay toàn bộ nội dung bằng khối "Cần có kết nối mạng"; có mạng lại thì hiện lại nội dung cũ (kết quả đang xem vẫn còn)
    private void renderOffline(Boolean offline) {
        boolean isOffline = Boolean.TRUE.equals(offline);
        if (isOffline) {
            hideKeyboard(); //đóng bàn phím trước khi ô nhập bị ẩn đi
        }
        scrollContent.setVisibility(isOffline ? View.GONE : View.VISIBLE);
        viewOfflineState.setVisibility(isOffline ? View.VISIBLE : View.GONE);
    }

    private void renderParseState(UiState<SuggestionListUiModel> state) {
        parsing = state instanceof UiState.Loading;
        boolean aiBusy = Boolean.TRUE.equals(viewModel.getAiBusy().getValue());
        textError.setVisibility(state instanceof UiState.Error && !aiBusy ? View.VISIBLE : View.GONE);
        textEmptyResult.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        layoutParsing.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        layoutResult.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Error) {
            textError.setText(((UiState.Error<SuggestionListUiModel>) state).message);
        } else if (state instanceof UiState.Success) {
            SuggestionListUiModel model = ((UiState.Success<SuggestionListUiModel>) state).data;
            textResultTitle.setText(model.titleText);
            adapter.submitList(model.items);
            buttonSaveSuggestions.setText(model.saveButtonText);
            canSave = model.canSave;
        }
        refreshControls();
    }

    private void renderSaveState(@Nullable UiState<Integer> state) {
        saving = state instanceof UiState.Loading;
        if (state instanceof UiState.Success) {
            int savedCount = ((UiState.Success<Integer>) state).data;
            SnackbarHelper.showSuccess(this, "Đã lưu " + savedCount + " giao dịch");
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        if (state instanceof UiState.Error) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    ((UiState.Error<Integer>) state).message, Snackbar.LENGTH_LONG).show();
        }
        refreshControls();
    }

    private void refreshControls() {
        boolean busy = parsing || saving;
        editQuickText.setEnabled(!busy);
        buttonAnalyze.setEnabled(!busy);
        buttonAnalyze.setText(parsing ? ANALYZING_LABEL : ANALYZE_LABEL);
        buttonSaveSuggestions.setEnabled(canSave && !busy);
        //Khung thông tin quyền riêng tư ẩn khi đang phân tích hoặc khi hiện khối "AI đang bận", theo thiết kế
        boolean aiBusy = Boolean.TRUE.equals(viewModel.getAiBusy().getValue());
        layoutPrivacyInfo.setVisibility(parsing || aiBusy ? View.GONE : View.VISIBLE);
    }

    private void showEditDialog(SuggestionItemUiModel item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_suggestion, null, false);
        TextInputLayout inputLayoutAmount = dialogView.findViewById(R.id.inputLayoutSuggestionAmount);
        TextInputEditText editAmount = dialogView.findViewById(R.id.editSuggestionAmount);
        TextInputLayout inputLayoutCategory = dialogView.findViewById(R.id.inputLayoutSuggestionCategory);
        MaterialAutoCompleteTextView editCategory = dialogView.findViewById(R.id.editSuggestionCategory);

        List<CategoryEntity> options = new ArrayList<>(categoryOptions);
        List<String> labels = new ArrayList<>();
        CategoryEntity[] selectedCategory = new CategoryEntity[1];
        for (CategoryEntity category : options) {
            labels.add(category.name + " · " + ("income".equals(category.type) ? "Thu nhập" : "Chi tiêu"));
            if (category.id.equals(item.categoryId)) {
                selectedCategory[0] = category;
            }
        }
        editCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        if (selectedCategory[0] != null) {
            editCategory.setText(labels.get(options.indexOf(selectedCategory[0])), false);
        }
        editCategory.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position >= 0 && position < options.size()) {
                selectedCategory[0] = options.get(position);
                inputLayoutCategory.setError(null);
            }
        });
        editAmount.setText(String.valueOf(item.amount));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Sửa giao dịch đề xuất")
                .setView(dialogView)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(d -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(v -> {
                boolean valid = true;

                long amount = parseAmount(editAmount.getText() != null ? editAmount.getText().toString() : "");
                Result<Void> amountValidation = viewModel.validateAmountField(amount);
                if (amountValidation instanceof Result.Error) {
                    inputLayoutAmount.setError(((Result.Error<Void>) amountValidation).message);
                    valid = false;
                } else {
                    inputLayoutAmount.setError(null);
                }

                if (selectedCategory[0] == null) {
                    inputLayoutCategory.setError("Vui lòng chọn danh mục");
                    valid = false;
                }

                if (!valid) {
                    return;
                }
                viewModel.updateItem(item.itemId, amount, selectedCategory[0]);
                dialog.dismiss();
            });
        });
        dialog.show();
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = requireActivity().getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }
}