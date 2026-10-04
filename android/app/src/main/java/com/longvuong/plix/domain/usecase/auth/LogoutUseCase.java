package com.longvuong.plix.domain.usecase.auth;

import com.longvuong.plix.data.repository.LogoutRepository;

import javax.inject.Inject;

//Logout Flow. Các method chạy đồng bộ, ViewModel gọi trên luồng nền
public class LogoutUseCase {
    static final long FINAL_SYNC_TIMEOUT_MILLIS = 10_000L;

    private final LogoutRepository logoutRepository;

    @Inject
    public LogoutUseCase(LogoutRepository logoutRepository) {
        this.logoutRepository = logoutRepository;
    }

    //Bước 2-3: kiểm tra pending (5 bảng) -> huỷ/chờ SyncWorker -> thử đồng bộ lần cuối (bỏ qua nếu mất mạng)
    //Không còn pending thì đăng xuất luôn, còn thì trả về kết quả "cần xác nhận" kèm số bản ghi còn lại; không tự xoá âm thầm
    public LogoutOutcome start() {
        if (logoutRepository.countPendingChanges() == 0) {
            logoutRepository.clearLocalSession();
            return LogoutOutcome.loggedOut();
        }
        logoutRepository.cancelRunningSync();
        if (logoutRepository.isOnline()) {
            logoutRepository.pushAllPending(FINAL_SYNC_TIMEOUT_MILLIS);
        }
        int remaining = logoutRepository.countPendingChanges();
        if (remaining == 0) {
            logoutRepository.clearLocalSession();
            return LogoutOutcome.loggedOut();
        }
        return LogoutOutcome.needsConfirmation(remaining);
    }

    //Bước 4: người dùng chọn "Vẫn đăng xuất (mất thay đổi chưa đồng bộ)"
    public void confirmLogoutAnyway() {
        logoutRepository.clearLocalSession();
    }

    //Bước 4: người dùng chọn "Huỷ, để tôi thử lại sau" -> không đăng xuất, không xoá gì, bật lại đồng bộ nền
    public void cancelLogout() {
        logoutRepository.resumeSync();
    }
}