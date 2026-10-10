package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;

public class AnomalyResult {
    public enum Level {
        NORMAL,
        HIGH,
        LOW,
        INSUFFICIENT_DATA
    }

    public final Level level;

    @Nullable
    public final String explanation; //giải thích do máy chủ trả về

    public AnomalyResult(Level level, @Nullable String explanation) {
        this.level = level;
        this.explanation = explanation;
    }
}