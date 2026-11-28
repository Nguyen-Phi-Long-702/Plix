package com.longvuong.plix.presentation.settings;

import com.longvuong.plix.core.notification.NotificationPermissionChecker;

//Quyền thông báo giả: test đặt granted để điều khiển kết quả, không cần Context
public class FakeNotificationPermission implements NotificationPermissionChecker {
    public boolean granted = true;

    @Override
    public boolean isNotificationPermissionGranted() {
        return granted;
    }
}
