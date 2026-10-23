package com.longvuong.plix.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.remote.dto.AuthResponseDto;
import com.longvuong.plix.data.repository.SyncRepository;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AuthViewModel extends ViewModel {
    private static final String PASSWORD_MISMATCH_MESSAGE = "Mật khẩu nhập lại không khớp";

    private final AuthManager authManager;
    private final SyncRepository syncRepository;
    private final MutableLiveData<UiState<Void>> authState = new MutableLiveData<>();

    @Inject
    public AuthViewModel(AuthManager authManager, SyncRepository syncRepository) {
        this.authManager = authManager;
        this.syncRepository = syncRepository;
    }

    public LiveData<UiState<Void>> getAuthState() {
        return authState;
    }

    public void login(String email, String password) {
        authState.setValue(new UiState.Loading<>());
        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDto response) {
                syncRepository.requestSync(); //đăng nhập xong kéo dữ liệu của tài khoản về (máy mới/cài lại app)
                authState.postValue(new UiState.Success<>(null));
            }

            @Override
            public void onError(String message) {
                authState.postValue(new UiState.Error<>(message));
            }
        });
    }

    public void register(String email, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            authState.setValue(new UiState.Error<>(PASSWORD_MISMATCH_MESSAGE));
            return;
        }
        authState.setValue(new UiState.Loading<>());
        authManager.register(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDto response) {
                authState.postValue(new UiState.Success<>(null));
            }

            @Override
            public void onError(String message) {
                authState.postValue(new UiState.Error<>(message));
            }
        });
    }
}