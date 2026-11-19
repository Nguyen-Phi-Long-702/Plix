package com.longvuong.plix.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

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
        assertEquals(1, result.size());
        RecurringCandidate candidate = result.get(0);
        assertEquals("expense", candidate.type);
        assertEquals(CAT_ENT, candidate.categoryId);
        assertEquals(amount, candidate.amount);
        assertEquals(dayOfMonth, candidate.dayOfMonth);
        assertEquals(consecutiveMonths, candidate.consecutiveMonths);
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
}