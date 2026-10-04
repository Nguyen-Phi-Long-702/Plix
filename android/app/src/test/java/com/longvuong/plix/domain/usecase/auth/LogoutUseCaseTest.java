package com.longvuong.plix.domain.usecase.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class LogoutUseCaseTest {
    private FakeLogoutRepository fakeRepository;
    private LogoutUseCase useCase;

    @Before
    public void setUp() {
        fakeRepository = new FakeLogoutRepository();
        useCase = new LogoutUseCase(fakeRepository);
    }

    //Nhánh 1: không có dữ liệu chờ đồng bộ -> đăng xuất ngay, không thử đồng bộ
    @Test
    public void start_noPending_logsOutImmediatelyWithoutFinalSync() {
        fakeRepository.pendingCount = 0;

        LogoutOutcome outcome = useCase.start();

        assertTrue(outcome.isLoggedOut);
        assertEquals(Collections.singletonList("clear"), fakeRepository.calls);
    }

    //Nhánh 2: còn pending + có mạng + đẩy lần cuối thành công -> đăng xuất; huỷ SyncWorker trước khi đẩy, đẩy trước khi xoá
    @Test
    public void start_pendingOnlineAndFinalSyncSucceeds_logsOutAfterCancelAndPush() {
        fakeRepository.pendingCount = 3;
        fakeRepository.online = true;
        fakeRepository.pendingCountAfterPush = 0;

        LogoutOutcome outcome = useCase.start();

        assertTrue(outcome.isLoggedOut);
        assertEquals(Arrays.asList("cancel", "push:" + LogoutUseCase.FINAL_SYNC_TIMEOUT_MILLIS, "clear"), fakeRepository.calls);
    }

    //Nhánh 3: còn pending + mất mạng -> bỏ qua bước đẩy, hỏi xác nhận, chưa xoá gì
    @Test
    public void start_pendingAndOffline_skipsFinalSyncAndAsksConfirmation() {
        fakeRepository.pendingCount = 3;
        fakeRepository.online = false;

        LogoutOutcome outcome = useCase.start();

        assertFalse(outcome.isLoggedOut);
        assertEquals(3, outcome.pendingCount);
        assertEquals(Collections.singletonList("cancel"), fakeRepository.calls);
    }

    //Server phản hồi chậm/quá hạn 10 giây: đẩy không xong, pending giữ nguyên -> hỏi xác nhận, chưa xoá gì
    @Test
    public void start_finalSyncTimesOut_asksConfirmationWithoutClearing() {
        fakeRepository.pendingCount = 3;
        fakeRepository.online = true;
        fakeRepository.pendingCountAfterPush = null;

        LogoutOutcome outcome = useCase.start();

        assertFalse(outcome.isLoggedOut);
        assertEquals(3, outcome.pendingCount);
        assertEquals(Arrays.asList("cancel", "push:" + LogoutUseCase.FINAL_SYNC_TIMEOUT_MILLIS), fakeRepository.calls);
    }

    //Đẩy thành công một phần: số hiển thị trong hộp thoại là số bản ghi CÒN LẠI
    @Test
    public void start_finalSyncPartiallySucceeds_reportsRemainingCount() {
        fakeRepository.pendingCount = 5;
        fakeRepository.online = true;
        fakeRepository.pendingCountAfterPush = 2;

        LogoutOutcome outcome = useCase.start();

        assertFalse(outcome.isLoggedOut);
        assertEquals(2, outcome.pendingCount);
        assertFalse(fakeRepository.calls.contains("clear"));
    }

    //Chọn "Vẫn đăng xuất (mất thay đổi chưa đồng bộ)"
    @Test
    public void confirmLogoutAnyway_clearsLocalSession() {
        useCase.confirmLogoutAnyway();

        assertEquals(Collections.singletonList("clear"), fakeRepository.calls);
    }

    //Chọn "Huỷ, để tôi thử lại sau": không xoá gì, bật lại đồng bộ nền
    @Test
    public void cancelLogout_resumesSyncAndDoesNotClear() {
        useCase.cancelLogout();

        assertEquals(Collections.singletonList("resume"), fakeRepository.calls);
    }
}