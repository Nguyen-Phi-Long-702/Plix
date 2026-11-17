package com.longvuong.plix.presentation.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.data.sync.SyncStatus;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncRefreshViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeSyncRepository fakeSyncRepository;
    private SyncRefreshViewModel viewModel;
    private List<Boolean> refreshingHistory;

    @Before
    public void setUp() {
        fakeSyncRepository = new FakeSyncRepository();
        viewModel = new SyncRefreshViewModel(fakeSyncRepository);
        refreshingHistory = new ArrayList<>(); //ghi lại mọi giá trị refreshing theo thứ tự, gồm giá trị ban đầu false
        viewModel.getRefreshing().observeForever(value -> refreshingHistory.add(value));
    }

    /**
     * TN5-U03
     * Online; requestRefresh(); status IDLE -> SYNCING -> IDLE: refreshing true rồi false; requestSyncNowCalls = 1; event SUCCESS 'Đã đồng bộ'.
     */
    @Test
    public void requestRefresh_whenOnlineAndSyncSucceeds_emitsSuccess() {
        viewModel.requestRefresh();
        fakeSyncRepository.syncStatus.setValue(SyncStatus.SYNCING);
        fakeSyncRepository.syncStatus.setValue(SyncStatus.IDLE);

        assertEquals(Arrays.asList(false, true, false), refreshingHistory);
        assertEquals(1, fakeSyncRepository.requestSyncNowCalls);
        SyncRefreshEvent event = viewModel.getEvent().getValue();
        assertEquals(SyncRefreshEvent.Type.SUCCESS, event.type);
        assertEquals("Đã đồng bộ", event.message);
    }

    /**
     * TN5-U04
     * isOnline = false; requestRefresh(): refreshing không bao giờ true; event OFFLINE 'Không có mạng, dữ liệu sẽ đồng bộ khi có mạng'; requestSyncNowCalls = 1.
     */
    @Test
    public void requestRefresh_whenOffline_emitsOfflineWithoutRefreshing() {
        fakeSyncRepository.online = false;

        viewModel.requestRefresh();

        assertFalse(refreshingHistory.contains(true));
        assertEquals(1, fakeSyncRepository.requestSyncNowCalls);
        SyncRefreshEvent event = viewModel.getEvent().getValue();
        assertEquals(SyncRefreshEvent.Type.OFFLINE, event.type);
        assertEquals("Không có mạng, dữ liệu sẽ đồng bộ khi có mạng", event.message);
    }

    /**
     * TN5-U05
     * status SYNCING -> ERROR; getLastSyncError = 'Máy chủ phản hồi lỗi (mã 503)': refreshing false; event ERROR kèm đúng chuỗi lỗi.
     */
    @Test
    public void requestRefresh_whenSyncEndsWithError_emitsErrorWithLastError() {
        fakeSyncRepository.lastSyncError = "Máy chủ phản hồi lỗi (mã 503)";

        viewModel.requestRefresh();
        fakeSyncRepository.syncStatus.setValue(SyncStatus.SYNCING);
        fakeSyncRepository.syncStatus.setValue(SyncStatus.ERROR);

        assertFalse(viewModel.getRefreshing().getValue());
        SyncRefreshEvent event = viewModel.getEvent().getValue();
        assertEquals(SyncRefreshEvent.Type.ERROR, event.type);
        assertEquals("Máy chủ phản hồi lỗi (mã 503)", event.message);
    }

    /**
     * TN5-U06
     * requestRefresh() rồi gọi onRefreshTimeout() khi chưa từng thấy SYNCING: refreshing false; event NOT_STARTED 'Không thể đồng bộ lúc này, thử lại sau'.
     */
    @Test
    public void onRefreshTimeout_whenNeverSawSyncing_emitsNotStarted() {
        viewModel.requestRefresh();
        viewModel.onRefreshTimeout();

        assertFalse(viewModel.getRefreshing().getValue());
        SyncRefreshEvent event = viewModel.getEvent().getValue();
        assertEquals(SyncRefreshEvent.Type.NOT_STARTED, event.type);
        assertEquals("Không thể đồng bộ lúc này, thử lại sau", event.message);
    }

    /**
     * TN5-U07
     * requestRefresh() hai lần khi đang refreshing: requestSyncNowCalls = 1.
     */
    @Test
    public void requestRefresh_whenAlreadyRefreshing_requestsSyncOnlyOnce() {
        viewModel.requestRefresh();
        viewModel.requestRefresh();

        assertEquals(1, fakeSyncRepository.requestSyncNowCalls);
    }
}