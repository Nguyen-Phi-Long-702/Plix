package com.longvuong.plix.domain.usecase.reminder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ComputeNextTriggerUseCaseTest {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ComputeNextTriggerUseCase useCase = new ComputeNextTriggerUseCase();

    /**
     * TN3-U01
     * now 2026-11-08T19:00 Asia/Ho_Chi_Minh, target 20:00: 3_600_000 ms.
     */
    @Test
    public void execute_nowBeforeTarget_returnsDelayToTargetSameDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 8, 19, 0, 0, 0, VIETNAM);

        assertEquals(3_600_000L, useCase.execute(now, LocalTime.of(20, 0)));
    }

    /**
     * TN3-U02
     * now 2026-11-08T20:00:00, target 20:00: 86_400_000 ms (đúng giờ đặt thì tính sang ngày mai).
     */
    @Test
    public void execute_nowEqualsTarget_returnsDelayToTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 8, 20, 0, 0, 0, VIETNAM);

        assertEquals(86_400_000L, useCase.execute(now, LocalTime.of(20, 0)));
    }

    /**
     * TN3-U03
     * now 2026-11-08T21:30, target 20:00: 81_000_000 ms (22,5 giờ).
     */
    @Test
    public void execute_nowAfterTarget_returnsDelayToTargetNextDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 8, 21, 30, 0, 0, VIETNAM);

        assertEquals(81_000_000L, useCase.execute(now, LocalTime.of(20, 0)));
    }

    /**
     * TN3-U04
     * now 2026-03-07T21:00 America/New_York, target 09:00: 39_600_000 ms
     * (11 giờ thực vì 02:00 nhảy tới 03:00 ngày 08/03/2026).
     */
    @Test
    public void execute_daylightSavingStartsBeforeTarget_returnsRealElapsedTime() {
        ZonedDateTime now = ZonedDateTime.of(2026, 3, 7, 21, 0, 0, 0, ZoneId.of("America/New_York"));

        assertEquals(39_600_000L, useCase.execute(now, LocalTime.of(9, 0)));
    }
}
