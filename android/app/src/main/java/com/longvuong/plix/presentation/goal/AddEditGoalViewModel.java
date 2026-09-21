package com.longvuong.plix.presentation.goal;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.repository.GoalRepository;
import com.longvuong.plix.domain.usecase.goal.CalculateGoalRequiredMonthlyUseCase;
import com.longvuong.plix.domain.usecase.goal.GoalRequiredMonthly;
import com.longvuong.plix.domain.usecase.goal.GoalStatus;
import com.longvuong.plix.domain.usecase.goal.GoalStatusUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditGoalViewModel extends ViewModel {
    public static final String ARG_GOAL_ID = "goalId";
    private final GoalRepository goalRepository;
    private final FormValidator formValidator;
    private final AuthManager authManager;
    private final CalculateGoalRequiredMonthlyUseCase calculateGoalRequiredMonthlyUseCase;
    private final GoalStatusUseCase goalStatusUseCase;

    private boolean initialized = false;
    private String editingGoalId;
    private GoalEntity loadedEntity;

    private String name = "";
    private long targetAmount = 0L;
    private long currentAmount = 0L;
    private long deadline = LocalDate.now(ZoneId.systemDefault()).plusYears(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    private final MutableLiveData<Boolean> formReady = new MutableLiveData<>(false);
    private final MutableLiveData<UiState<Void>> saveState = new MutableLiveData<>();

    @Inject
    public AddEditGoalViewModel(GoalRepository goalRepository, FormValidator formValidator, AuthManager authManager, CalculateGoalRequiredMonthlyUseCase calculateGoalRequiredMonthlyUseCase, GoalStatusUseCase goalStatusUseCase) {
        this.goalRepository = goalRepository;
        this.formValidator = formValidator;
        this.authManager = authManager;
        this.calculateGoalRequiredMonthlyUseCase = calculateGoalRequiredMonthlyUseCase;
        this.goalStatusUseCase = goalStatusUseCase;
    }

    public void init(@Nullable String goalId) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.editingGoalId = goalId;
        if (goalId == null) {
            formReady.setValue(true);
        } else {
            loadExistingGoal(goalId);
        }
    }

    private void loadExistingGoal(String id) {
        goalRepository.getById(id, result -> {
            if (result instanceof Result.Success) {
                GoalEntity entity = ((Result.Success<GoalEntity>) result).data;
                if (entity == null) {
                    saveState.setValue(new UiState.Error<>("Không tìm thấy mục tiêu cần sửa"));
                    return;
                }
                loadedEntity = entity;
                name = entity.name;
                targetAmount = entity.targetAmount;
                currentAmount = entity.currentAmount;
                deadline = entity.deadline;
                formReady.setValue(true);
            } else {
                Result.Error<GoalEntity> error = (Result.Error<GoalEntity>) result;
                saveState.setValue(new UiState.Error<>(error.message));
            }
        });
    }

    public boolean isEditMode() {
        return editingGoalId != null;
    }

    public LiveData<Boolean> getFormReady() {
        return formReady;
    }

    public LiveData<UiState<Void>> getSaveState() {
        return saveState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(long targetAmount) {
        this.targetAmount = targetAmount;
    }

    public long getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(long currentAmount) {
        this.currentAmount = currentAmount;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public Result<Void> validateNameField(String name) {
        return formValidator.validateGoalName(name);
    }

    public Result<Void> validateTargetAmountField(long targetAmount) {
        return formValidator.validateAmount(targetAmount);
    }

    public Result<Void> validateCurrentAmountField(long currentAmount) {
        return formValidator.validateGoalCurrentAmount(currentAmount);
    }

    public Result<Void> validateDeadlineField(long deadline) {
        return formValidator.validateGoalDeadline(deadline);
    }

    public GoalRequiredMonthly previewRequiredMonthly() {
        return calculateGoalRequiredMonthlyUseCase.execute(buildPreviewEntity());
    }

    public GoalStatus previewStatus() {
        return goalStatusUseCase.execute(buildPreviewEntity());
    }

    private GoalEntity buildPreviewEntity() {
        GoalEntity preview = new GoalEntity();
        preview.targetAmount = targetAmount;
        preview.currentAmount = currentAmount;
        preview.deadline = deadline;
        return preview;
    }

    public void save() {
        String userId = authManager.getCurrentUserId();
        if (userId == null && !isEditMode()) {
            saveState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }

        if (isEditMode() && loadedEntity != null) {
            Result<Void> currentValidation = formValidator.validateGoalCurrentAmount(currentAmount);
            if (currentValidation instanceof Result.Error) {
                saveState.setValue(new UiState.Error<>(((Result.Error<Void>) currentValidation).message));
                return;
            }
            loadedEntity.currentAmount = currentAmount;
            loadedEntity.updatedAt = System.currentTimeMillis();
            loadedEntity.syncStatus = "pending";
            saveState.setValue(new UiState.Loading<>());
            goalRepository.update(loadedEntity, result -> saveState.setValue(toUiState(result)));
            return;
        }

        Result<Void> nameValidation = formValidator.validateGoalName(name);
        if (nameValidation instanceof Result.Error) {
            saveState.setValue(new UiState.Error<>(((Result.Error<Void>) nameValidation).message));
            return;
        }
        Result<Void> targetValidation = formValidator.validateAmount(targetAmount);
        if (targetValidation instanceof Result.Error) {
            saveState.setValue(new UiState.Error<>(((Result.Error<Void>) targetValidation).message));
            return;
        }
        Result<Void> deadlineValidation = formValidator.validateGoalDeadline(deadline);
        if (deadlineValidation instanceof Result.Error) {
            saveState.setValue(new UiState.Error<>(((Result.Error<Void>) deadlineValidation).message));
            return;
        }

        GoalEntity entity = new GoalEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.name = name != null ? name.trim() : "";
        entity.targetAmount = targetAmount;
        entity.currentAmount = 0L;
        entity.deadline = deadline;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        saveState.setValue(new UiState.Loading<>());
        goalRepository.insert(entity, result -> saveState.setValue(toUiState(result)));
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}