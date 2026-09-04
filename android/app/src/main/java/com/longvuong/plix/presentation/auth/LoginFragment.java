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
import androidx.navigation.Navigation;

import com.longvuong.plix.R;

public class LoginFragment extends Fragment {

    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        EditText editEmail = view.findViewById(R.id.editEmail);
        EditText editPassword = view.findViewById(R.id.editPassword);
        TextView textResult = view.findViewById(R.id.textResult);
        Button buttonLogin = view.findViewById(R.id.buttonLogin);
        TextView textGoRegister = view.findViewById(R.id.textGoRegister);

        buttonLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            textResult.setText("Đang đăng nhập...");
            viewModel.login(email, password);
        });

        textGoRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register));

        viewModel.getAuthSuccessToken().observe(getViewLifecycleOwner(), token ->
                textResult.setText("Đăng nhập thành công!\nJWT: " + token));

        viewModel.getAuthError().observe(getViewLifecycleOwner(), error ->
                textResult.setText("Lỗi: " + error));
    }
}