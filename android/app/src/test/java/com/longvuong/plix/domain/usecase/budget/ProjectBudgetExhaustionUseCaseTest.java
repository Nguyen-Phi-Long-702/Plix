package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;

public class ProjectBudgetExhaustionUseCaseTest {
    private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, 11);

    private ProjectBudgetExhaustionUseCase useCase;

    @Before
    public void setUp() {
        useCase = new ProjectBudgetExhaustionUseCase();
    }

    /**
     * TN4-U05
     * limit 3.000.000, today 2026-11-10, spent 2.000.000, period 2026-11: exhaustionDay = ceil(3.000.000 x 10 / 2.000.000) = 15,
     * ngày dự kiến 2026-11-15.
     */
    @Test
    public void execute_limit3MSpent2MOnDay10_projectsDay15() {
        BudgetProjection result = useCase.execute(3_000_000L, 2_000_000L, LocalDate.of(2026, 11, 10), NOVEMBER_2026);

        assertNotNull(result);
        assertEquals(15, result.exhaustionDay);
        assertEquals(LocalDate.of(2026, 11, 15), result.exhaustionDate);
    }

    /**
     * TN4-U06
     * limit 3.000.000, today 2026-11-06, spent 2.000.000: mới qua 6 ngày (nhỏ hơn 7) nên không có dự báo (null).
     */
    @Test
    public void execute_beforeSeventhDay_returnsNull() {
        BudgetProjection result = useCase.execute(3_000_000L, 2_000_000L, LocalDate.of(2026, 11, 6), NOVEMBER_2026);

        assertNull(result);
    }

    /**
     * TN4-U07
     * Ngày dự kiến không nhỏ hơn số ngày của tháng thì không hiện:
     * (a) limit 5.000.000, today 2026-11-20, spent 2.000.000 -> ngày 50: null;
     * (b) limit 3.000.000, today 2026-11-10, spent 1.000.000 -> ngày 30, không nhỏ hơn 30 ngày của tháng 11: null.
     */
    @Test
    public void execute_projectedDayNotBeforeEndOfMonth_returnsNull() {
        BudgetProjection dayFifty = useCase.execute(5_000_000L, 2_000_000L, LocalDate.of(2026, 11, 20), NOVEMBER_2026);
        BudgetProjection dayThirty = useCase.execute(3_000_000L, 1_000_000L, LocalDate.of(2026, 11, 10), NOVEMBER_2026);

        assertNull(dayFifty);
        assertNull(dayThirty);
    }

    /**
     * TN4-U08
     * limit 2.900.000, today 2026-11-10, spent 1.000.000: exhaustionDay = ceil(2.900.000 x 10 / 1.000.000) = 29 (nhỏ hơn 30) nên vẫn hiện,
     * ngày dự kiến 2026-11-29.
     */
    @Test
    public void execute_projectedDay29InThirtyDayMonth_stillProjects() {
        BudgetProjection result = useCase.execute(2_900_000L, 1_000_000L, LocalDate.of(2026, 11, 10), NOVEMBER_2026);

        assertNotNull(result);
        assertEquals(29, result.exhaustionDay);
        assertEquals(LocalDate.of(2026, 11, 29), result.exhaustionDate);
    }

    /**
     * TN4-U09
     * Không có dự báo (null) khi: đã chạm hoặc vượt hạn mức (spent >= limit), chưa chi (spent = 0),
     * hoặc kỳ không phải tháng hiện tại (period 2026-10 khi today ở 2026-11).
     */
    @Test
    public void execute_spentReachedLimitOrNothingSpentOrOtherPeriod_returnsNull() {
        LocalDate today = LocalDate.of(2026, 11, 10);

        assertNull(useCase.execute(3_000_000L, 3_000_000L, today, NOVEMBER_2026));
        assertNull(useCase.execute(3_000_000L, 3_500_000L, today, NOVEMBER_2026));
        assertNull(useCase.execute(3_000_000L, 0L, today, NOVEMBER_2026));
        assertNull(useCase.execute(3_000_000L, 2_000_000L, today, YearMonth.of(2026, 10)));
    }
}