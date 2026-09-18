package com.longvuong.plix.presentation.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BudgetListFragment extends Fragment {
    private BudgetListViewModel viewModel;
    private BudgetAdapter adapter;
    private RecyclerView recyclerBudgets;
    private ProgressBar progressLoading;
    private TextView textEmptyState;
    private FloatingActionButton fabAddBudget;

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
        setupRecyclerView();
        fabAddBudget.setOnClickListener(v -> navigateToAddBudget());
        viewModel.getBudgetListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getProgressByBudgetId().observe(getViewLifecycleOwner(), adapter::submitProgress);
        viewModel.getActiveCategories().observe(getViewLifecycleOwner(), adapter::submitCategories);
    }

    private void bindViews(View view) {
        recyclerBudgets = view.findViewById(R.id.recyclerBudgets);
        progressLoading = view.findViewById(R.id.progressLoading);
        textEmptyState = view.findViewById(R.id.textEmptyState);
        fabAddBudget = view.findViewById(R.id.fabAddBudget);
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(this::navigateToEditBudget);
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

    private void renderState(UiState<List<BudgetEntity>> state) {
        progressLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textEmptyState.setVisibility((state instanceof UiState.Empty || state instanceof UiState.Error) ? View.VISIBLE : View.GONE);
        recyclerBudgets.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);
        if (state instanceof UiState.Success) {
            adapter.submitList(((UiState.Success<List<BudgetEntity>>) state).data);
        } else if (state instanceof UiState.Error) {
            textEmptyState.setText(((UiState.Error<List<BudgetEntity>>) state).message);
        } else if (state instanceof UiState.Empty) {
            textEmptyState.setText("Chưa có ngân sách — tạo ngân sách đầu tiên");
        }
    }
}