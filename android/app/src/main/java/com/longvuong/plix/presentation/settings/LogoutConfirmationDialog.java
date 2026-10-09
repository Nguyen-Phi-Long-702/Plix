package com.longvuong.plix.presentation.settings;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.longvuong.plix.R;

//Hộp thoại xác nhận bắt buộc khi còn dữ liệu chưa đồng bộ (Mục 6.9 bước 4): overlay trên màn hiện tại, không phải route Navigation
public final class LogoutConfirmationDialog {

    private LogoutConfirmationDialog() {
    }

    public static void show(Context context, int pendingCount, Runnable onCancel, Runnable onConfirm) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Còn dữ liệu chưa đồng bộ")
                .setMessage("Bạn có " + pendingCount + " thay đổi chưa đồng bộ lên cloud. "
                        + "Nếu đăng xuất bây giờ, các thay đổi này sẽ MẤT trên thiết bị này "
                        + "(dữ liệu đã đồng bộ trước đó vẫn an toàn trên cloud).")
                .setCancelable(false) //bắt buộc chọn 1 trong 2, không đóng được bằng nút Back hay chạm ra ngoài
                .setNegativeButton("Huỷ, để tôi thử lại sau", (d, which) -> onCancel.run())
                .setPositiveButton("Vẫn đăng xuất (mất thay đổi chưa đồng bộ)", (d, which) -> onConfirm.run())
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.budget_progress_danger));
    }
}