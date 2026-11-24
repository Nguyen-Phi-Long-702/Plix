package com.longvuong.plix.domain.usecase.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CompareMonthlyExpenseUseCaseTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final String CAT_FOOD = "cat-food"; //Ăn uống
    private static final String CAT_COMMUTE = "cat-commute"; //Di chuyển
    private static final String CAT_ENT = "cat-ent"; //Giải trí
    private static final String CAT_HEALTH = "cat-health"; //Y tế

    private CompareMonthlyExpenseUseCase useCase;

    @Before
    public void setUp() {
        useCase = new CompareMonthlyExpenseUseCase(new AggregateAnalyticsUseCase());
    }

    //Giao dịch bất kỳ, giữa trưa theo ZONE
    private static TransactionEntity transaction(String id, String type, String userId, boolean isDeleted,
                                                 LocalDate date, String categoryId, long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.type = type;
        entity.userId = userId;
        entity.isDeleted = isDeleted;
        entity.categoryId = categoryId;
        entity.amount = amount;
        entity.occurredAt = date.atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli();
        return entity;
    }

    //Giao dịch chi của user-A, chưa xoá
    private static TransactionEntity expense(String id, LocalDate date, String categoryId, long amount) {
        return transaction(id, "expense", USER_A, false, date, categoryId, amount);
    }

    //Bộ T4-1 (DP_02 mục 5.5), today = 2026-11-08
    private static List<TransactionEntity> dataT41() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 11, 3), CAT_FOOD, 300_000L));
        all.add(expense("t-2", LocalDate.of(2026, 11, 5), CAT_FOOD, 200_000L));
        all.add(expense("t-3", LocalDate.of(2026, 11, 6), CAT_COMMUTE, 100_000L));
        all.add(expense("t-4", LocalDate.of(2026, 11, 9), CAT_FOOD, 50_000L)); //sau hôm nay: bị loại
        all.add(expense("t-5", LocalDate.of(2026, 10, 2), CAT_FOOD, 400_000L));
        all.add(expense("t-6", LocalDate.of(2026, 10, 7), CAT_COMMUTE, 150_000L));
        all.add(expense("t-7", LocalDate.of(2026, 10, 20), CAT_FOOD, 999_999L)); //sau ngày 8 của tháng trước: bị loại
        return all;
    }

    private MonthComparison compare(List<TransactionEntity> all, LocalDate today) {
        return useCase.execute(all, USER_A, today, ZONE);
    }

    private static CategoryComparison rowOf(MonthComparison comparison, String categoryId) {
        for (CategoryComparison row : comparison.categories) {
            if (categoryId.equals(row.categoryId)) {
                return row;
            }
        }
        fail("Không tìm thấy dòng của " + categoryId);
        return null;
    }

    private static void assertRow(CategoryComparison row, long current, long previous, long delta, Integer percent) {
        assertEquals(current, row.currentAmount);
        assertEquals(previous, row.previousAmount);
        assertEquals(delta, row.deltaAmount);
        assertEquals(percent, row.percent);
    }

    /**
     * TN4-U01
     * Bộ T4-1 (today 2026-11-08): Ăn uống 500.000 / 400.000 (+100.000, +25%); Di chuyển 100.000 / 150.000 (-50.000, -33%);
     * tổng 600.000 / 550.000 (+50.000, +9%); thứ tự Ăn uống rồi Di chuyển; giao dịch 09/11 và 20/10 bị loại.
     */
    @Test
    public void execute_dataT41_comparesByCategorySameWindowAndOrdersByAbsoluteDelta() {
        MonthComparison result = compare(dataT41(), TODAY);

        assertEquals(2, result.categories.size());
        assertEquals(CAT_FOOD, result.categories.get(0).categoryId);
        assertRow(result.categories.get(0), 500_000L, 400_000L, 100_000L, 25);
        assertEquals(CAT_COMMUTE, result.categories.get(1).categoryId);
        assertRow(result.categories.get(1), 100_000L, 150_000L, -50_000L, -33);
        assertEquals(600_000L, result.currentTotal);
        assertEquals(550_000L, result.previousTotal);
        assertEquals(50_000L, result.deltaTotal);
        assertEquals(Integer.valueOf(9), result.percentTotal);
        assertEquals(LocalDate.of(2026, 11, 1), result.currentFrom);
        assertEquals(LocalDate.of(2026, 11, 8), result.currentTo);
        assertEquals(LocalDate.of(2026, 10, 1), result.previousFrom);
        assertEquals(LocalDate.of(2026, 10, 8), result.previousTo);
    }

    /**
     * TN4-U02
     * Giải trí 80.000 chỉ có tháng này: percent = null ('mới'); Y tế 120.000 chỉ có tháng trước: current 0, delta -120.000, percent -100.
     */
    @Test
    public void execute_newAndDisappearedCategories_percentNullAndMinusHundred() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 11, 4), CAT_ENT, 80_000L));
        all.add(expense("t-2", LocalDate.of(2026, 10, 4), CAT_HEALTH, 120_000L));

        MonthComparison result = compare(all, TODAY);

        assertEquals(2, result.categories.size());
        assertRow(rowOf(result, CAT_ENT), 80_000L, 0L, 80_000L, null);
        assertRow(rowOf(result, CAT_HEALTH), 0L, 120_000L, -120_000L, -100);
    }

    /**
     * TN4-U03
     * today 2027-03-30, tháng trước là 02/2027 chỉ có 28 ngày: cửa sổ tháng trước là 01 đến 28/02/2027;
     * giao dịch 25/02 100.000 được tính (previous = 100.000), giao dịch 05/03 60.000 là current.
     */
    @Test
    public void execute_todayAfterLastDayOfPreviousMonth_clampsPreviousWindowToMonthLength() {
        LocalDate today = LocalDate.of(2027, 3, 30);
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2027, 2, 25), CAT_FOOD, 100_000L));
        all.add(expense("t-2", LocalDate.of(2027, 3, 5), CAT_FOOD, 60_000L));

        MonthComparison result = compare(all, today);

        assertEquals(LocalDate.of(2027, 2, 1), result.previousFrom);
        assertEquals(LocalDate.of(2027, 2, 28), result.previousTo);
        assertEquals(LocalDate.of(2027, 3, 1), result.currentFrom);
        assertEquals(LocalDate.of(2027, 3, 30), result.currentTo);
        assertEquals(100_000L, result.previousTotal);
        assertEquals(60_000L, result.currentTotal);
    }

    /**
     * TN4-U04
     * Giao dịch thu, giao dịch đã xoá và giao dịch của user khác (đặt trong cả hai cửa sổ) không được tính vào current và previous;
     * chỉ còn hai giao dịch chi hợp lệ của user-A (100.000 tháng này, 80.000 tháng trước).
     */
    @Test
    public void execute_incomeDeletedAndOtherUserTransactions_areExcluded() {
        LocalDate currentDay = LocalDate.of(2026, 11, 5);
        LocalDate previousDay = LocalDate.of(2026, 10, 5);
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 11, 3), CAT_FOOD, 100_000L));
        all.add(expense("t-2", LocalDate.of(2026, 10, 3), CAT_FOOD, 80_000L));
        all.add(transaction("t-3", "income", USER_A, false, currentDay, CAT_FOOD, 500_000L));
        all.add(transaction("t-4", "income", USER_A, false, previousDay, CAT_FOOD, 500_000L));
        all.add(transaction("t-5", "expense", USER_A, true, currentDay, CAT_FOOD, 600_000L));
        all.add(transaction("t-6", "expense", USER_A, true, previousDay, CAT_FOOD, 600_000L));
        all.add(transaction("t-7", "expense", USER_B, false, currentDay, CAT_FOOD, 700_000L));
        all.add(transaction("t-8", "expense", USER_B, false, previousDay, CAT_FOOD, 700_000L));

        MonthComparison result = compare(all, TODAY);

        assertEquals(100_000L, result.currentTotal);
        assertEquals(80_000L, result.previousTotal);
        assertEquals(1, result.categories.size());
        assertRow(result.categories.get(0), 100_000L, 80_000L, 20_000L, 25);
    }
}