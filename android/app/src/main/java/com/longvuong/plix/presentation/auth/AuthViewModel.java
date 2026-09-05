package com.longvuong.plix.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.remote.dto.AuthResponseDto;

public class AuthViewModel extends ViewModel {
    private final AuthManager authManager = new AuthManager();

    private final MutableLiveData<String> authSuccessToken = new MutableLiveData<>();
    private final MutableLiveData<String> authError = new MutableLiveData<>();
    private final MutableLiveData<String> jwtPocResult = new MutableLiveData<>();

    public LiveData<String> getAuthSuccessToken() {
        return authSuccessToken;
    }

    public LiveData<String> getAuthError() {
        return authError;
    }

    public LiveData<String> getJwtPocResult() {
        return jwtPocResult;
    }

    public void login(String email, String password) {
        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDto response) {
                authSuccessToken.postValue(response.accessToken);
            }

            @Override
            public void onError(String message) {
                authError.postValue(message);
            }
        });
    }

    public void register(String email, String password) {
        authManager.register(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponseDto response) {
                authSuccessToken.postValue(response.accessToken);
            }

            @Override
            public void onError(String message) {
                authError.postValue(message);
            }
        });
    }

    public void testJwtPoc() {
        jwtPocResult.postValue("Đang kiểm tra JWT...");
        authManager.testBackendAuth(jwtPocResult::postValue);
    }
}