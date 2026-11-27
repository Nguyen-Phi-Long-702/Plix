package com.longvuong.plix.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.longvuong.plix.R;
import com.longvuong.plix.presentation.common.SnackbarHelper;
import com.longvuong.plix.presentation.common.UiState;

import dagger.hilt.android.AndroidEntryPoint;

//Màn Quên mật khẩu hai bước (AUTH-14, AUTH-16): chỉ hiển thị, gắn view, quan sát LiveData và điều hướng; mọi xử lý nằm ở ForgotPasswordViewModel
@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private ForgotPasswordViewModel viewModel;

    private View layoutStepEmail;
    private View layoutStepCode;
    private TextView textStepCodeDescription;
    private EditText editRecoverEmail;
    private Button buttonSendCode;
    private CircularProgressIndicator progressSendCode;
    private EditText editRecoveryCode;
    private EditText editNewPassword;
    private EditText editConfirmNewPassword;
    private Button buttonResetPassword;
    private CircularProgressIndicator progressResetPassword;
    private TextView textResendCode;
    private TextView textRecoveryError;
    private TextView textBackToLogin;

    private boolean busy; //đang gửi mã hoặc đang đặt lại mật khẩu

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        layoutStepEmail = view.findViewById(R.id.layoutStepEmail);
        layoutStepCode = view.findViewById(R.id.layoutStepCode);
        textStepCodeDescription = view.findViewById(R.id.textStepCodeDescription);
        editRecoverEmail = view.findViewById(R.id.editRecoverEmail);
        buttonSendCode = view.findViewById(R.id.buttonSendCode);
        progressSendCode = view.findViewById(R.id.progressSendCode);
        editRecoveryCode = view.findViewById(R.id.editRecoveryCode);
        editNewPassword = view.findViewById(R.id.editNewPassword);
        editConfirmNewPassword = view.findViewById(R.id.editConfirmNewPassword);
        buttonResetPassword = view.findViewById(R.id.buttonResetPassword);
        progressResetPassword = view.findViewById(R.id.progressResetPassword);
        textResendCode = view.findViewById(R.id.textResendCode);
        textRecoveryError = view.findViewById(R.id.textRecoveryError);
        textBackToLogin = view.findViewById(R.id.textBackToLogin);
        CharSequence sendCodeLabel = buttonSendCode.getText();
        CharSequence resetLabel = buttonResetPassword.getText();

        buttonSendCode.setOnClickListener(v ->
                viewModel.requestCode(editRecoverEmail.getText().toString().trim()));

        //Gửi lại mã dùng email đã nhập ở bước 1 (ViewModel giữ trong SavedStateHandle)
        textResendCode.setOnClickListener(v -> viewModel.requestCode(viewModel.getEmail()));

        //Cắt khoảng trắng hai đầu như màn Đăng nhập và Đăng ký, để mật khẩu đặt lại đăng nhập được
        buttonResetPassword.setOnClickListener(v -> viewModel.resetPassword(
                editRecoveryCode.getText().toString().trim(),
                editNewPassword.getText().toString().trim(),
                editConfirmNewPassword.getText().toString().trim()));

        textBackToLogin.setOnClickListener(v -> navigateToLogin());

        viewModel.getStep().observe(getViewLifecycleOwner(), step -> {
            boolean enterCode = step == ForgotPasswordViewModel.Step.ENTER_CODE;
            layoutStepEmail.setVisibility(enterCode ? View.GONE : View.VISIBLE);
            layoutStepCode.setVisibility(enterCode ? View.VISIBLE : View.GONE);
            if (enterCode) {
                textStepCodeDescription.setText(
                        "Đã gửi mã tới " + viewModel.getEmail() + ". Nhập mã và mật khẩu mới");
            }
        });

        viewModel.getRequestCodeState().observe(getViewLifecycleOwner(), state -> {
            boolean loading = state instanceof UiState.Loading;
            setBusy(loading);
            buttonSendCode.setText(loading ? "" : sendCodeLabel); //đang gửi: chữ nhường chỗ cho vòng xoay
            progressSendCode.setVisibility(loading ? View.VISIBLE : View.GONE);
            showError(state instanceof UiState.Error ? ((UiState.Error<Void>) state).message : null);
        });

        viewModel.getResetState().observe(getViewLifecycleOwner(), state -> {
            boolean loading = state instanceof UiState.Loading;
            setBusy(loading);
            buttonResetPassword.setText(loading ? "" : resetLabel);
            progressResetPassword.setVisibility(loading ? View.VISIBLE : View.GONE);
            showError(state instanceof UiState.Error ? ((UiState.Error<Void>) state).message : null);

            if (state instanceof UiState.Success) {
                SnackbarHelper.showSuccess(this, "Đã đặt lại mật khẩu, vui lòng đăng nhập");
                navigateToLogin();
            }
        });
    }

    //Đang xử lý: khoá các ô nhập và nút; nội dung các ô được giữ nguyên
    private void setBusy(boolean busy) {
        this.busy = busy;
        editRecoverEmail.setEnabled(!busy);
        buttonSendCode.setEnabled(!busy);
        editRecoveryCode.setEnabled(!busy);
        editNewPassword.setEnabled(!busy);
        editConfirmNewPassword.setEnabled(!busy);
        buttonResetPassword.setEnabled(!busy);
        textResendCode.setEnabled(!busy);
    }

    //Thông báo lỗi dùng chung cho cả hai bước; message = null thì ẩn
    private void showError(@Nullable String message) {
        textRecoveryError.setVisibility(message != null ? View.VISIBLE : View.GONE);
        textRecoveryError.setText(message);
    }

    //Về màn Đăng nhập và bỏ màn Quên mật khẩu khỏi back stack (cùng kết quả với action_forgotPassword_to_login ở DP_02 mục 1.6)
    private void navigateToLogin() {
        Navigation.findNavController(requireView()).navigate(
                R.id.loginFragment,
                null,
                new NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build());
    }
}