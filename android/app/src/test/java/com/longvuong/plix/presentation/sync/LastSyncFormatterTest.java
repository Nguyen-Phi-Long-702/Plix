package com.longvuong.plix.presentation.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.ZoneId;

public class LastSyncFormatterTest {
    private static final long SYNC_AT_MILLIS = 1794121500000L; //07:05 08/11/2026 UTC = 14:05 08/11/2026 giờ Việt Nam

    /**
     * TN5-U01
     * format(1794121500000L, Asia/Ho_Chi_Minh) và format(1794121500000L, UTC): '14:05 08/11/2026' và '07:05 08/11/2026'.
     */
    @Test
    public void format_usesGivenTimeZone() {
        assertEquals("14:05 08/11/2026", LastSyncFormatter.format(SYNC_AT_MILLIS, ZoneId.of("Asia/Ho_Chi_Minh")));
        assertEquals("07:05 08/11/2026", LastSyncFormatter.format(SYNC_AT_MILLIS, ZoneId.of("UTC")));
    }

    /**
     * TN5-U02
     * format(null, zone): 'chưa có'.
     */
    @Test
    public void format_whenNull_returnsChuaCo() {
        assertEquals("chưa có", LastSyncFormatter.format(null, ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}