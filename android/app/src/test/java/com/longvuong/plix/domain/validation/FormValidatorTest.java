package com.longvuong.plix.domain.validation;

import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class FormValidatorTest {

    private final FormValidator formValidator = new FormValidator();

    @Test
    public void validateAmount_zero_returnsError() {
        assertTrue(formValidator.validateAmount(0) instanceof Result.Error);
    }

    @Test
    public void validateAmount_negative_returnsError() {
        assertTrue(formValidator.validateAmount(-5000) instanceof Result.Error);
    }

    @Test
    public void validateAmount_overMaxLimit_returnsError() {
        assertTrue(formValidator.validateAmount(FormValidator.MAX_AMOUNT + 1) instanceof Result.Error);
    }

    @Test
    public void validateAmount_validValue_returnsSuccess() {
        assertTrue(formValidator.validateAmount(50000) instanceof Result.Success);
    }

    @Test
    public void validateAmount_exactlyMaxLimit_returnsSuccess() {
        assertTrue(formValidator.validateAmount(FormValidator.MAX_AMOUNT) instanceof Result.Success);
    }

    @Test
    public void validateNote_exceeds500Characters_returnsError() {
        String longNote = "a".repeat(501);
        assertTrue(formValidator.validateNote(longNote) instanceof Result.Error);
    }

    @Test
    public void validateNote_exactly500Characters_returnsSuccess() {
        String note = "a".repeat(500);
        assertTrue(formValidator.validateNote(note) instanceof Result.Success);
    }

    @Test
    public void validateNote_null_returnsSuccess() {
        assertTrue(formValidator.validateNote(null) instanceof Result.Success);
    }

    @Test
    public void validateOccurredAt_tooFarInFuture_returnsError() {
        long nextYearEpochMs = ZonedDateTime.now(ZoneId.systemDefault())
                .plusYears(1).toInstant().toEpochMilli();
        assertTrue(formValidator.validateOccurredAt(nextYearEpochMs) instanceof Result.Error);
    }

    @Test
    public void validateOccurredAt_tooFarInPast_returnsError() {
        long sixYearsAgoEpochMs = ZonedDateTime.now(ZoneId.systemDefault())
                .minusYears(6).toInstant().toEpochMilli();
        assertTrue(formValidator.validateOccurredAt(sixYearsAgoEpochMs) instanceof Result.Error);
    }

    @Test
    public void validateOccurredAt_today_returnsSuccess() {
        long now = System.currentTimeMillis();
        assertTrue(formValidator.validateOccurredAt(now) instanceof Result.Success);
    }
}