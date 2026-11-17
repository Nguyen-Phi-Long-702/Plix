package com.longvuong.plix.presentation.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ForgotPasswordViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private SavedStateHandle savedStateHandle;
    private FakeAuthManagerForRecovery fakeAuthManager;
    private ForgotPasswordViewModel viewModel;

    @Before
    public void setUp() {
        savedStateHandle = new SavedStateHandle();
        fakeAuthManager = new FakeAuthManagerForRecovery();
        viewModel = new ForgotPasswordViewModel(savedStateHandle, fakeAuthManager, new FormValidator());
    }

    private void assertError(String expectedMessage, UiState<Void> state) {
        assertTrue(state instanceof UiState.Error);
        assertEquals(expectedMessage, ((UiState.Error<Void>) state).message);
    }

    /**
     * TN1-U08
     * requestCode(""): requestCodeState = Error 'Vui lòng nhập email'; recoverCalls = 0; step vẫn ENTER_EMAIL.
     */
    @Test
    public void requestCode_emptyEmail_emitsErrorAndStaysOnEnterEmail() {
        viewModel.requestCode("");

        assertError("Vui lòng nhập email", viewModel.getRequestCodeState().getValue());
        assertEquals(0, fakeAuthManager.recoverCalls);
        assertEquals(ForgotPasswordViewModel.Step.ENTER_EMAIL, viewModel.getStep().getValue());
    }

    /**
     * TN1-U09
     * requestCode('a@b.c') thành công: step = ENTER_CODE; requestCodeState = Success; recoverCalls = 1;
     * canResend(now) = false, canResend(now + 60_000) = true.
     */
    @Test
    public void requestCode_success_movesToEnterCodeAndStartsResendCooldown() {
        viewModel.requestCode("a@b.c");

        assertEquals(ForgotPasswordViewModel.Step.ENTER_CODE, viewModel.getStep().getValue());
        assertTrue(viewModel.getRequestCodeState().getValue() instanceof UiState.Success);
        assertEquals(1, fakeAuthManager.recoverCalls);
        //Lấy đúng thời điểm gửi mà ViewModel đã ghi để kết quả không phụ thuộc đồng hồ của máy chạy test
        Long sentAt = savedStateHandle.get(ForgotPasswordViewModel.KEY_LAST_SENT_AT);
        assertFalse(viewModel.canResend(sentAt));
        assertTrue(viewModel.canResend(sentAt + ForgotPasswordViewModel.RESEND_COOLDOWN_MILLIS));
    }

    /**
     * TN1-U10
     * requestCode('a@b.c') nhận onError('Không có kết nối mạng'): step vẫn ENTER_EMAIL; requestCodeState = Error('Không có kết nối mạng').
     */
    @Test
    public void requestCode_networkError_keepsEnterEmailStep() {
        fakeAuthManager.errorMessage = "Không có kết nối mạng";

        viewModel.requestCode("a@b.c");

        assertEquals(ForgotPasswordViewModel.Step.ENTER_EMAIL, viewModel.getStep().getValue());
        assertError("Không có kết nối mạng", viewModel.getRequestCodeState().getValue());
    }

    /**
     * TN1-U11
     * resetPassword('123456', 'Abc12345', 'Abc12346'): resetState = Error 'Mật khẩu nhập lại không khớp'; resetCalls = 0.
     */
    @Test
    public void resetPassword_confirmDiffers_emitsMismatchErrorWithoutCallingAuthManager() {
        viewModel.resetPassword("123456", "Abc12345", "Abc12346");

        assertError("Mật khẩu nhập lại không khớp", viewModel.getResetState().getValue());
        assertEquals(0, fakeAuthManager.resetCalls);
    }

    /**
     * TN1-U12
     * resetPassword('123456', 'Abc12345', 'Abc12345') thành công: resetState = Success; AuthManager giả nhận đúng (email, code, newPassword);
     * getAccessToken() = null và isLoggedIn() = false sau đó (không tạo phiên đăng nhập).
     */
    @Test
    public void resetPassword_success_passesArgumentsAndCreatesNoSession() {
        viewModel.requestCode("a@b.c"); //sang bước 2 để ViewModel có email

        viewModel.resetPassword("123456", "Abc12345", "Abc12345");

        assertTrue(viewModel.getResetState().getValue() instanceof UiState.Success);
        assertEquals(1, fakeAuthManager.resetCalls);
        assertEquals("a@b.c", fakeAuthManager.lastEmail);
        assertEquals("123456", fakeAuthManager.lastCode);
        assertEquals("Abc12345", fakeAuthManager.lastNewPassword);
        assertNull(fakeAuthManager.getAccessToken());
        assertFalse(fakeAuthManager.isLoggedIn());
    }
}