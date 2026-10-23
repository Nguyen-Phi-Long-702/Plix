package com.longvuong.plix.presentation.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AuthViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AuthViewModel viewModel;

    @Before
    public void setUp() {
        //Các test dưới đây đều dừng ngay ở bước kiểm tra nhập lại mật khẩu, không chạm tới mạng hay đồng bộ nên SyncRepository để null
        viewModel = new AuthViewModel(new AuthManager(null), null);
    }

    @Test
    public void register_confirmPasswordDiffers_emitsMismatchError() {
        viewModel.register("long@gmail.com", "12345678", "12345679");

        assertMismatchError(viewModel.getAuthState().getValue());
    }

    @Test
    public void register_confirmPasswordDiffersByCase_emitsMismatchError() {
        viewModel.register("long@gmail.com", "Matkhau123", "matkhau123");

        assertMismatchError(viewModel.getAuthState().getValue());
    }

    @Test
    public void register_confirmPasswordEmpty_emitsMismatchError() {
        viewModel.register("long@gmail.com", "12345678", "");

        assertMismatchError(viewModel.getAuthState().getValue());
    }

    private void assertMismatchError(UiState<Void> state) {
        assertTrue(state instanceof UiState.Error);
        assertEquals("Mật khẩu nhập lại không khớp", ((UiState.Error<Void>) state).message);
    }
}