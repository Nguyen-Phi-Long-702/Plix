package com.longvuong.plix.presentation.transaction;

public class AnomalyBannerUiModel {
    public final boolean high; //true = cao bất thường, false = thấp bất thường
    public final String title;
    public final String explanation;
    public final boolean showPendingNote;

    public AnomalyBannerUiModel(boolean high, String title, String explanation, boolean showPendingNote) {
        this.high = high;
        this.title = title;
        this.explanation = explanation;
        this.showPendingNote = showPendingNote;
    }
}