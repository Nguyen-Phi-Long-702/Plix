package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class SuggestBudgetLimitUseCaseTest {
    private static final YearMonth TARGET = YearMonth.of(2026, 11);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final String CAT_FOOD = "cat-food"; //Ăn uống
    private static final String CAT_ENT = "cat-ent"; //Giải trí

    private SuggestBudgetLimitUseCase useCase;

    @Before
    public void setUp() {
        useCase = new SuggestBudgetLimitUseCase();
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

    /**
     * TN4-U10
     * Ngân sách tổng, kỳ 2026-11; chi 08/2026 3.000.000, 09/2026 4.000.000, 10/2026 5.000.000 (gồm 2 giao dịch 2.000.000 + 3.000.000 khác danh mục):
     * (3.000.000 + 4.000.000 + 5.000.000) / 3 = 4.000.000 (đã tròn nghìn), monthsUsed = 3.
     */
    @Test
    public void execute_threeMonthsOfData_averagesThreeMonths() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 8, 15), CAT_FOOD, 3_000_000L));
        all.add(expense("t-2", LocalDate.of(2026, 9, 15), CAT_FOOD, 4_000_000L));
        all.add(expense("t-3", LocalDate.of(2026, 10, 5), CAT_FOOD, 2_000_000L));
        all.add(expense("t-4", LocalDate.of(2026, 10, 20), CAT_ENT, 3_000_000L));

        BudgetLimitSuggestion result = useCase.execute(all, USER_A, null, TARGET, ZONE);

        assertNotNull(result);
        assertEquals(4_000_000L, result.suggestedAmount);
        assertEquals(3, result.monthsUsed);
    }

    /**
     * TN4-U11
     * Kỳ 2026-11; chỉ có chi 09/2026 4.000.000 và 10/2026 2.001.000 (08/2026 không có): chỉ tính 2 tháng có dữ liệu,
     * (4.000.000 + 2.001.000) / 2 = 3.000.500, làm tròn nửa lên đến nghìn = 3.001.000, monthsUsed = 2.
     */
    @Test
    public void execute_twoMonthsOfData_averagesOnlyMonthsWithDataAndRoundsHalfUp() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 9, 15), CAT_FOOD, 4_000_000L));
        all.add(expense("t-2", LocalDate.of(2026, 10, 15), CAT_FOOD, 2_001_000L));

        BudgetLimitSuggestion result = useCase.execute(all, USER_A, null, TARGET, ZONE);

        assertNotNull(result);
        assertEquals(3_001_000L, result.suggestedAmount);
        assertEquals(2, result.monthsUsed);
    }

    /**
     * TN4-U12
     * Không có gợi ý (null) khi: (a) không có giao dịch chi nào; (b) chỉ có chi 06/2026 9.000.000 (ngoài 3 tháng 08, 09, 10/2026).
     */
    @Test
    public void execute_noDataOrOnlyTooOldMonth_returnsNull() {
        List<TransactionEntity> onlyTooOld = new ArrayList<>();
        onlyTooOld.add(expense("t-1", LocalDate.of(2026, 6, 15), CAT_FOOD, 9_000_000L));

        assertNull(useCase.execute(new ArrayList<>(), USER_A, null, TARGET, ZONE));
        assertNull(useCase.execute(onlyTooOld, USER_A, null, TARGET, ZONE));
    }

    /**
     * TN4-U13
     * Ngân sách danh mục 'cat-food', kỳ 2026-11: chỉ tính chi cat-food 10/2026 600.000; bị loại: chi cat-ent 400.000,
     * giao dịch thu cat-food 700.000, giao dịch cat-food đã xoá 800.000, giao dịch cat-food của user khác 900.000.
     * Gợi ý = 600.000, monthsUsed = 1.
     */
    @Test
    public void execute_categoryScope_countsOnlyThatCategoryExpenseOfCurrentUser() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(expense("t-1", LocalDate.of(2026, 10, 10), CAT_FOOD, 600_000L));
        all.add(expense("t-2", LocalDate.of(2026, 10, 12), CAT_ENT, 400_000L)); //khác danh mục: bị loại
        all.add(transaction("t-3", "income", USER_A, false, LocalDate.of(2026, 10, 5), CAT_FOOD, 700_000L)); //giao dịch thu: bị loại
        all.add(transaction("t-4", "expense", USER_A, true, LocalDate.of(2026, 10, 6), CAT_FOOD, 800_000L)); //đã xoá: bị loại
        all.add(transaction("t-5", "expense", USER_B, false, LocalDate.of(2026, 10, 7), CAT_FOOD, 900_000L)); //user khác: bị loại

        BudgetLimitSuggestion result = useCase.execute(all, USER_A, CAT_FOOD, TARGET, ZONE);

        assertNotNull(result);
        assertEquals(600_000L, result.suggestedAmount);
        assertEquals(1, result.monthsUsed);
    }
}