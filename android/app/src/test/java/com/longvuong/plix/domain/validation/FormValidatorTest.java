package com.longvuong.plix.domain.validation;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void validatePeriod_correctFormat_returnsSuccess() {
        assertTrue(formValidator.validatePeriod("2026-09") instanceof Result.Success);
    }

    @Test
    public void validatePeriod_invalidMonth_returnsError() {
        assertTrue(formValidator.validatePeriod("2026-13") instanceof Result.Error);
    }

    @Test
    public void validatePeriod_wrongOrderFormat_returnsError() {
        assertTrue(formValidator.validatePeriod("09-2026") instanceof Result.Error);
    }

    @Test
    public void validatePeriod_null_returnsError() {
        assertTrue(formValidator.validatePeriod(null) instanceof Result.Error);
    }

    @Test
    public void validateGoalCurrentAmount_negative_returnsError() {
        assertTrue(formValidator.validateGoalCurrentAmount(-1000) instanceof Result.Error);
    }

    @Test
    public void validateGoalCurrentAmount_zero_returnsSuccess() {
        assertTrue(formValidator.validateGoalCurrentAmount(0) instanceof Result.Success);
    }

    @Test
    public void validateGoalCurrentAmount_overMaxLimit_returnsError() {
        assertTrue(formValidator.validateGoalCurrentAmount(FormValidator.MAX_AMOUNT + 1) instanceof Result.Error);
    }

    @Test
    public void validateGoalCurrentAmount_validValue_returnsSuccess() {
        assertTrue(formValidator.validateGoalCurrentAmount(3_000_000) instanceof Result.Success);
    }

    @Test
    public void validateGoalName_empty_returnsError() {
        assertTrue(formValidator.validateGoalName("") instanceof Result.Error);
    }

    @Test
    public void validateGoalName_blank_returnsError() {
        assertTrue(formValidator.validateGoalName("   ") instanceof Result.Error);
    }

    @Test
    public void validateGoalName_null_returnsError() {
        assertTrue(formValidator.validateGoalName(null) instanceof Result.Error);
    }

    @Test
    public void validateGoalName_validValue_returnsSuccess() {
        assertTrue(formValidator.validateGoalName("Mua xe máy") instanceof Result.Success);
    }

    @Test
    public void validateGoalName_exceedsMaxLength_returnsError() {
        String longName = "a".repeat(FormValidator.MAX_GOAL_NAME_LENGTH + 1);
        assertTrue(formValidator.validateGoalName(longName) instanceof Result.Error);
    }

    @Test
    public void validateGoalName_exactlyMaxLength_returnsSuccess() {
        String name = "a".repeat(FormValidator.MAX_GOAL_NAME_LENGTH);
        assertTrue(formValidator.validateGoalName(name) instanceof Result.Success);
    }

    @Test
    public void validateGoalDeadline_pastDate_returnsError() {
        long yesterdayEpochMs = ZonedDateTime.now(ZoneId.systemDefault())
                .minusDays(1).toInstant().toEpochMilli();
        assertTrue(formValidator.validateGoalDeadline(yesterdayEpochMs) instanceof Result.Error);
    }

    @Test
    public void validateGoalDeadline_today_returnsSuccess() {
        long todayEndOfDayEpochMs = ZonedDateTime.now(ZoneId.systemDefault())
                .withHour(23).withMinute(59).withSecond(59).toInstant().toEpochMilli();
        assertTrue(formValidator.validateGoalDeadline(todayEndOfDayEpochMs) instanceof Result.Success);
    }

    @Test
    public void validateGoalDeadline_futureDate_returnsSuccess() {
        long nextMonthEpochMs = ZonedDateTime.now(ZoneId.systemDefault())
                .plusMonths(1).toInstant().toEpochMilli();
        assertTrue(formValidator.validateGoalDeadline(nextMonthEpochMs) instanceof Result.Success);
    }

    /**
     * TN1-U03
     * validateNewPassword('', ''): Result.Error với message 'Vui lòng nhập mật khẩu mới'.
     */
    @Test
    public void validateNewPassword_empty_returnsEnterPasswordError() {
        Result<Void> result = formValidator.validateNewPassword("", "");

        assertTrue(result instanceof Result.Error);
        assertEquals("Vui lòng nhập mật khẩu mới", ((Result.Error<Void>) result).message);
    }

    /**
     * TN1-U04
     * validateNewPassword('Abc12345', 'Abc12346'): Result.Error với message 'Mật khẩu nhập lại không khớp'.
     */
    @Test
    public void validateNewPassword_confirmMismatch_returnsMismatchError() {
        Result<Void> result = formValidator.validateNewPassword("Abc12345", "Abc12346");

        assertTrue(result instanceof Result.Error);
        assertEquals("Mật khẩu nhập lại không khớp", ((Result.Error<Void>) result).message);
    }

    /**
     * TN1-U05
     * validateNewPassword('Abc12345', 'Abc12345'): Result.Success (độ mạnh do Supabase quyết định).
     */
    @Test
    public void validateNewPassword_matchingValues_returnsSuccess() {
        assertTrue(formValidator.validateNewPassword("Abc12345", "Abc12345") instanceof Result.Success);
    }

    /**
     * TN1-U06
     * validateRecoveryEmail('') và ('   '): Error 'Vui lòng nhập email'; (' a@b.c '): Success vì đã cắt khoảng trắng.
     */
    @Test
    public void validateRecoveryEmail_blankAndPadded_rejectsBlankAndAcceptsTrimmed() {
        Result<Void> empty = formValidator.validateRecoveryEmail("");
        Result<Void> spaces = formValidator.validateRecoveryEmail("   ");
        Result<Void> padded = formValidator.validateRecoveryEmail(" a@b.c ");

        assertTrue(empty instanceof Result.Error);
        assertEquals("Vui lòng nhập email", ((Result.Error<Void>) empty).message);
        assertTrue(spaces instanceof Result.Error);
        assertEquals("Vui lòng nhập email", ((Result.Error<Void>) spaces).message);
        assertTrue(padded instanceof Result.Success);
    }

    /**
     * TN1-U07
     * validateRecoveryCode('12345'), ('12345a'), ('1234567'): Error 'Mã gồm 6 chữ số'; ('123456'): Success.
     */
    @Test
    public void validateRecoveryCode_variousInputs_acceptsOnlySixDigits() {
        Result<Void> tooShort = formValidator.validateRecoveryCode("12345");
        Result<Void> valid = formValidator.validateRecoveryCode("123456");
        Result<Void> hasLetter = formValidator.validateRecoveryCode("12345a");
        Result<Void> tooLong = formValidator.validateRecoveryCode("1234567");

        assertTrue(tooShort instanceof Result.Error);
        assertEquals("Mã gồm 6 chữ số", ((Result.Error<Void>) tooShort).message);
        assertTrue(valid instanceof Result.Success);
        assertTrue(hasLetter instanceof Result.Error);
        assertEquals("Mã gồm 6 chữ số", ((Result.Error<Void>) hasLetter).message);
        assertTrue(tooLong instanceof Result.Error);
        assertEquals("Mã gồm 6 chữ số", ((Result.Error<Void>) tooLong).message);
    }
}