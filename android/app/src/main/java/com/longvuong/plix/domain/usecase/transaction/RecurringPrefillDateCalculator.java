package com.longvuong.plix.domain.usecase.transaction;

import java.time.LocalDate;
import java.time.YearMonth;

import javax.inject.Inject;

//Ngày giao dịch điền sẵn khi người dùng chấp nhận gợi ý định kỳ (FIN-18). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 7.6: ngày đến hạn kế tiếp nghiêm ngặt sau today (kẹp theo độ dài tháng); nếu sang năm khác thì dùng today
public class RecurringPrefillDateCalculator {

    @Inject
    public RecurringPrefillDateCalculator() {
    }

    public LocalDate calculate(int dayOfMonth, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate dueDate = clampToMonth(currentMonth, dayOfMonth);
        if (!dueDate.isAfter(today)) {
            dueDate = clampToMonth(currentMonth.plusMonths(1), dayOfMonth); //ngày D tháng này đã tới hoặc đã qua nên lấy tháng sau
        }
        if (dueDate.getYear() != today.getYear()) {
            return today; //FormValidator.validateOccurredAt chỉ cho phép chọn ngày tới hết năm hiện tại
        }
        return dueDate;
    }

    //Ngày D của tháng, kẹp theo số ngày của tháng (ngày 31 ở tháng 30 ngày thành ngày 30)
    private static LocalDate clampToMonth(YearMonth month, int dayOfMonth) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }
}