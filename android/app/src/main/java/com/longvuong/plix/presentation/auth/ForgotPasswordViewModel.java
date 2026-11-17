package com.longvuong.plix.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

//Máy trạng thái của màn Quên mật khẩu (AUTH-14, AUTH-16): bước 1 nhập email để nhận mã, bước 2 nhập mã và mật khẩu mới.
//Chỉ email, bước hiện tại và thời điểm gửi mã được lưu trong SavedStateHandle; mã, mật khẩu và token không bao giờ được lưu hay ghi log
@HiltViewModel
public class ForgotPasswordViewModel extends ViewModel {
    public enum Step { ENTER_EMAIL, ENTER_CODE }

    public static final long RESEND_COOLDOWN_MILLIS = 60_000L; //khoá gửi lại mã 60 giây theo giới hạn /recover của Supabase

    static final String KEY_EMAIL = "email";
    static final String KEY_STEP = "step";
    static final String KEY_LAST_SENT_AT = "lastSentAt";

    private final SavedStateHandle savedStateHandle;
    private final AuthManager authManager;
    private final FormValidator formValidator;

    private final MutableLiveData<Step> step = new MutableLiveData<>();
    //null = chưa gửi lần nào; Loading = đang gửi mã; Success = đã gửi mã; Error = thông báo lỗi tiếng Việt
    private final MutableLiveData<UiState<Void>> requestCodeState = new MutableLiveData<>();
    //null = chưa đặt lại lần nào; Loading = đang đặt lại; Success = đã đặt lại mật khẩu (chưa đăng nhập); Error = thông báo lỗi tiếng Việt
    private final MutableLiveData<UiState<Void>> resetState = new MutableLiveData<>();

    @Inject
    public ForgotPasswordViewModel(SavedStateHandle savedStateHandle, AuthManager authManager, FormValidator formValidator) {
        this.savedStateHandle = savedStateHandle;
        this.authManager = authManager;
        this.formValidator = formValidator;
        //Khôi phục bước sau khi tiến trình bị huỷ (process death); chưa có gì thì bắt đầu ở bước nhập email
        String savedStep = savedStateHandle.get(KEY_STEP);
        step.setValue(savedStep != null ? Step.valueOf(savedStep) : Step.ENTER_EMAIL);
    }

    public LiveData<Step> getStep() {
        return step;
    }

    public LiveData<UiState<Void>> getRequestCodeState() {
        return requestCodeState;
    }

    public LiveData<UiState<Void>> getResetState() {
        return resetState;
    }

    //Email đã dùng để gửi mã (rỗng khi chưa gửi)
    public String getEmail() {
        String email = savedStateHandle.get(KEY_EMAIL);
        return email != null ? email : "";
    }

    //Được gửi lại mã khi chưa từng gửi hoặc đã qua đủ RESEND_COOLDOWN_MILLIS kể từ lần gửi gần nhất
    public boolean canResend(long nowMillis) {
        return getResendRemainingMillis(nowMillis) == 0L;
    }

    //Số mili giây còn phải chờ để gửi lại mã (0 nếu được gửi ngay); Fragment đếm ngược dựa trên giá trị này
    public long getResendRemainingMillis(long nowMillis) {
        Long lastSentAt = savedStateHandle.get(KEY_LAST_SENT_AT);
        if (lastSentAt == null) {
            return 0L;
        }
        long remaining = RESEND_COOLDOWN_MILLIS - (nowMillis - lastSentAt);
        return Math.max(0L, remaining);
    }

    //Bước 1 (cũng dùng để gửi lại mã): kiểm tra email rồi nhờ Supabase gửi mã 6 chữ số
    public void requestCode(String email) {
        Result<Void> validation = formValidator.validateRecoveryEmail(email);
        if (validation instanceof Result.Error) {
            requestCodeState.setValue(new UiState.Error<>(((Result.Error<Void>) validation).message));
            return;
        }
        String trimmedEmail = email.trim();
        requestCodeState.setValue(new UiState.Loading<>());
        authManager.requestPasswordRecovery(trimmedEmail, new AuthManager.ActionCallback() {
            @Override
            public void onSuccess() {
                //Supabase trả thành công kể cả khi email chưa đăng ký nên luôn sang bước 2 (không lộ tài khoản)
                savedStateHandle.set(KEY_EMAIL, trimmedEmail);
                savedStateHandle.set(KEY_STEP, Step.ENTER_CODE.name());
                savedStateHandle.set(KEY_LAST_SENT_AT, System.currentTimeMillis());
                step.postValue(Step.ENTER_CODE);
                requestCodeState.postValue(new UiState.Success<>(null));
            }

            @Override
            public void onError(String message) {
                requestCodeState.postValue(new UiState.Error<>(message)); //giữ nguyên bước hiện tại
            }
        });
    }

    //Bước 2: kiểm tra mã và mật khẩu mới trên máy rồi đặt lại mật khẩu; thành công vẫn chưa đăng nhập (người dùng quay về màn Đăng nhập)
    public void resetPassword(String code, String newPassword, String confirm) {
        Result<Void> codeValidation = formValidator.validateRecoveryCode(code);
        if (codeValidation instanceof Result.Error) {
            resetState.setValue(new UiState.Error<>(((Result.Error<Void>) codeValidation).message));
            return;
        }
        Result<Void> passwordValidation = formValidator.validateNewPassword(newPassword, confirm);
        if (passwordValidation instanceof Result.Error) {
            resetState.setValue(new UiState.Error<>(((Result.Error<Void>) passwordValidation).message));
            return;
        }
        resetState.setValue(new UiState.Loading<>());
        authManager.resetPasswordWithCode(getEmail(), code.trim(), newPassword, new AuthManager.ActionCallback() {
            @Override
            public void onSuccess() {
                resetState.postValue(new UiState.Success<>(null));
            }

            @Override
            public void onError(String message) {
                resetState.postValue(new UiState.Error<>(message));
            }
        });
    }
}