package com.longvuong.plix.presentation.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.budget.BudgetProgress;
import com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase;
import com.longvuong.plix.domain.usecase.budget.DeleteBudgetUseCase;
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
    private final DeleteBudgetUseCase deleteBudgetUseCase;
    private final AuthManager authManager;

    private final String currentPeriod = YearMonth.now().toString();

    private final MediatorLiveData<UiState<List<BudgetEntity>>> budgetListState = new MediatorLiveData<>();
    private final MutableLiveData<Map<String, BudgetProgress>> progressByBudgetId = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<UiState<Void>> deleteState = new MutableLiveData<>();

    private List<BudgetEntity> latestBudgets = new ArrayList<>();
    private List<TransactionEntity> latestTransactions = new ArrayList<>();

    @Inject
    public BudgetListViewModel(BudgetRepository budgetRepository,
                               TransactionRepository transactionRepository,
                               CategoryRepository categoryRepository,
                               CalculateBudgetProgressUseCase calculateBudgetProgressUseCase,
                               DeleteBudgetUseCase deleteBudgetUseCase,
                               AuthManager authManager) {
        this.categoryRepository = categoryRepository;
        this.calculateBudgetProgressUseCase = calculateBudgetProgressUseCase;
        this.deleteBudgetUseCase = deleteBudgetUseCase;
        this.authManager = authManager;

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

    public LiveData<UiState<Void>> getDeleteState() {
        return deleteState;
    }

    public void deleteBudget(BudgetEntity budget) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            deleteState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }
        deleteBudgetUseCase.execute(budget, userId, result -> deleteState.setValue(toUiState(result)));
    }

    //Fragment gọi sau khi đã hiện thông báo, tránh hiện lại khi quay về màn hình này
    public void onDeleteStateHandled() {
        deleteState.setValue(null);
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}