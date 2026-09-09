package com.longvuong.plix.domain.validation;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FormValidator {

    public static final long MAX_AMOUNT = 999_999_999_999L; //999 tỷ vnd, chặn tràn số
    public static final int MAX_NOTE_LENGTH = 500;
    private static final int MAX_PAST_YEARS = 5;

    @Inject
    public FormValidator() {
    }

    public Result<Void> validateAmount(long amount) {
        if (amount <= 0) {
            return new Result.Error<>(ErrorType.VALIDATION, "Số tiền phải lớn hơn 0", null);
        }
        if (amount > MAX_AMOUNT) {
            return new Result.Error<>(ErrorType.VALIDATION, "Số tiền vượt quá giới hạn cho phép", null);
        }
        return new Result.Success<>(null);
    }

    public Result<Void> validateNote(String note) {
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            return new Result.Error<>(ErrorType.VALIDATION,
                    "Ghi chú không được vượt quá " + MAX_NOTE_LENGTH + " ký tự", null);
        }
        return new Result.Success<>(null);
    }

    public Result<Void> validateOccurredAt(long occurredAtEpochMs) {
        ZoneId zone = ZoneId.systemDefault();

        long maxFutureEpochMs = Year.now(zone).atMonth(12).atEndOfMonth()
                .atTime(23, 59, 59)
                .atZone(zone)
                .toInstant()
                .toEpochMilli();

        long minPastEpochMs = LocalDate.now(zone).minusYears(MAX_PAST_YEARS)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli();

        if (occurredAtEpochMs > maxFutureEpochMs) {
            return new Result.Error<>(ErrorType.VALIDATION,
                    "Ngày giao dịch không được chọn quá xa trong tương lai", null);
        }
        if (occurredAtEpochMs < minPastEpochMs) {
            return new Result.Error<>(ErrorType.VALIDATION,
                    "Ngày giao dịch không được chọn quá xa trong quá khứ", null);
        }
        return new Result.Success<>(null);
    }

    public Result<Void> validateTransaction(long amount, String note, long occurredAtEpochMs) {
        Result<Void> amountResult = validateAmount(amount);
        if (amountResult instanceof Result.Error) {
            return amountResult;
        }
        Result<Void> noteResult = validateNote(note);
        if (noteResult instanceof Result.Error) {
            return noteResult;
        }
        return validateOccurredAt(occurredAtEpochMs);
    }
}