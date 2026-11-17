package com.longvuong.plix.presentation.sync;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.repository.SyncRepository;
import com.longvuong.plix.data.sync.SyncStatus;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SyncRefreshViewModel extends ViewModel {
    private static final String MESSAGE_SUCCESS = "Đã đồng bộ";
    private static final String MESSAGE_ERROR_DEFAULT = "Đồng bộ thất bại";
    private static final String MESSAGE_OFFLINE = "Không có mạng, dữ liệu sẽ đồng bộ khi có mạng";
    private static final String MESSAGE_NOT_STARTED = "Không thể đồng bộ lúc này, thử lại sau";

    private final SyncRepository syncRepository;
    //true = đang quay vòng làm mới; Fragment quan sát để bật/tắt SwipeRefreshLayout
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    //sự kiện một lần cho Snackbar; null = không có sự kiện đang chờ. Fragment gọi onEventHandled() sau khi hiện để xoay màn hình không phát lại
    private final MutableLiveData<SyncRefreshEvent> event = new MutableLiveData<>();
    private final Observer<SyncStatus> syncStatusObserver = this::onSyncStatusChanged;
    //đã thấy SYNCING kể từ lượt kéo hiện tại; chưa thấy thì giá trị IDLE/ERROR có sẵn từ trước không được coi là kết quả của lượt này
    private boolean sawSyncing;

    @Inject
    public SyncRefreshViewModel(SyncRepository syncRepository) {
        this.syncRepository = syncRepository;
        syncRepository.getSyncStatus().observeForever(syncStatusObserver);
    }

    public LiveData<Boolean> getRefreshing() {
        return refreshing;
    }

    public LiveData<SyncRefreshEvent> getEvent() {
        return event;
    }

    public void requestRefresh() {
        if (isRefreshing()) {
            return; //kéo liên tiếp chỉ tạo một lượt
        }
        syncRepository.requestSyncNow();
        if (!syncRepository.isOnline()) {
            //lượt vẫn được xếp, WorkManager sẽ chạy khi có mạng; không quay vòng để khỏi quay mãi
            event.setValue(new SyncRefreshEvent(SyncRefreshEvent.Type.OFFLINE, MESSAGE_OFFLINE));
            return;
        }
        sawSyncing = syncRepository.getSyncStatus().getValue() == SyncStatus.SYNCING;
        refreshing.setValue(true);
    }

    //Fragment gọi sau 8.000 ms kể từ lúc kéo
    public void onRefreshTimeout() {
        if (isRefreshing() && !sawSyncing) {
            refreshing.setValue(false);
            event.setValue(new SyncRefreshEvent(SyncRefreshEvent.Type.NOT_STARTED, MESSAGE_NOT_STARTED));
        }
        //đã bắt đầu đồng bộ thì chờ đến khi kết thúc
    }

    public void onEventHandled() {
        event.setValue(null);
    }

    private boolean isRefreshing() {
        return Boolean.TRUE.equals(refreshing.getValue());
    }

    private void onSyncStatusChanged(SyncStatus status) {
        if (status == SyncStatus.SYNCING) {
            sawSyncing = true;
            return;
        }
        if (status == null || !isRefreshing() || !sawSyncing) {
            return;
        }
        refreshing.setValue(false);
        if (status == SyncStatus.ERROR) {
            String lastError = syncRepository.getLastSyncError();
            event.setValue(new SyncRefreshEvent(SyncRefreshEvent.Type.ERROR, lastError != null ? lastError : MESSAGE_ERROR_DEFAULT));
        } else {
            event.setValue(new SyncRefreshEvent(SyncRefreshEvent.Type.SUCCESS, MESSAGE_SUCCESS));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        syncRepository.getSyncStatus().removeObserver(syncStatusObserver);
    }
}