package com.longvuong.plix.presentation.budget;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.domain.usecase.budget.AddBudgetUseCase;
import com.longvuong.plix.domain.usecase.budget.UpdateBudgetUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditBudgetViewModel extends ViewModel {
    public static final String ARG_BUDGET_ID = "budgetId";
    private final BudgetRepository budgetRepository;
    private final AddBudgetUseCase addBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final FormValidator formValidator;
    private final AuthManager authManager;
    private boolean initialized = false;
    private String editingBudgetId;
    private BudgetEntity loadedEntity;
    private String period = YearMonth.now().toString();
    private boolean overall = true;
    private String categoryId;
    private long limitAmount = 0L;
    private int thresholdPercent = 80;
    private final MutableLiveData<Boolean> formReady = new MutableLiveData<>(false);
    private final MutableLiveData<UiState<Void>> saveState = new MutableLiveData<>();
    private final MediatorLiveData<List<CategoryEntity>> expenseCategories = new MediatorLiveData<>();

    @Inject
    public AddEditBudgetViewModel(BudgetRepository budgetRepository, CategoryRepository categoryRepository, AddBudgetUseCase addBudgetUseCase, UpdateBudgetUseCase updateBudgetUseCase, FormValidator formValidator, AuthManager authManager) {
        this.budgetRepository = budgetRepository;
        this.addBudgetUseCase = addBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.formValidator = formValidator;
        this.authManager = authManager;
        expenseCategories.addSource(categoryRepository.getActiveCategories(), categories -> {
            List<CategoryEntity> filtered = new ArrayList<>();
            if (categories != null) {
                for (CategoryEntity category : categories) {
                    if ("expense".equals(category.type)) {
                        filtered.add(category);
                    }
                }
            }
            expenseCategories.setValue(filtered);
        });
    }

    public void init(@Nullable String budgetId) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.editingBudgetId = budgetId;
        if (budgetId == null) {
            formReady.setValue(true);
        } else {
            loadExistingBudget(budgetId);
        }
    }

    private void loadExistingBudget(String id) {
        budgetRepository.getById(id, result -> {
            if (result instanceof Result.Success) {
                BudgetEntity entity = ((Result.Success<BudgetEntity>) result).data;
                if (entity == null) {
                    saveState.setValue(new UiState.Error<>("Không tìm thấy ngân sách cần sửa"));
                    return;
                }
                loadedEntity = entity;
                period = entity.period;
                overall = entity.categoryId == null;
                categoryId = entity.categoryId;
                limitAmount = entity.limitAmount;
                thresholdPercent = entity.thresholdPercent;
                formReady.setValue(true);
            } else {
                Result.Error<BudgetEntity> error = (Result.Error<BudgetEntity>) result;
                saveState.setValue(new UiState.Error<>(error.message));
            }
        });
    }

    public boolean isEditMode() {
        return editingBudgetId != null;
    }

    public LiveData<Boolean> getFormReady() {
        return formReady;
    }

    public LiveData<List<CategoryEntity>> getExpenseCategories() {
        return expenseCategories;
    }

    public LiveData<UiState<Void>> getSaveState() {
        return saveState;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public boolean isOverall() {
        return overall;
    }

    public void setOverall(boolean overall) {
        this.overall = overall;
        if (overall) {
            setCategoryId(null);
        }
    }

    @Nullable
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@Nullable String categoryId) {
        this.categoryId = categoryId;
    }

    public long getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(long limitAmount) {
        this.limitAmount = limitAmount;
    }

    public int getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(int thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public Result<Void> validatePeriodField(String period) {
        return formValidator.validatePeriod(period);
    }

    public Result<Void> validateLimitAmountField(long limitAmount) {
        return formValidator.validateAmount(limitAmount);
    }

    public void save() {
        String userId = authManager.getCurrentUserId();
        if (userId == null && !isEditMode()) {
            saveState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }

        if (isEditMode() && loadedEntity != null) {
            loadedEntity.limitAmount = limitAmount;
            loadedEntity.thresholdPercent = thresholdPercent;
            saveState.setValue(new UiState.Loading<>());
            updateBudgetUseCase.execute(loadedEntity, result -> saveState.setValue(toUiState(result)));
            return;
        }
        BudgetEntity entity = new BudgetEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.period = period;
        entity.categoryId = overall ? null : categoryId;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = thresholdPercent;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        saveState.setValue(new UiState.Loading<>());
        addBudgetUseCase.execute(entity, result -> saveState.setValue(toUiState(result)));
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}