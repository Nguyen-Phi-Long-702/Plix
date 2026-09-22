package com.longvuong.plix.presentation.goal;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.repository.GoalRepository;
import com.longvuong.plix.domain.usecase.goal.CalculateGoalRequiredMonthlyUseCase;
import com.longvuong.plix.domain.usecase.goal.GoalProgress;
import com.longvuong.plix.domain.usecase.goal.GoalStatusUseCase;
import com.longvuong.plix.domain.usecase.goal.GoalUseCase;
import com.longvuong.plix.presentation.common.UiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class GoalListViewModel extends ViewModel {
    private final GoalUseCase goalUseCase;
    private final AuthManager authManager;
    private final MediatorLiveData<UiState<List<GoalEntity>>> goalListState = new MediatorLiveData<>();
    private final MutableLiveData<Map<String, GoalProgress>> progressByGoalId = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<UiState<Void>> deleteState = new MutableLiveData<>();

    @Inject
    public GoalListViewModel(GoalRepository goalRepository, GoalUseCase goalUseCase, AuthManager authManager, GoalStatusUseCase goalStatusUseCase, CalculateGoalRequiredMonthlyUseCase calculateGoalRequiredMonthlyUseCase) {
        this.goalUseCase = goalUseCase;
        this.authManager = authManager;

        goalListState.setValue(new UiState.Loading<>());
        goalListState.addSource(goalRepository.getActiveGoals(), goals -> {
            List<GoalEntity> safeGoals = goals != null ? goals : new ArrayList<>();
            Map<String, GoalProgress> progressMap = new HashMap<>();
            for (GoalEntity goal : safeGoals) {
                int percent = goal.targetAmount <= 0 ? 0 : (int) Math.round(goal.currentAmount * 100.0 / goal.targetAmount);
                progressMap.put(goal.id, new GoalProgress(percent, goalStatusUseCase.execute(goal), calculateGoalRequiredMonthlyUseCase.execute(goal)));
            }
            progressByGoalId.setValue(progressMap);
            goalListState.setValue(safeGoals.isEmpty() ? new UiState.Empty<>() : new UiState.Success<>(safeGoals));
        });
    }

    public LiveData<UiState<List<GoalEntity>>> getGoalListState() {
        return goalListState;
    }

    public LiveData<Map<String, GoalProgress>> getProgressByGoalId() {
        return progressByGoalId;
    }

    public LiveData<UiState<Void>> getDeleteState() {
        return deleteState;
    }

    public void deleteGoal(GoalEntity goal) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            deleteState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }
        goalUseCase.deleteGoal(goal, userId, result -> deleteState.setValue(toUiState(result)));
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}