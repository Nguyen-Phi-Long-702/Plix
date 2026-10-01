package com.longvuong.plix.presentation.sync;

import android.content.res.ColorStateList;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.longvuong.plix.R;
import com.longvuong.plix.data.sync.SyncStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SyncIndicatorMenuProvider implements MenuProvider {
    private static final long SPIN_DURATION_MILLIS = 1100L;
    private final AppCompatActivity activity;
    private final SyncStatusViewModel viewModel;
    private final NavController navController;
    private final Set<Integer> visibleDestinationIds = new HashSet<>(Arrays.asList(
            R.id.homeFragment,
            R.id.transactionListFragment,
            R.id.budgetGoalFragment,
            R.id.goalListFragment,
            R.id.analyticsFragment));

    private SyncStatus currentStatus = SyncStatus.IDLE;

    public SyncIndicatorMenuProvider(AppCompatActivity activity, SyncStatusViewModel viewModel, NavController navController) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.navController = navController;
        viewModel.getSyncStatus().observe(activity, status -> {
            currentStatus = status;
            activity.invalidateMenu();
        });
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> activity.invalidateMenu());
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_sync_indicator, menu);
        View actionView = menu.findItem(R.id.action_sync_status).getActionView();
        if (actionView != null) {
            actionView.setOnClickListener(v -> showStatusDialog());
        }
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.action_sync_status);
        if (item == null) {
            return;
        }
        NavDestination destination = navController.getCurrentDestination();
        item.setVisible(destination != null && visibleDestinationIds.contains(destination.getId()));
        View actionView = item.getActionView();
        if (actionView != null) {
            render(actionView);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    private void render(View actionView) {
        ImageView icon = actionView.findViewById(R.id.imageSyncIcon);
        View errorDot = actionView.findViewById(R.id.viewSyncErrorDot);
        icon.clearAnimation();
        errorDot.setVisibility(currentStatus == SyncStatus.ERROR ? View.VISIBLE : View.GONE);

        int colorRes;
        switch (currentStatus) {
            case SYNCING:
                colorRes = R.color.sync_syncing;
                icon.startAnimation(createSpinAnimation());
                break;
            case ERROR:
                colorRes = R.color.sync_error;
                break;
            default:
                colorRes = R.color.sync_idle;
                break;
        }
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(activity, colorRes)));
    }

    private Animation createSpinAnimation() {
        RotateAnimation animation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(SPIN_DURATION_MILLIS);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setInterpolator(new LinearInterpolator());
        return animation;
    }

    private void showStatusDialog() {
        String message;
        switch (currentStatus) {
            case SYNCING:
                message = "Đang đồng bộ dữ liệu lên máy chủ...";
                break;
            case ERROR: {
                String lastError = viewModel.getLastSyncError();
                message = "Đồng bộ thất bại" + (lastError != null ? ": " + lastError : "") + ".\nDữ liệu vẫn an toàn trên máy - Plix sẽ tự thử lại khi có thể.";
                break;
            }
            default:
                message = "Không có lỗi đồng bộ. Các thay đổi trên máy sẽ tự được gửi lên máy chủ khi có mạng.";
                break;
        }
        new MaterialAlertDialogBuilder(activity).setTitle("Trạng thái đồng bộ").setMessage(message).setPositiveButton("Đóng", null).show();
    }
}