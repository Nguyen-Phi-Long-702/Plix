package com.longvuong.plix.domain.usecase.reminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FindUpcomingRecurringDueUseCaseTest {
    private static final String USER_ID = "user-1";
    private static final int LEAD_DAYS = 1; //FIN-11 nhắc trước 1 ngày

    private final FindUpcomingRecurringDueUseCase useCase = new FindUpcomingRecurringDueUseCase();

    /**
     * TN3-U05
     * today 2026-11-08, mẫu MONTHLY:9, MONTHLY:8, MONTHLY:10: target = 09/11 nên chỉ mẫu MONTHLY:9 được trả về,
     * dueDate = 2026-11-09.
     */
    @Test
    public void execute_templateDueTomorrow_returnsOnlyThatTemplate() {
        TransactionEntity dueTomorrow = template("t-9", USER_ID, 100_000L, "MONTHLY:9");
        TransactionEntity dueToday = template("t-8", USER_ID, 100_000L, "MONTHLY:8");
        TransactionEntity dueInTwoDays = template("t-10", USER_ID, 100_000L, "MONTHLY:10");

        List<UpcomingRecurringDue> result = useCase.execute(
                Arrays.asList(dueTomorrow, dueToday, dueInTwoDays), USER_ID, LocalDate.of(2026, 11, 8), LEAD_DAYS);

        assertEquals(1, result.size());
        assertEquals("t-9", result.get(0).template.id);
        assertEquals(LocalDate.of(2026, 11, 9), result.get(0).dueDate);
    }

    /**
     * TN3-U06
     * today 2026-11-29, mẫu MONTHLY:31: target = 30/11, tháng 11 có 30 ngày, min(31, 30) = 30 nên trả về
     * với dueDate = 2026-11-30.
     */
    @Test
    public void execute_day31InThirtyDayMonth_returnsDueOnDay30() {
        TransactionEntity day31 = template("t-31", USER_ID, 100_000L, "MONTHLY:31");

        List<UpcomingRecurringDue> result = useCase.execute(
                Collections.singletonList(day31), USER_ID, LocalDate.of(2026, 11, 29), LEAD_DAYS);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 11, 30), result.get(0).dueDate);
    }

    /**
     * TN3-U07
     * (a) today 2027-02-27 (tháng 2 không nhuận, 28 ngày), mẫu MONTHLY:29, :30, :31: cả ba trả về dueDate = 2027-02-28;
     * (b) today 2028-02-28 (tháng 2 nhuận, 29 ngày), mẫu MONTHLY:31: trả về dueDate = 2028-02-29.
     */
    @Test
    public void execute_februaryNonLeapAndLeap_clampsToLastDayOfMonth() {
        TransactionEntity day29 = template("t-29", USER_ID, 100_000L, "MONTHLY:29");
        TransactionEntity day30 = template("t-30", USER_ID, 100_000L, "MONTHLY:30");
        TransactionEntity day31 = template("t-31", USER_ID, 100_000L, "MONTHLY:31");

        List<UpcomingRecurringDue> nonLeap = useCase.execute(
                Arrays.asList(day29, day30, day31), USER_ID, LocalDate.of(2027, 2, 27), LEAD_DAYS);

        assertEquals(3, nonLeap.size());
        for (UpcomingRecurringDue due : nonLeap) {
            assertEquals(LocalDate.of(2027, 2, 28), due.dueDate);
        }

        List<UpcomingRecurringDue> leap = useCase.execute(
                Collections.singletonList(day31), USER_ID, LocalDate.of(2028, 2, 28), LEAD_DAYS);

        assertEquals(1, leap.size());
        assertEquals(LocalDate.of(2028, 2, 29), leap.get(0).dueDate);
    }

    /**
     * TN3-U08
     * today 2026-11-08 (target = 09/11): mẫu đã xoá, mẫu của user khác, giao dịch có recurrenceParentId, isRecurring = false,
     * quy tắc 'WEEKLY:3', quy tắc 'MONTHLY:0': mỗi mẫu bị loại bởi đúng một điều kiện nên không mục nào được trả về.
     */
    @Test
    public void execute_ineligibleTemplates_returnsEmpty() {
        TransactionEntity deleted = template("t-deleted", USER_ID, 100_000L, "MONTHLY:9");
        deleted.isDeleted = true;
        TransactionEntity otherUser = template("t-other-user", "user-2", 100_000L, "MONTHLY:9");
        TransactionEntity generated = template("t-generated", USER_ID, 100_000L, "MONTHLY:9");
        generated.recurrenceParentId = "t-parent";
        TransactionEntity notRecurring = template("t-not-recurring", USER_ID, 100_000L, "MONTHLY:9");
        notRecurring.isRecurring = false;
        TransactionEntity weekly = template("t-weekly", USER_ID, 100_000L, "WEEKLY:3");
        TransactionEntity dayZero = template("t-day-zero", USER_ID, 100_000L, "MONTHLY:0");

        List<UpcomingRecurringDue> result = useCase.execute(
                Arrays.asList(deleted, otherUser, generated, notRecurring, weekly, dayZero),
                USER_ID, LocalDate.of(2026, 11, 8), LEAD_DAYS);

        assertTrue(result.isEmpty());
    }

    //Mẫu định kỳ hợp lệ (chưa xoá, isRecurring, không có recurrenceParentId); từng test sửa đúng một trường để tạo ca cần thử
    private static TransactionEntity template(String id, String userId, long amount, String recurrenceRule) {
        TransactionEntity template = new TransactionEntity();
        template.id = id;
        template.userId = userId;
        template.amount = amount;
        template.type = "expense";
        template.isRecurring = true;
        template.recurrenceRule = recurrenceRule;
        template.recurrenceParentId = null;
        template.syncStatus = "synced";
        template.isDeleted = false;
        return template;
    }
}
