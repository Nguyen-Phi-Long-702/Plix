package com.longvuong.plix.presentation.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.budget.BudgetProgress;
import com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase;
import com.longvuong.plix.presentation.common.UiState;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class BudgetListViewModel extends ViewModel {
    private final CalculateBudgetProgressUseCase calculateBudgetProgressUseCase;
    private final CategoryRepository categoryRepository;

    private final String currentPeriod = YearMonth.now().toString();

    private final MediatorLiveData<UiState<List<BudgetEntity>>> budgetListState = new MediatorLiveData<>();
    private final MutableLiveData<Map<String, BudgetProgress>> progressByBudgetId = new MutableLiveData<>(new HashMap<>());

    private List<BudgetEntity> latestBudgets = new ArrayList<>();
    private List<TransactionEntity> latestTransactions = new ArrayList<>();

    @Inject
    public BudgetListViewModel(BudgetRepository budgetRepository,
                               TransactionRepository transactionRepository,
                               CategoryRepository categoryRepository,
                               CalculateBudgetProgressUseCase calculateBudgetProgressUseCase) {
        this.categoryRepository = categoryRepository;
        this.calculateBudgetProgressUseCase = calculateBudgetProgressUseCase;

        budgetListState.setValue(new UiState.Loading<>());
        budgetListState.addSource(budgetRepository.getActiveByPeriod(currentPeriod), budgets -> {
            latestBudgets = budgets != null ? budgets : new ArrayList<>();
            recomputeAndPublish();
        });
        budgetListState.addSource(transactionRepository.getAll(), transactions -> {
            latestTransactions = transactions != null ? transactions : new ArrayList<>();
            recomputeAndPublish();
        });
    }

    private void recomputeAndPublish() {
        Map<String, BudgetProgress> progressMap = new HashMap<>();
        long periodStart = CalculateBudgetProgressUseCase.periodStartAtMillis(currentPeriod);
        long periodEnd = CalculateBudgetProgressUseCase.periodEndAtMillis(currentPeriod);
        for (BudgetEntity budget : latestBudgets) {
            progressMap.put(budget.id,
                    calculateBudgetProgressUseCase.execute(budget, latestTransactions, periodStart, periodEnd));
        }
        progressByBudgetId.setValue(progressMap);
        budgetListState.setValue(latestBudgets.isEmpty()
                ? new UiState.Empty<>() : new UiState.Success<>(latestBudgets));
    }

    public LiveData<UiState<List<BudgetEntity>>> getBudgetListState() {
        return budgetListState;
    }

    public LiveData<Map<String, BudgetProgress>> getProgressByBudgetId() {
        return progressByBudgetId;
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categoryRepository.getActiveCategories();
    }
}