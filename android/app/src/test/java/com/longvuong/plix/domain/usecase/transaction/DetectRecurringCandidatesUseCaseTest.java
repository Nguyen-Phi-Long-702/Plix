package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectRecurringCandidatesUseCaseTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final String CAT_ENT = "cat-ent";
    private static final String CAT_RENT = "cat-rent";
    private static final String CAT_SALARY = "cat-salary";

    private DetectRecurringCandidatesUseCase useCase;

    @Before
    public void setUp() {
        useCase = new DetectRecurringCandidatesUseCase();
    }

    //Giao dịch chi của user-A, danh mục cat-ent, năm 2026, giữa trưa theo ZONE; mặc định không xoá, không phải mẫu, không do Worker sinh
    private static TransactionEntity transaction(String id, int month, int day, long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = USER_A;
        entity.type = "expense";
        entity.categoryId = CAT_ENT;
        entity.amount = amount;
        entity.occurredAt = LocalDate.of(2026, month, day).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli();
        return entity;
    }

    //BD1: 260.000 vào 05/08, 05/09, 05/10/2026
    private static List<TransactionEntity> dataBd1() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 260_000L));
        all.add(transaction("t-09", 9, 5, 260_000L));
        all.add(transaction("t-10", 10, 5, 260_000L));
        return all;
    }

    //Gọi use case với user-A, không có mẫu, không có khoá bỏ qua
    private List<RecurringCandidate> detect(List<TransactionEntity> all) {
        return useCase.execute(all, Collections.<TransactionEntity>emptyList(), Collections.<String>emptySet(),
                USER_A, TODAY, ZONE);
    }

    private static void assertSingleCandidate(List<RecurringCandidate> result, long amount, int dayOfMonth,
                                              int consecutiveMonths) {
        assertSingleCandidate(result, "expense", CAT_ENT, amount, dayOfMonth, consecutiveMonths);
    }

    private static void assertSingleCandidate(List<RecurringCandidate> result, String type, String categoryId,
                                              long amount, int dayOfMonth, int consecutiveMonths) {
        assertEquals(1, result.size());
        RecurringCandidate candidate = result.get(0);
        assertEquals(type, candidate.type);
        assertEquals(categoryId, candidate.categoryId);
        assertEquals(amount, candidate.amount);
        assertEquals(dayOfMonth, candidate.dayOfMonth);
        assertEquals(consecutiveMonths, candidate.consecutiveMonths);
    }

    private List<RecurringCandidate> detect(List<TransactionEntity> all, LocalDate today, ZoneId zone) {
        return useCase.execute(all, Collections.<TransactionEntity>emptyList(), Collections.<String>emptySet(),
                USER_A, today, zone);
    }

    private static long noonOf(int year, int month, int day) {
        return LocalDate.of(year, month, day).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static TransactionEntity transactionAt(String id, String type, String categoryId, long occurredAt,
                                                   long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = USER_A;
        entity.type = type;
        entity.categoryId = categoryId;
        entity.amount = amount;
        entity.occurredAt = occurredAt;
        return entity;
    }

    private static List<TransactionEntity> dataBd14() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transactionAt("t-07", "expense", CAT_ENT, Instant.parse("2026-07-31T17:30:00Z").toEpochMilli(), 90_000L));
        all.add(transactionAt("t-08", "expense", CAT_ENT, Instant.parse("2026-08-31T17:30:00Z").toEpochMilli(), 90_000L));
        all.add(transactionAt("t-09", "expense", CAT_ENT, Instant.parse("2026-09-30T17:30:00Z").toEpochMilli(), 90_000L));
        return all;
    }

    private static List<TransactionEntity> dataBd19() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transactionAt("t-09", "expense", CAT_ENT, noonOf(2026, 9, 5), 100_000L));
        all.add(transactionAt("t-10", "expense", CAT_ENT, noonOf(2026, 10, 5), 100_000L));
        all.add(transactionAt("t-11", "expense", CAT_ENT, noonOf(2026, 11, 5), 100_000L));
        return all;
    }

    /**
     * TN6-U01
     * BD1: cat-ent 260.000 vào 05/08, 05/09, 05/10/2026 (today 2026-11-08): 1 ứng viên {expense, cat-ent, 260.000, ngày 5, 3 tháng}.
     */
    @Test
    public void execute_threeConsecutiveMonthsSameAmountAndDay_returnsOneCandidate() {
        assertSingleCandidate(detect(dataBd1()), 260_000L, 5, 3);
    }

    /**
     * TN6-U02
     * BD2: chỉ 05/09 và 05/10, 260.000: chuỗi 2 tháng nhỏ hơn 3, không có ứng viên.
     */
    @Test
    public void execute_onlyTwoMonths_returnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-09", 9, 5, 260_000L));
        all.add(transaction("t-10", 10, 5, 260_000L));

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U03
     * BD3: ngày 5, tháng 08 = 200.000, 09 = 200.000, 10 = 230.000: |200.000 - 230.000| x 100 = 3.000.000 > 230.000 x 10 = 2.300.000, không có ứng viên.
     */
    @Test
    public void execute_amountDiffersMoreThanTenPercent_returnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 200_000L));
        all.add(transaction("t-09", 9, 5, 200_000L));
        all.add(transaction("t-10", 10, 5, 230_000L));

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U04
     * BD4: ngày 5, tháng 08 = 210.000, 09 = 220.000, 10 = 230.000: so với 230.000, lệch 20.000 x 100 = 2.000.000 <= 2.300.000; 1 ứng viên amount 230.000, 3 tháng.
     */
    @Test
    public void execute_amountWithinTenPercentOfLatest_returnsCandidateWithLatestAmount() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 210_000L));
        all.add(transaction("t-09", 9, 5, 220_000L));
        all.add(transaction("t-10", 10, 5, 230_000L));

        assertSingleCandidate(detect(all), 230_000L, 5, 3);
    }

    /**
     * TN6-U05
     * BD5: cat-ent 100.000 vào 05/08, 08/09, 05/10: |8 - 5| = 3 <= 3; 1 ứng viên ngày 5, 3 tháng.
     */
    @Test
    public void execute_dayDiffersByThree_returnsCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 100_000L));
        all.add(transaction("t-09", 9, 8, 100_000L));
        all.add(transaction("t-10", 10, 5, 100_000L));

        assertSingleCandidate(detect(all), 100_000L, 5, 3);
    }

    /**
     * TN6-U06
     * BD6: cat-ent 100.000 vào 05/08, 09/09, 05/10: |9 - 5| = 4 > 3, không có ứng viên.
     */
    @Test
    public void execute_dayDiffersByFour_returnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 100_000L));
        all.add(transaction("t-09", 9, 9, 100_000L));
        all.add(transaction("t-10", 10, 5, 100_000L));

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U07
     * BD7: cat-ent 100.000 ngày 5 chỉ có tháng 08 và 10: thiếu tháng 09 nên chuỗi dừng ở 1, không có ứng viên.
     */
    @Test
    public void execute_missingMiddleMonth_returnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 100_000L));
        all.add(transaction("t-10", 10, 5, 100_000L));

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U08
     * BD8: cat-ent 100.000 vào 05/08, 05/09, 20/09, 05/10: tháng 09 có 2 giao dịch (không đơn) nên chuỗi từ neo chỉ còn tháng 10, không có ứng viên.
     */
    @Test
    public void execute_monthWithTwoTransactions_breaksChainAndReturnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 100_000L));
        all.add(transaction("t-09a", 9, 5, 100_000L));
        all.add(transaction("t-09b", 9, 20, 100_000L));
        all.add(transaction("t-10", 10, 5, 100_000L));

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U09
     * BD9: 3 giao dịch ngày 5 tháng 08, 09, 10 đều có recurrenceParentId = 'tpl-1' (do Worker sinh): bị loại, không có ứng viên.
     */
    @Test
    public void execute_transactionsGeneratedByWorker_returnsNoCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 100_000L));
        all.add(transaction("t-09", 9, 5, 100_000L));
        all.add(transaction("t-10", 10, 5, 100_000L));
        for (TransactionEntity entity : all) {
            entity.recurrenceParentId = "tpl-1";
        }

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U10
     * BD10: dữ liệu BD1 cộng 1 mẫu định kỳ (isRecurring = true, recurrenceParentId = null, chưa xoá, expense, cat-ent): nhóm bị loại, không có ứng viên.
     */
    @Test
    public void execute_groupAlreadyHasActiveTemplate_returnsNoCandidate() {
        TransactionEntity template = transaction("tpl-1", 10, 5, 260_000L);
        template.isRecurring = true;
        List<TransactionEntity> templates = new ArrayList<>();
        templates.add(template);

        List<RecurringCandidate> result = useCase.execute(dataBd1(), templates, Collections.<String>emptySet(),
                USER_A, TODAY, ZONE);

        assertEquals(0, result.size());
    }

    /**
     * TN6-U11
     * BD11: dữ liệu BD1 nhưng giao dịch tháng 09 isDeleted = true: bị loại, chuỗi chỉ còn 1 tháng, không có ứng viên.
     */
    @Test
    public void execute_deletedTransactionInChain_returnsNoCandidate() {
        List<TransactionEntity> all = dataBd1();
        all.get(1).isDeleted = true;

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U12
     * BD12: dữ liệu BD1 nhưng userId = 'user-B', gọi với 'user-A': bị loại hết, không có ứng viên.
     */
    @Test
    public void execute_transactionsOfAnotherUser_returnsNoCandidate() {
        List<TransactionEntity> all = dataBd1();
        for (TransactionEntity entity : all) {
            entity.userId = USER_B;
        }

        assertEquals(0, detect(all).size());
    }
    /**
     * TN6-U13
     * BD13: today 2027-04-10; cat-rent 5.000.000 vào 31/01, 28/02, 31/03/2027: tháng 2 có hiệu lực ngày min(31, 28) = 28, lệch 0; 1 ứng viên ngày 31, 3 tháng.
     */
    @Test
    public void execute_dayThirtyOneClampedToShortMonth_returnsCandidateOnDayThirtyOne() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transactionAt("t-01", "expense", CAT_RENT, noonOf(2027, 1, 31), 5_000_000L));
        all.add(transactionAt("t-02", "expense", CAT_RENT, noonOf(2027, 2, 28), 5_000_000L));
        all.add(transactionAt("t-03", "expense", CAT_RENT, noonOf(2027, 3, 31), 5_000_000L));

        assertSingleCandidate(detect(all, LocalDate.of(2027, 4, 10), ZONE), "expense", CAT_RENT, 5_000_000L, 31, 3);
    }

    /**
     * TN6-U14
     * BD14: cat-ent 90.000 tại 2026-07-31T17:30Z, 2026-08-31T17:30Z, 2026-09-30T17:30Z; zone Asia/Ho_Chi_Minh: giờ địa phương là 01/08, 01/09, 01/10; 1 ứng viên ngày 1, 3 tháng.
     */
    @Test
    public void execute_utcPlusSevenTimestamps_usesLocalDatesAndReturnsCandidateOnDayOne() {
        assertSingleCandidate(detect(dataBd14(), TODAY, ZONE), 90_000L, 1, 3);
    }

    /**
     * TN6-U15
     * BD15: như BD14 nhưng zone UTC: tháng 07, 08, 09; tháng gần nhất là 09, không phải tháng 10 hay 11 nên không có neo, không có ứng viên.
     */
    @Test
    public void execute_sameTimestampsInUtc_returnsNoCandidate() {
        assertEquals(0, detect(dataBd14(), TODAY, ZoneId.of("UTC")).size());
    }

    /**
     * TN6-U16
     * BD16: income cat-salary 15.000.000 ngày 10 các tháng 08, 09, 10/2026: 1 ứng viên income, 15.000.000, ngày 10, 3 tháng.
     */
    @Test
    public void execute_incomeThreeConsecutiveMonths_returnsIncomeCandidate() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transactionAt("t-08", "income", CAT_SALARY, noonOf(2026, 8, 10), 15_000_000L));
        all.add(transactionAt("t-09", "income", CAT_SALARY, noonOf(2026, 9, 10), 15_000_000L));
        all.add(transactionAt("t-10", "income", CAT_SALARY, noonOf(2026, 10, 10), 15_000_000L));

        assertSingleCandidate(detect(all), "income", CAT_SALARY, 15_000_000L, 10, 3);
    }

    /**
     * TN6-U17
     * BD17: dữ liệu BD1 nhưng categoryId = null: bị loại ở bước 1, không có ứng viên.
     */
    @Test
    public void execute_transactionsWithoutCategory_returnsNoCandidate() {
        List<TransactionEntity> all = dataBd1();
        for (TransactionEntity entity : all) {
            entity.categoryId = null;
        }

        assertEquals(0, detect(all).size());
    }

    /**
     * TN6-U18
     * BD18: dữ liệu BD1 và dismissedKeys = {'expense|cat-ent'}: bị loại ở bước 8, không có ứng viên.
     */
    @Test
    public void execute_groupKeyIsDismissed_returnsNoCandidate() {
        List<RecurringCandidate> result = useCase.execute(dataBd1(), Collections.<TransactionEntity>emptyList(),
                Collections.singleton("expense|cat-ent"), USER_A, TODAY, ZONE);

        assertEquals(0, result.size());
    }

    /**
     * TN6-U19
     * BD19: today 2026-11-08; cat-ent 100.000 ngày 5 các tháng 09, 10, 11/2026: 05/11 không sau cuối ngày 08/11 nên được xét; neo là tháng hiện tại; 1 ứng viên, 3 tháng.
     */
    @Test
    public void execute_anchorInCurrentMonth_returnsCandidate() {
        assertSingleCandidate(detect(dataBd19()), 100_000L, 5, 3);
    }

    /**
     * TN6-U20
     * BD20: như BD19 nhưng today 2026-11-04: 05/11 sau cuối ngày 04/11 nên bị loại; neo tháng 10, chuỗi chỉ còn tháng 10 và 09 (2), không có ứng viên.
     */
    @Test
    public void execute_transactionInFuture_isExcludedAndReturnsNoCandidate() {
        assertEquals(0, detect(dataBd19(), LocalDate.of(2026, 11, 4), ZONE).size());
    }

    /**
     * TN6-U21
     * Thứ tự: BD1 (260.000, 3 tháng) cộng cat-rent 5.000.000 ngày 1 các tháng 07, 08, 09, 10/2026 (4 tháng): cat-rent đứng trước vì 4 > 3.
     */
    @Test
    public void execute_twoCandidates_ordersByConsecutiveMonthsDescending() {
        List<TransactionEntity> all = dataBd1();
        all.add(transactionAt("r-07", "expense", CAT_RENT, noonOf(2026, 7, 1), 5_000_000L));
        all.add(transactionAt("r-08", "expense", CAT_RENT, noonOf(2026, 8, 1), 5_000_000L));
        all.add(transactionAt("r-09", "expense", CAT_RENT, noonOf(2026, 9, 1), 5_000_000L));
        all.add(transactionAt("r-10", "expense", CAT_RENT, noonOf(2026, 10, 1), 5_000_000L));

        List<RecurringCandidate> result = detect(all);

        assertEquals(2, result.size());
        assertEquals(CAT_RENT, result.get(0).categoryId);
        assertEquals(4, result.get(0).consecutiveMonths);
        assertEquals(CAT_ENT, result.get(1).categoryId);
        assertEquals(3, result.get(1).consecutiveMonths);
    }
}