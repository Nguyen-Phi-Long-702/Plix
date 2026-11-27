package com.longvuong.plix.core.notification;

//Giao diện hẹp chỉ hỏi trạng thái quyền thông báo, để lớp không dùng Android (ví dụ ReminderSettingsViewModel) và test không cần Context
public interface NotificationPermissionChecker {
    boolean isNotificationPermissionGranted();
}
