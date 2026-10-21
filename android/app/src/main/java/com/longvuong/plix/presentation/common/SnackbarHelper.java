package com.longvuong.plix.presentation.common;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;
import com.longvuong.plix.R;

//Cách hiện Snackbar thống nhất sau khi Thêm/Sửa thành công: gắn vào cửa sổ của Activity (vẫn còn sau khi Fragment đóng lại), nằm phía trên thanh điều hướng dưới khi thanh này đang hiện
public final class SnackbarHelper {
    private SnackbarHelper() {
    }

    public static void showSuccess(@NonNull Fragment fragment, @NonNull String message) {
        FragmentActivity activity = fragment.requireActivity();
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        View bottomNavigation = activity.findViewById(R.id.bottomNavigationView);
        if (bottomNavigation != null && bottomNavigation.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNavigation);
        }
        snackbar.show();
    }
}