package com.longvuong.plix.presentation.sync;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//Định dạng thời điểm "Đồng bộ lần cuối" (SYNC-12): 'HH:mm dd/MM/yyyy' theo múi giờ truyền vào; chưa có thì trả 'chưa có'. Java thuần, test được trên JVM
public final class LastSyncFormatter {
    private static final String NONE_TEXT = "chưa có";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private LastSyncFormatter() {
    }

    public static String format(@Nullable Long lastSuccessAt, ZoneId zone) {
        if (lastSuccessAt == null || lastSuccessAt <= 0L) {
            return NONE_TEXT;
        }
        return FORMATTER.format(Instant.ofEpochMilli(lastSuccessAt).atZone(zone));
    }
}