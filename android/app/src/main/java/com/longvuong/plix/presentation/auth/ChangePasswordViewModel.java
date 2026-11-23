package com.longvuong.plix.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

//Máy trạng thái của màn Đổi mật khẩu (AUTH-15, AUTH-16): kiểm tra trên máy, rồi xác minh mật khẩu hiện tại, thành công mới đổi sang mật khẩu mới.
//Các mật khẩu chỉ nằm trong tham số và callback đang chạy, không lưu vào trường nào của lớp và không ghi log
@HiltViewModel
public class ChangePasswordViewModel extends ViewModel {
    private static final String CURRENT_PASSWORD_EMPTY_MESSAGE = "Vui lòng nhập mật khẩu hiện tại";
    private static final String SAME_PASSWORD_MESSAGE = "Mật khẩu mới phải khác mật khẩu hiện tại";

    private final AuthManager authManager;
    private final FormValidator formValidator;

    //null = chưa đổi lần nào; Loading = đang xác minh hoặc đang đổi; Success = đã đổi mật khẩu (vẫn đăng nhập); Error = thông báo lỗi tiếng Việt
    private final MutableLiveData<UiState<Void>> changeState = new MutableLiveData<>();

    @Inject
    public ChangePasswordViewModel(AuthManager authManager, FormValidator formValidator) {
        this.authManager = authManager;
        this.formValidator = formValidator;
    }

    public LiveData<UiState<Void>> getChangeState() {
        return changeState;
    }

    //Kiểm tra trên máy theo thứ tự: mật khẩu hiện tại rỗng, mật khẩu mới rỗng hoặc nhập lại không khớp, mật khẩu mới trùng mật khẩu hiện tại (sai thì không gọi mạng);
    //hợp lệ thì xác minh mật khẩu hiện tại rồi mới đổi mật khẩu
    public void changePassword(String current, String newPassword, String confirm) {
        if (current == null || current.isEmpty()) {
            changeState.setValue(new UiState.Error<>(CURRENT_PASSWORD_EMPTY_MESSAGE));
            return;
        }
        Result<Void> passwordValidation = formValidator.validateNewPassword(newPassword, confirm);
        if (passwordValidation instanceof Result.Error) {
            changeState.setValue(new UiState.Error<>(((Result.Error<Void>) passwordValidation).message));
            return;
        }
        if (newPassword.equals(current)) {
            changeState.setValue(new UiState.Error<>(SAME_PASSWORD_MESSAGE));
            return;
        }
        changeState.setValue(new UiState.Loading<>());
        authManager.verifyCurrentPassword(current, new AuthManager.ActionCallback() {
            @Override
            public void onSuccess() {
                //Mật khẩu hiện tại đúng: đổi sang mật khẩu mới (AuthManager dùng token của lần xác minh, không đổi phiên đăng nhập)
                authManager.changePassword(newPassword, new AuthManager.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        changeState.postValue(new UiState.Success<>(null));
                    }

                    @Override
                    public void onError(String message) {
                        changeState.postValue(new UiState.Error<>(message));
                    }
                });
            }

            @Override
            public void onError(String message) {
                changeState.postValue(new UiState.Error<>(message)); //không gọi changePassword
            }
        });
    }
}