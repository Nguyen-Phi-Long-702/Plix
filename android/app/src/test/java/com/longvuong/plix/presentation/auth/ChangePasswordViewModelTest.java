package com.longvuong.plix.presentation.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ChangePasswordViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAuthManagerForRecovery fakeAuthManager;
    private ChangePasswordViewModel viewModel;

    @Before
    public void setUp() {
        fakeAuthManager = new FakeAuthManagerForRecovery();
        viewModel = new ChangePasswordViewModel(fakeAuthManager, new FormValidator());
    }

    private void assertError(String expectedMessage, UiState<Void> state) {
        assertTrue(state instanceof UiState.Error);
        assertEquals(expectedMessage, ((UiState.Error<Void>) state).message);
    }

    /**
     * TN1-U13
     * changePassword("", "Abc12345", "Abc12345"): Error 'Vui lòng nhập mật khẩu hiện tại'; changeCalls = 0.
     */
    @Test
    public void changePassword_emptyCurrentPassword_emitsErrorWithoutCallingAuthManager() {
        viewModel.changePassword("", "Abc12345", "Abc12345");

        assertError("Vui lòng nhập mật khẩu hiện tại", viewModel.getChangeState().getValue());
        assertEquals(0, fakeAuthManager.verifyCalls);
        assertEquals(0, fakeAuthManager.changeCalls);
    }

    /**
     * TN1-U14
     * changePassword("Abc12345", "Abc12345", "Abc12345"): Error 'Mật khẩu mới phải khác mật khẩu hiện tại'; changeCalls = 0 (không gọi mạng).
     */
    @Test
    public void changePassword_newPasswordSameAsCurrent_emitsErrorWithoutCallingAuthManager() {
        viewModel.changePassword("Abc12345", "Abc12345", "Abc12345");

        assertError("Mật khẩu mới phải khác mật khẩu hiện tại", viewModel.getChangeState().getValue());
        assertEquals(0, fakeAuthManager.verifyCalls);
        assertEquals(0, fakeAuthManager.changeCalls);
    }

    /**
     * TN1-U15 (a)
     * AuthManager giả thành công: state Success; đã xác minh mật khẩu hiện tại rồi mới đổi mật khẩu (mỗi bước đúng 1 lần).
     */
    @Test
    public void changePassword_success_verifiesThenChangesAndEmitsSuccess() {
        viewModel.changePassword("Abc12345", "Xyz67890", "Xyz67890");

        assertTrue(viewModel.getChangeState().getValue() instanceof UiState.Success);
        assertEquals(1, fakeAuthManager.verifyCalls);
        assertEquals(1, fakeAuthManager.changeCalls);
    }

    /**
     * TN1-U15 (b)
     * AuthManager giả onError('Email hoặc mật khẩu không đúng'): state Error đúng nguyên văn; changeCalls = 0 (không đổi mật khẩu).
     */
    @Test
    public void changePassword_currentPasswordWrong_emitsErrorAndDoesNotChange() {
        fakeAuthManager.errorMessage = "Email hoặc mật khẩu không đúng";

        viewModel.changePassword("Abc12345", "Xyz67890", "Xyz67890");

        assertError("Email hoặc mật khẩu không đúng", viewModel.getChangeState().getValue());
        assertEquals(1, fakeAuthManager.verifyCalls);
        assertEquals(0, fakeAuthManager.changeCalls);
    }
}