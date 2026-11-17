package com.longvuong.plix.presentation.sync;

//Sự kiện một lần của kéo để làm mới (SYNC-10): Fragment hiện thành Snackbar rồi gọi SyncRefreshViewModel.onEventHandled()
public final class SyncRefreshEvent {
    public enum Type {
        SUCCESS,
        ERROR,
        OFFLINE,
        NOT_STARTED
    }

    public final Type type;
    public final String message;

    public SyncRefreshEvent(Type type, String message) {
        this.type = type;
        this.message = message;
    }
}