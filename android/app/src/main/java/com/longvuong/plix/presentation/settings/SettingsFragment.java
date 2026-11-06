package com.longvuong.plix.presentation.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.R;
import com.longvuong.plix.core.notification.NotificationHelper;
import com.longvuong.plix.domain.usecase.auth.LogoutOutcome;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;
import java.io.File;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    @Inject
    NotificationHelper notificationHelper;

    private SettingsViewModel viewModel;
    private SwitchMaterial switchQuickAdd;
    private SwitchMaterial switchGeminiFallback;
    private View groupNotificationPermission;
    private View bannerNotificationPermission;
    private View rowRetrainAi;
    private TextView badgeNewCorrections;
    private View textRetrainHint;
    private View imageRetrainChevron;
    private ProgressBar progressRetrainAi;
    private ExportViewModel exportViewModel;
    private View rowExportCsv;
    private View imageExportChevron;
    private ProgressBar progressExportCsv;
    private View rowLogout;
    private ProgressBar progressLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        TextView textAccountEmail = view.findViewById(R.id.textAccountEmail);
        String email = viewModel.getCurrentUserEmail();
        if (email != null) {
            textAccountEmail.setText(email);
        }

        View rowCategoryManagement = view.findViewById(R.id.rowCategoryManagement);
        rowCategoryManagement.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_categoryManagementFragment));
        TextView textCategorySummary = view.findViewById(R.id.textCategorySummary);
        viewModel.getCategorySummary().observe(getViewLifecycleOwner(), summary -> textCategorySummary.setText(summary));

        switchQuickAdd = view.findViewById(R.id.switchQuickAdd);
        switchGeminiFallback = view.findViewById(R.id.switchGeminiFallback);
        view.findViewById(R.id.rowQuickAdd).setOnClickListener(v -> switchQuickAdd.toggle());
        view.findViewById(R.id.rowGeminiFallback).setOnClickListener(v -> switchGeminiFallback.toggle());
        switchQuickAdd.setOnCheckedChangeListener((button, isChecked) -> viewModel.setQuickAddEnabled(isChecked));
        switchGeminiFallback.setOnCheckedChangeListener((button, isChecked) -> viewModel.setGeminiFallbackEnabled(isChecked));
        viewModel.getQuickAddEnabled().observe(getViewLifecycleOwner(), enabled -> switchQuickAdd.setChecked(enabled));
        viewModel.getGeminiFallbackEnabled().observe(getViewLifecycleOwner(), enabled -> switchGeminiFallback.setChecked(enabled));

        groupNotificationPermission = view.findViewById(R.id.groupNotificationPermission);
        bannerNotificationPermission = view.findViewById(R.id.bannerNotificationPermission);
        bannerNotificationPermission.setOnClickListener(v -> startActivity(notificationHelper.buildNotificationSettingsIntent()));

        rowRetrainAi = view.findViewById(R.id.rowRetrainAi);
        badgeNewCorrections = view.findViewById(R.id.badgeNewCorrections);
        textRetrainHint = view.findViewById(R.id.textRetrainHint);
        imageRetrainChevron = view.findViewById(R.id.imageRetrainChevron);
        progressRetrainAi = view.findViewById(R.id.progressRetrainAi);
        rowRetrainAi.setOnClickListener(v -> viewModel.retrain());
        viewModel.getRetrainState().observe(getViewLifecycleOwner(), this::renderRetrainState);

        exportViewModel = new ViewModelProvider(this).get(ExportViewModel.class);
        rowExportCsv = view.findViewById(R.id.rowExportCsv);
        imageExportChevron = view.findViewById(R.id.imageExportChevron);
        progressExportCsv = view.findViewById(R.id.progressExportCsv);
        rowExportCsv.setOnClickListener(v -> exportViewModel.exportCsv());
        exportViewModel.getExportState().observe(getViewLifecycleOwner(), this::renderExportState);

        view.findViewById(R.id.rowAbout).setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_aboutFragment));
        rowLogout = view.findViewById(R.id.rowLogout);
        progressLogout = view.findViewById(R.id.progressLogout);
        rowLogout.setOnClickListener(v -> viewModel.logout());
        viewModel.getLogoutState().observe(getViewLifecycleOwner(), this::renderLogoutState);

        TextView textAppVersion = view.findViewById(R.id.textAppVersion);
        textAppVersion.setText("Plix v" + BuildConfig.VERSION_NAME);
    }

    @Override
    public void onResume() {
        super.onResume();
        groupNotificationPermission.setVisibility(notificationHelper.isNotificationPermissionGranted() ? View.GONE : View.VISIBLE);
        showNewCorrectionsBadge(viewModel.shouldShowNewCorrectionsBadge());
    }

    //Huy hiệu "N mới" và dòng gợi ý huấn luyện lại hiện cùng nhau khi đã đủ số lần sửa gợi ý
    private void showNewCorrectionsBadge(boolean show) {
        if (show) {
            badgeNewCorrections.setText(viewModel.getPendingCorrectionCount() + " mới");
        }
        int visibility = show ? View.VISIBLE : View.GONE;
        badgeNewCorrections.setVisibility(visibility);
        textRetrainHint.setVisibility(visibility);
    }

    private void renderRetrainState(UiState<Void> state) {
        if (state instanceof UiState.Loading) {
            rowRetrainAi.setEnabled(false);
            imageRetrainChevron.setVisibility(View.GONE);
            progressRetrainAi.setVisibility(View.VISIBLE);
        } else if (state instanceof UiState.Success) {
            rowRetrainAi.setEnabled(true);
            imageRetrainChevron.setVisibility(View.VISIBLE);
            progressRetrainAi.setVisibility(View.GONE);
            showNewCorrectionsBadge(false);
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Đã cập nhật mô hình gợi ý AI", Snackbar.LENGTH_SHORT).show();
        } else if (state instanceof UiState.Error) {
            rowRetrainAi.setEnabled(true);
            imageRetrainChevron.setVisibility(View.VISIBLE);
            progressRetrainAi.setVisibility(View.GONE);
            String message = ((UiState.Error<Void>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }
    
    private void renderExportState(UiState<File> state) {
        boolean loading = state instanceof UiState.Loading;
        rowExportCsv.setEnabled(!loading);
        imageExportChevron.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressExportCsv.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (state instanceof UiState.Success) {
            shareCsvFile(((UiState.Success<File>) state).data);
            exportViewModel.onExportHandled();
        } else if (state instanceof UiState.Error) {
            String message = ((UiState.Error<File>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            exportViewModel.onExportHandled();
        }
    }

    //Mở bảng chia sẻ của hệ thống để người dùng chọn ứng dụng nhận file CSV
    private void shareCsvFile(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/csv");
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ file CSV"));
    }

    private void renderLogoutState(UiState<LogoutOutcome> state) {
        boolean loading = state instanceof UiState.Loading;
        rowLogout.setEnabled(!loading);
        progressLogout.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (state instanceof UiState.Success) {
            LogoutOutcome outcome = ((UiState.Success<LogoutOutcome>) state).data;
            if (!outcome.isLoggedOut) {
                LogoutConfirmationDialog.show(requireContext(), outcome.pendingCount, viewModel::cancelLogout, viewModel::confirmLogoutAnyway);
            }
            //Đã đăng xuất: MainActivity tự đưa người dùng về màn hình đăng nhập (luồng "cần đăng nhập lại" có sẵn)
        } else if (state instanceof UiState.Error) {
            String message = ((UiState.Error<LogoutOutcome>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }
}