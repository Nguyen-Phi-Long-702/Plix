package com.longvuong.plix.presentation.transaction;

public class AnomalyBannerUiModel {
    public enum Kind {
        HIGH, //cao bất thường
        LOW, //thấp bất thường
        INSUFFICIENT_DATA //chưa đủ dữ liệu: thông tin tích cực, không phải cảnh báo hay lỗi
    }

    public final Kind kind;
    public final String title; //rỗng với INSUFFICIENT_DATA (banner này không có tiêu đề)
    public final String explanation;
    public final boolean showPendingNote;

    public AnomalyBannerUiModel(Kind kind, String title, String explanation, boolean showPendingNote) {
        this.kind = kind;
        this.title = title;
        this.explanation = explanation;
        this.showPendingNote = showPendingNote;
    }
}