package com.longvuong.plix.core.auth;

import androidx.annotation.Nullable;

//Điểm chung để mỗi tính năng tự dọn cấu hình theo người dùng của mình khi đăng xuất (NF-11)
public interface UserSessionCleaner {
    //userId có thể null (đăng xuất khi chưa xác định được người dùng), bộ dọn phải chịu được giá trị này
    void clearForUser(@Nullable String userId);
}