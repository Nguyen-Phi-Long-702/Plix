package com.longvuong.plix.presentation.category;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.longvuong.plix.R;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CategoryManagementFragment extends Fragment {
    private CategoryManagementViewModel viewModel;
    private CategoryAdapter adapter;
    private RecyclerView recyclerCategories;
    private ProgressBar progressLoading;
    private TextView textEmptyState;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_management, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CategoryManagementViewModel.class);
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        progressLoading = view.findViewById(R.id.progressLoading);
        textEmptyState = view.findViewById(R.id.textEmptyState);
        FloatingActionButton fabAddCategory = view.findViewById(R.id.fabAddCategory);
        adapter = new CategoryAdapter(new CategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onEditCategory(CategoryEntity category) {
                showCategoryDialog(category);
            }
            @Override
            public void onDeleteCategory(CategoryEntity category) {
                showDeleteConfirmationDialog(category);
            }
        });
        recyclerCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCategories.setAdapter(adapter);

        fabAddCategory.setOnClickListener(v -> showCategoryDialog(null));

        viewModel.getCategoryListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getFormState().observe(getViewLifecycleOwner(), this::renderFormState);
    }
    private void renderState(UiState<List<CategoryEntity>> state) {
        progressLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textEmptyState.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        recyclerCategories.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            adapter.submitList(((UiState.Success<List<CategoryEntity>>) state).data,
                    viewModel.getCurrentUserId());
        }
    }
    private void renderFormState(UiState<Void> state) {
        if (state instanceof UiState.Success) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    "Đã lưu danh mục", Snackbar.LENGTH_SHORT).show();
        } else if (state instanceof UiState.Error) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    ((UiState.Error<Void>) state).message, Snackbar.LENGTH_LONG).show();
        }
    }
    private void showCategoryDialog(@Nullable CategoryEntity existing) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_edit_category, null, false);

        TextInputLayout inputLayoutName = dialogView.findViewById(R.id.inputLayoutCategoryName);
        TextInputEditText editName = dialogView.findViewById(R.id.editCategoryName);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleCategoryType);

        boolean isEdit = existing != null;
        if (isEdit) {
            editName.setText(existing.name);
            toggleType.check("income".equals(existing.type) ? R.id.btnCategoryIncome : R.id.btnCategoryExpense);
        } else {
            toggleType.check(R.id.btnCategoryExpense);
        }
        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Result<Void> validation = viewModel.validateNameField(s.toString());
                inputLayoutName.setError(validation instanceof Result.Error
                        ? ((Result.Error<Void>) validation).message : null);
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "Sửa danh mục" : "Danh mục mới")
                .setView(dialogView)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(d -> {
            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(v -> {
                String name = editName.getText() != null ? editName.getText().toString() : "";
                Result<Void> validation = viewModel.validateNameField(name);
                if (validation instanceof Result.Error) {
                    inputLayoutName.setError(((Result.Error<Void>) validation).message);
                    return;
                }
                inputLayoutName.setError(null);
                String type = toggleType.getCheckedButtonId() == R.id.btnCategoryIncome ? "income" : "expense";
                if (isEdit) {
                    viewModel.updateCategory(existing, name, type);
                } else {
                    viewModel.addCategory(name, type);
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }
    private void showDeleteConfirmationDialog(CategoryEntity category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá danh mục \"" + category.name + "\"?")
                .setMessage("Các giao dịch đang dùng danh mục này sẽ hiển thị \"Chưa phân loại\". " + "Hành động này không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (d, which) -> viewModel.deleteCategory(category))
                .show();
    }
}