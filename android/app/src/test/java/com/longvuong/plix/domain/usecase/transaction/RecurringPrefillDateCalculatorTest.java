package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

public class RecurringPrefillDateCalculatorTest {
    private RecurringPrefillDateCalculator calculator;

    @Before
    public void setUp() {
        calculator = new RecurringPrefillDateCalculator();
    }

    /**
     * TN6-U22
     * today 2026-11-08, D = 5: 05/11 không sau 08/11 nên sang tháng sau, kết quả 2026-12-05.
     */
    @Test
    public void calculate_dayAlreadyPassedThisMonth_returnsSameDayNextMonth() {
        assertEquals(LocalDate.of(2026, 12, 5), calculator.calculate(5, LocalDate.of(2026, 11, 8)));
    }

    /**
     * TN6-U23
     * today 2026-11-08, D = 8: 08/11 không sau 08/11 (bằng hôm nay) nên sang tháng sau, kết quả 2026-12-08.
     */
    @Test
    public void calculate_dayIsToday_returnsSameDayNextMonth() {
        assertEquals(LocalDate.of(2026, 12, 8), calculator.calculate(8, LocalDate.of(2026, 11, 8)));
    }

    /**
     * TN6-U24
     * today 2026-11-08, D = 31: tháng 11 có 30 ngày nên kẹp thành 30/11, sau 08/11, kết quả 2026-11-30.
     */
    @Test
    public void calculate_day31InShortMonth_clampsToLastDayOfMonth() {
        assertEquals(LocalDate.of(2026, 11, 30), calculator.calculate(31, LocalDate.of(2026, 11, 8)));
    }

    /**
     * TN6-U25
     * today 2026-12-10, D = 5: lần kế tiếp là 05/01/2027 sang năm khác nên dùng hôm nay, kết quả 2026-12-10.
     */
    @Test
    public void calculate_nextDueDateInNextYear_returnsToday() {
        assertEquals(LocalDate.of(2026, 12, 10), calculator.calculate(5, LocalDate.of(2026, 12, 10)));
    }

    /**
     * TN6-U26
     * today 2026-12-10, D = 20: 20/12 sau 10/12 và cùng năm, kết quả 2026-12-20.
     */
    @Test
    public void calculate_dayLaterThisMonthInSameYear_returnsThisMonth() {
        assertEquals(LocalDate.of(2026, 12, 20), calculator.calculate(20, LocalDate.of(2026, 12, 10)));
    }
}