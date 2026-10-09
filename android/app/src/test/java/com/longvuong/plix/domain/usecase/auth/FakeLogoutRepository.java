package com.longvuong.plix.domain.usecase.auth;

import com.longvuong.plix.data.repository.LogoutRepository;

import java.util.ArrayList;
import java.util.List;

class FakeLogoutRepository implements LogoutRepository {
    int pendingCount;
    boolean online;
    //null = lần đẩy lần cuối không làm đổi số pending (thất bại/quá hạn); có giá trị = số pending còn lại sau khi đẩy
    Integer pendingCountAfterPush;
    final List<String> calls = new ArrayList<>();

    @Override
    public int countPendingChanges() {
        return pendingCount;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    public void cancelRunningSync() {
        calls.add("cancel");
    }

    @Override
    public void pushAllPending(long timeoutMillis) {
        calls.add("push:" + timeoutMillis);
        if (pendingCountAfterPush != null) {
            pendingCount = pendingCountAfterPush;
        }
    }

    @Override
    public void clearLocalSession() {
        calls.add("clear");
    }

    @Override
    public void resumeSync() {
        calls.add("resume");
    }
}