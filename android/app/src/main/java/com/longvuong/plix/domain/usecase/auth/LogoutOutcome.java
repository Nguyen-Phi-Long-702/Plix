package com.longvuong.plix.domain.usecase.auth;

//Kết quả của bước kiểm tra + đồng bộ lần cuối: hoặc đã đăng xuất xong, hoặc cần người dùng xác nhận vì còn dữ liệu chưa đồng bộ
public final class LogoutOutcome {
    public final boolean isLoggedOut;
    public final int pendingCount;

    private LogoutOutcome(boolean isLoggedOut, int pendingCount) {
        this.isLoggedOut = isLoggedOut;
        this.pendingCount = pendingCount;
    }

    public static LogoutOutcome loggedOut() {
        return new LogoutOutcome(true, 0);
    }

    public static LogoutOutcome needsConfirmation(int pendingCount) {
        return new LogoutOutcome(false, pendingCount);
    }
}