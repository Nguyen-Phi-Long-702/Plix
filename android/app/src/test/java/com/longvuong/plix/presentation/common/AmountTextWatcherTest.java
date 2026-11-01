package com.longvuong.plix.presentation.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AmountTextWatcherTest {
    @Test
    public void parse_ignoresSeparatorsAndLetters() {
        assertEquals(1500000L, AmountTextWatcher.parse("1.500.000"));
        assertEquals(45000L, AmountTextWatcher.parse("45.000đ"));
    }

    @Test
    public void parse_emptyOrNoDigits_returnsZero() {
        assertEquals(0L, AmountTextWatcher.parse(""));
        assertEquals(0L, AmountTextWatcher.parse("abc"));
    }

    @Test
    public void parse_tooLargeForLong_returnsZero() {
        assertEquals(0L, AmountTextWatcher.parse("99999999999999999999"));
    }

    @Test
    public void format_usesDotAsThousandSeparator() {
        assertEquals("1.500.000", AmountTextWatcher.format(1500000L));
        assertEquals("999", AmountTextWatcher.format(999L));
    }
}