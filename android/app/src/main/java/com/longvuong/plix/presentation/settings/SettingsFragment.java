package com.longvuong.plix.presentation.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.longvuong.plix.R;
import com.longvuong.plix.core.notification.NotificationHelper;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    @Inject
    NotificationHelper notificationHelper;

    private SettingsViewModel viewModel;
    private View bannerNotificationPermission;
    private View rowRetrainAi;
    private View badgeNewCorrections;
    private ProgressBar progressRetrainAi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        View rowCategoryManagement = view.findViewById(R.id.rowCategoryManagement);
        rowCategoryManagement.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_categoryManagementFragment));

        bannerNotificationPermission = view.findViewById(R.id.bannerNotificationPermission);
        bannerNotificationPermission.setOnClickListener(v -> startActivity(notificationHelper.buildNotificationSettingsIntent()));

        rowRetrainAi = view.findViewById(R.id.rowRetrainAi);
        badgeNewCorrections = view.findViewById(R.id.badgeNewCorrections);
        progressRetrainAi = view.findViewById(R.id.progressRetrainAi);
        rowRetrainAi.setOnClickListener(v -> viewModel.retrain());
        viewModel.getRetrainState().observe(getViewLifecycleOwner(), this::renderRetrainState);
    }

    @Override
    public void onResume() {
        super.onResume();
        bannerNotificationPermission.setVisibility(notificationHelper.isNotificationPermissionGranted() ? View.GONE : View.VISIBLE);
        badgeNewCorrections.setVisibility(viewModel.shouldShowNewCorrectionsBadge() ? View.VISIBLE : View.GONE);
    }

    private void renderRetrainState(UiState<Void> state) {
        if (state instanceof UiState.Loading) {
            rowRetrainAi.setEnabled(false);
            progressRetrainAi.setVisibility(View.VISIBLE);
        } else if (state instanceof UiState.Success) {
            rowRetrainAi.setEnabled(true);
            progressRetrainAi.setVisibility(View.GONE);
            badgeNewCorrections.setVisibility(View.GONE);
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Đã cập nhật mô hình gợi ý AI", Snackbar.LENGTH_SHORT).show();
        } else if (state instanceof UiState.Error) {
            rowRetrainAi.setEnabled(true);
            progressRetrainAi.setVisibility(View.GONE);
            String message = ((UiState.Error<Void>) state).message;
            Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }
}