package com.longvuong.plix.core.auth;

import androidx.annotation.Nullable;

import java.util.Set;

import timber.log.Timber;

//Gọi lần lượt mọi UserSessionCleaner đã đăng ký; một bộ dọn lỗi không được chặn các bộ dọn khác hay chặn đăng xuất
public final class UserSessionCleaners {

    private UserSessionCleaners() {
    }

    public static void clearAll(Set<UserSessionCleaner> cleaners, @Nullable String userId) {
        for (UserSessionCleaner cleaner : cleaners) {
            try {
                cleaner.clearForUser(userId);
            } catch (RuntimeException e) {
                //Chỉ ghi loại lỗi, không ghi userId hay nội dung lỗi để không lộ dữ liệu người dùng (SEC-08)
                Timber.w("Một bộ dọn theo người dùng thất bại khi đăng xuất: %s", e.getClass().getSimpleName());
            }
        }
    }
}