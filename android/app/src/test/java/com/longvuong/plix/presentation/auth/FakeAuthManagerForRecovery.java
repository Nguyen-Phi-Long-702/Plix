package com.longvuong.plix.presentation.auth;

import com.longvuong.plix.core.auth.AuthManager;

//AuthManager giả cho test luồng quên mật khẩu: không gọi mạng, trả ngay thành công hoặc lỗi đã đặt sẵn và ghi lại số lần gọi cùng tham số nhận được
class FakeAuthManagerForRecovery extends AuthManager {
    //null = các lời gọi trả thành công; khác null = các lời gọi trả onError(errorMessage)
    String errorMessage;

    int recoverCalls;
    int resetCalls;
    String lastEmail;
    String lastCode;
    String lastNewPassword;

    FakeAuthManagerForRecovery() {
        super(null); //không có EncryptedTokenStore nên không có phiên đăng nhập nào
    }

    @Override
    public void requestPasswordRecovery(String email, ActionCallback callback) {
        recoverCalls++;
        lastEmail = email;
        respond(callback);
    }

    @Override
    public void resetPasswordWithCode(String email, String code, String newPassword, ActionCallback callback) {
        resetCalls++;
        lastEmail = email;
        lastCode = code;
        lastNewPassword = newPassword;
        respond(callback);
    }

    private void respond(ActionCallback callback) {
        if (errorMessage == null) {
            callback.onSuccess();
        } else {
            callback.onError(errorMessage);
        }
    }
}