package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.presentation.auth.FakeAuthManager;
import com.longvuong.plix.presentation.sync.FakeSyncRepository;

import org.junit.Before;
import org.junit.Test;

public class ForegroundSyncObserverTest {
    private FakeSyncRepository fakeSyncRepository;
    private FakeAuthManager fakeAuthManager;
    private ForegroundSyncObserver observer;

    @Before
    public void setUp() {
        fakeSyncRepository = new FakeSyncRepository();
        fakeAuthManager = new FakeAuthManager();
        observer = new ForegroundSyncObserver(fakeSyncRepository, fakeAuthManager);
    }

    /**
     * TN5-U08
     * isLoggedIn = true; onForeground(now = 100_000); rồi onForeground(now = 100_000 + 30_000); rồi onForeground(now = 100_000 + 60_000): requestSyncCalls: 1 sau lần 1; vẫn 1 sau lần 2 (30 giây); thành 2 sau lần 3 (đủ 60 giây).
     */
    @Test
    public void onForeground_whenLoggedIn_requestsSyncAtMostOncePerMinute() {
        fakeAuthManager.loggedIn = true;

        observer.onForeground(100_000L);
        assertEquals(1, fakeSyncRepository.requestSyncCalls);

        observer.onForeground(100_000L + 30_000L);
        assertEquals(1, fakeSyncRepository.requestSyncCalls);

        observer.onForeground(100_000L + 60_000L);
        assertEquals(2, fakeSyncRepository.requestSyncCalls);
    }

    /**
     * TN5-U09
     * isLoggedIn = false; onForeground(now): requestSyncCalls = 0.
     */
    @Test
    public void onForeground_whenNotLoggedIn_doesNotRequestSync() {
        fakeAuthManager.loggedIn = false;

        observer.onForeground(100_000L);

        assertEquals(0, fakeSyncRepository.requestSyncCalls);
    }
}