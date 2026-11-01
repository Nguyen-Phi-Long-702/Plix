package com.longvuong.plix.presentation.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.presentation.common.EmptyStateView;
import com.longvuong.plix.presentation.common.UiState;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BudgetListFragment extends Fragment {
    private BudgetListViewModel viewModel;
    private BudgetAdapter adapter;
    private RecyclerView recyclerBudgets;
    private View layoutStateLoading;
    private EmptyStateView viewEmptyState;
    private TextView textBudgetPeriod;
    private FloatingActionButton fabAddBudget;
    private MaterialButton btnTabBudget;
    private MaterialButton btnTabGoal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BudgetListViewModel.class);
        bindViews(view);
        setupTabToggle();
        setupRecyclerView();
        fabAddBudget.setOnClickListener(v -> navigateToAddBudget());
        viewEmptyState.setOnActionClickListener(v -> navigateToAddBudget());
        textBudgetPeriod.setText(viewModel.getPeriodLabel());
        viewModel.getBudgetListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getProgressByBudgetId().observe(getViewLifecycleOwner(), adapter::submitProgress);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(), adapter::submitCategories);
        viewModel.getDeleteState().observe(getViewLifecycleOwner(), this::renderDeleteState);
    }

    private void bindViews(View view) {
        recyclerBudgets = view.findViewById(R.id.recyclerBudgets);
        layoutStateLoading = view.findViewById(R.id.layoutStateLoading);
        viewEmptyState = view.findViewById(R.id.viewEmptyState);
        textBudgetPeriod = view.findViewById(R.id.textBudgetPeriod);
        fabAddBudget = view.findViewById(R.id.fabAddBudget);
        btnTabBudget = view.findViewById(R.id.btnTabBudget);
        btnTabGoal = view.findViewById(R.id.btnTabGoal);
    }

    private void setupTabToggle() {
        btnTabBudget.setSelected(true);
        btnTabGoal.setOnClickListener(v -> {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.budgetGoalFragment, true)
                    .build();
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_budgetGoalFragment_to_goalListFragment, null, options);
        });
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(new BudgetAdapter.OnBudgetActionListener() {
            @Override
            public void onBudgetClick(BudgetEntity budget) {
                navigateToEditBudget(budget);
            }

            @Override
            public void onDeleteBudget(BudgetEntity budget, String displayName) {
                showDeleteConfirmationDialog(budget, displayName);
            }
        });
        recyclerBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBudgets.setAdapter(adapter);
    }

    private void navigateToAddBudget() {
        Navigation.findNavController(requireView()).navigate(R.id.action_budgetGoalFragment_to_addEditBudgetFragment);
    }

    private void navigateToEditBudget(BudgetEntity budget) {
        Bundle args = new Bundle();
        args.putString(AddEditBudgetViewModel.ARG_BUDGET_ID, budget.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_budgetGoalFragment_to_addEditBudgetFragment, args);
    }

    private void showDeleteConfirmationDialog(BudgetEntity budget, String displayName) {
        YearMonth period = YearMonth.parse(budget.period);
        String message = "Ngân sách kỳ Tháng " + period.getMonthValue() + " / " + period.getYear()
                + " với hạn mức " + formatCurrency(budget.limitAmount)
                + " sẽ bị xoá. Các giao dịch liên quan không bị ảnh hưởng. Hành động này không thể hoàn tác.";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá ngân sách \"" + displayName + "\"?")
                .setMessage(message)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (d, which) -> viewModel.deleteBudget(budget))
                .show();
    }

    private void renderDeleteState(UiState<Void> state) {
        if (state instanceof UiState.Success) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Đã xoá ngân sách", Snackbar.LENGTH_SHORT).show();
            viewModel.onDeleteStateHandled();
        } else if (state instanceof UiState.Error) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    ((UiState.Error<Void>) state).message, Snackbar.LENGTH_LONG).show();
            viewModel.onDeleteStateHandled();
        }
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }

    private void renderState(UiState<List<BudgetEntity>> state) {
        layoutStateLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        viewEmptyState.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        boolean hasData = state instanceof UiState.Success;
        recyclerBudgets.setVisibility(hasData ? View.VISIBLE : View.GONE);
        textBudgetPeriod.setVisibility(hasData ? View.VISIBLE : View.GONE);
        if (hasData) {
            adapter.submitList(((UiState.Success<List<BudgetEntity>>) state).data);
        }
    }
}