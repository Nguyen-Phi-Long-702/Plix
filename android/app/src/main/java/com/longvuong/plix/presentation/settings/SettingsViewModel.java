package com.longvuong.plix.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.domain.usecase.auth.LogoutOutcome;
import com.longvuong.plix.domain.usecase.auth.LogoutUseCase;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {
    private static final int NEW_CORRECTIONS_BADGE_THRESHOLD = 10;
    private static final String LOGOUT_ERROR_MESSAGE = "Không thể đăng xuất, vui lòng thử lại";

    private final AiRepository aiRepository;
    private final LogoutUseCase logoutUseCase;
    private final AppExecutors appExecutors;
    private final MutableLiveData<UiState<Void>> retrainState = new MutableLiveData<>();
    //Empty = chưa làm gì, Loading = đang kiểm tra/đồng bộ lần cuối/xoá, Success = đã đăng xuất hoặc cần xác nhận, Error = lỗi bất ngờ
    private final MutableLiveData<UiState<LogoutOutcome>> logoutState = new MutableLiveData<>(new UiState.Empty<>());

    @Inject
    public SettingsViewModel(AiRepository aiRepository, LogoutUseCase logoutUseCase, AppExecutors appExecutors) {
        this.aiRepository = aiRepository;
        this.logoutUseCase = logoutUseCase;
        this.appExecutors = appExecutors;
    }

    public LiveData<UiState<Void>> getRetrainState() {
        return retrainState;
    }

    public LiveData<UiState<LogoutOutcome>> getLogoutState() {
        return logoutState;
    }

    public void retrain() {
        retrainState.setValue(new UiState.Loading<>());
        aiRepository.retrain(result -> retrainState.setValue(toUiState(result)));
    }

    public boolean shouldShowNewCorrectionsBadge() {
        return aiRepository.getPendingCorrectionCount() >= NEW_CORRECTIONS_BADGE_THRESHOLD;
    }

    //Người dùng nhấn "Đăng xuất": kiểm tra pending -> huỷ/chờ SyncWorker -> đồng bộ lần cuối (tối đa 10 giây)
    public void logout() {
        if (logoutState.getValue() instanceof UiState.Loading) {
            return; //đang xử lý, bỏ qua lần nhấn thứ hai
        }
        logoutState.setValue(new UiState.Loading<>());
        appExecutors.networkIO().execute(() -> {
            try {
                logoutState.postValue(new UiState.Success<>(logoutUseCase.start()));
            } catch (Exception e) {
                logoutState.postValue(new UiState.Error<>(LOGOUT_ERROR_MESSAGE));
            }
        });
    }

    //Người dùng chọn "Vẫn đăng xuất (mất thay đổi chưa đồng bộ)"
    public void confirmLogoutAnyway() {
        logoutState.setValue(new UiState.Loading<>());
        appExecutors.networkIO().execute(() -> {
            try {
                logoutUseCase.confirmLogoutAnyway();
                logoutState.postValue(new UiState.Success<>(LogoutOutcome.loggedOut()));
            } catch (Exception e) {
                logoutState.postValue(new UiState.Error<>(LOGOUT_ERROR_MESSAGE));
            }
        });
    }

    //Người dùng chọn "Huỷ, để tôi thử lại sau": không đăng xuất, không xoá gì
    public void cancelLogout() {
        logoutState.setValue(new UiState.Empty<>());
        appExecutors.networkIO().execute(logoutUseCase::cancelLogout);
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}