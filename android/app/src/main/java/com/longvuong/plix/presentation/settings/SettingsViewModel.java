package com.longvuong.plix.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {
    private static final int NEW_CORRECTIONS_BADGE_THRESHOLD = 10;

    private final AiRepository aiRepository;
    private final MutableLiveData<UiState<Void>> retrainState = new MutableLiveData<>();

    @Inject
    public SettingsViewModel(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
    }

    public LiveData<UiState<Void>> getRetrainState() {
        return retrainState;
    }

    public void retrain() {
        retrainState.setValue(new UiState.Loading<>());
        aiRepository.retrain(result -> retrainState.setValue(toUiState(result)));
    }

    public boolean shouldShowNewCorrectionsBadge() {
        return aiRepository.getPendingCorrectionCount() >= NEW_CORRECTIONS_BADGE_THRESHOLD;
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}