package com.longvuong.plix.domain.usecase.reminder;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

//Chọn các mẫu giao dịch định kỳ đến hạn sau `leadDays` ngày kể từ hôm nay (FIN-11). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 3.4: lọc mẫu hợp lệ, đặt target = today + leadDays, mẫu đến hạn khi ngày của target bằng ngày lặp đã kẹp theo độ dài tháng
public class FindUpcomingRecurringDueUseCase {
    private static final String RECURRENCE_PREFIX = "MONTHLY:"; //cùng quy tắc với RecurringTransactionWorker
    private static final int MIN_DAY_OF_MONTH = 1;
    private static final int MAX_DAY_OF_MONTH = 31;

    //Thứ tự kết quả: ngày đến hạn tăng dần, rồi số tiền giảm dần
    private static final Comparator<UpcomingRecurringDue> DUE_ORDER = (a, b) -> {
        int byDate = a.dueDate.compareTo(b.dueDate);
        if (byDate != 0) {
            return byDate;
        }
        return Long.compare(b.template.amount, a.template.amount);
    };

    @Inject
    public FindUpcomingRecurringDueUseCase() {
    }

    public List<UpcomingRecurringDue> execute(List<TransactionEntity> templates, String userId, LocalDate today, int leadDays) {
        //Cộng ngày trên LocalDate nên đúng cả khi qua tháng (ví dụ 29/11 + 1 = 30/11, 27/02 + 1 = 28/02)
        LocalDate target = today.plusDays(leadDays);
        List<UpcomingRecurringDue> result = new ArrayList<>();
        for (TransactionEntity template : templates) {
            if (!isEligibleTemplate(template, userId)) {
                continue;
            }
            int dayOfMonth = parseDayOfMonth(template.recurrenceRule);
            if (dayOfMonth < 0) {
                continue; //quy tắc không đúng dạng MONTHLY:<1..31>
            }
            //Ngày lặp lớn hơn số ngày của tháng thì kẹp về ngày cuối tháng (ngày 31 trong tháng 30 ngày, ngày 29..31 trong tháng 2)
            if (target.getDayOfMonth() == Math.min(dayOfMonth, target.lengthOfMonth())) {
                result.add(new UpcomingRecurringDue(template, target));
            }
        }
        result.sort(DUE_ORDER);
        return result;
    }

    //Chỉ lấy mẫu của người dùng hiện tại, chưa xoá, là mẫu định kỳ gốc (không phải giao dịch do Worker sinh ra)
    private static boolean isEligibleTemplate(TransactionEntity template, String userId) {
        return !template.isDeleted
                && template.isRecurring
                && template.recurrenceParentId == null
                && Objects.equals(userId, template.userId);
    }

    private static int parseDayOfMonth(String recurrenceRule) {
        if (recurrenceRule == null || !recurrenceRule.startsWith(RECURRENCE_PREFIX)) {
            return -1;
        }
        try {
            int day = Integer.parseInt(recurrenceRule.substring(RECURRENCE_PREFIX.length()));
            return (day >= MIN_DAY_OF_MONTH && day <= MAX_DAY_OF_MONTH) ? day : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
