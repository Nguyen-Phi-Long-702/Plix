package com.longvuong.plix.presentation.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.longvuong.plix.R;
import com.longvuong.plix.presentation.common.EmptyStateView;
import com.longvuong.plix.presentation.common.ExpandableFabView;
import com.longvuong.plix.presentation.common.UiState;

import java.text.NumberFormat;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        ExpandableFabView expandableFab = view.findViewById(R.id.expandableFab);
        expandableFab.setOnAddTransactionClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_homeFragment_to_addEditTransactionFragment));
        expandableFab.setOnQuickAddClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_homeFragment_to_quickAddNlFragment));
        viewModel.getQuickAddVisible().observe(getViewLifecycleOwner(), visible ->
                expandableFab.setQuickAddVisible(visible));

        EmptyStateView emptyHome = view.findViewById(R.id.emptyHome);
        emptyHome.setOnActionClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_homeFragment_to_addEditTransactionFragment));

        view.findViewById(R.id.textViewAllTransactions).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_homeFragment_to_transactionListFragment));

        RecentTransactionAdapter recentAdapter = new RecentTransactionAdapter();
        RecyclerView recyclerRecent = view.findViewById(R.id.recyclerRecentTransactions);
        recyclerRecent.setAdapter(recentAdapter);

        View layoutLoading = view.findViewById(R.id.layoutHomeLoading);
        View scrollHome = view.findViewById(R.id.scrollHome);
        viewModel.getHomeState().observe(getViewLifecycleOwner(), state -> {
            layoutLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            emptyHome.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
            scrollHome.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Success) {
                bindHome(view, ((UiState.Success<HomeUiModel>) state).data, recentAdapter);
            }
        });
    }

    private void bindHome(View view, HomeUiModel model, RecentTransactionAdapter recentAdapter) {
        setText(view, R.id.textMonthLabel, "Tổng quan " + model.monthLabel);
        setText(view, R.id.textNetBalance, formatCurrency(model.netBalance));
        setText(view, R.id.textIncome, formatCurrency(model.totalIncome));
        setText(view, R.id.textExpense, formatCurrency(model.totalExpense));

        recentAdapter.submit(model.recentTransactions, model.categoryNamesById);

        HomeUiModel.BudgetSummary budget = model.budget;
        view.findViewById(R.id.layoutHomeBudget).setVisibility(budget != null ? View.VISIBLE : View.GONE);
        if (budget == null) {
            return;
        }
        setText(view, R.id.textBudgetName, budget.name);
        setText(view, R.id.textBudgetPercent, budget.percent + "%");
        setText(view, R.id.textBudgetSpent, "Đã chi " + formatCurrency(budget.spentAmount));
        setText(view, R.id.textBudgetLimit, "Hạn mức " + formatCurrency(budget.limitAmount));
        LinearProgressIndicator progressBudget = view.findViewById(R.id.progressBudget);
        progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), budgetColorRes(budget.level)));
        progressBudget.setProgressCompat(Math.min(budget.percent, 100), false);
    }

    private void setText(View root, int viewId, String text) {
        ((TextView) root.findViewById(viewId)).setText(text);
    }

    private int budgetColorRes(HomeUiModel.BudgetLevel level) {
        switch (level) {
            case DANGER:
                return R.color.budget_progress_danger;
            case WARNING:
                return R.color.budget_progress_warning;
            default:
                return R.color.budget_progress_normal;
        }
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }
}