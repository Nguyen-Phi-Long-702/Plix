package com.longvuong.plix.domain.usecase.reminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.domain.usecase.goal.GoalStatusUseCase;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class FindGoalsNearDeadlineUseCaseTest {
    private static final String USER_ID = "user-1";
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    private final FindGoalsNearDeadlineUseCase useCase = new FindGoalsNearDeadlineUseCase(new GoalStatusUseCase());

    /**
     * TN3-U09
     * today 2026-11-08, mục tiêu có deadline 2026-11-15, 2026-11-09, 2026-11-12: hai mục tiêu đầu được trả về
     * với daysLeft 7 và 1; mục tiêu deadline 2026-11-12 (còn 4 ngày) không được trả về.
     */
    @Test
    public void execute_goalsWithSevenAndOneDaysLeft_returnsOnlyThoseTwo() {
        GoalEntity sevenDaysLeft = goal("g-7", USER_ID, LocalDate.of(2026, 11, 15));
        GoalEntity oneDayLeft = goal("g-1", USER_ID, LocalDate.of(2026, 11, 9));
        GoalEntity fourDaysLeft = goal("g-4", USER_ID, LocalDate.of(2026, 11, 12));

        List<GoalDeadlineReminder> result = useCase.execute(
                Arrays.asList(sevenDaysLeft, oneDayLeft, fourDaysLeft), USER_ID, TODAY, VIETNAM);

        assertEquals(2, result.size());
        assertEquals("g-7", result.get(0).goal.id);
        assertEquals(7, result.get(0).daysLeft);
        assertEquals("g-1", result.get(1).goal.id);
        assertEquals(1, result.get(1).daysLeft);
    }

    /**
     * TN3-U10
     * today 2026-11-08: mục tiêu đã đạt (currentAmount >= targetAmount), đã xoá, deadline hôm nay, deadline đã qua,
     * của user khác: mỗi mục tiêu bị loại bởi đúng một điều kiện nên không mục nào được trả về.
     */
    @Test
    public void execute_ineligibleGoals_returnsEmpty() {
        GoalEntity achieved = goal("g-achieved", USER_ID, LocalDate.of(2026, 11, 15));
        achieved.currentAmount = achieved.targetAmount;
        GoalEntity deleted = goal("g-deleted", USER_ID, LocalDate.of(2026, 11, 15));
        deleted.isDeleted = true;
        GoalEntity dueToday = goal("g-due-today", USER_ID, TODAY);
        GoalEntity expired = goal("g-expired", USER_ID, LocalDate.of(2026, 11, 7));
        GoalEntity otherUser = goal("g-other-user", "user-2", LocalDate.of(2026, 11, 15));

        List<GoalDeadlineReminder> result = useCase.execute(
                Arrays.asList(achieved, deleted, dueToday, expired, otherUser), USER_ID, TODAY, VIETNAM);

        assertTrue(result.isEmpty());
    }

    //Mục tiêu hợp lệ, chưa đạt, chưa xoá; deadline lưu lúc 23:59:59 của ngày đã chọn giống cách app lưu (AddEditGoalFragment); từng test sửa đúng một trường để tạo ca cần thử
    private static GoalEntity goal(String id, String userId, LocalDate deadlineDate) {
        GoalEntity goal = new GoalEntity();
        goal.id = id;
        goal.userId = userId;
        goal.name = "Mua xe máy";
        goal.targetAmount = 10_000_000L;
        goal.currentAmount = 3_000_000L;
        goal.deadline = deadlineDate.atTime(23, 59, 59).atZone(VIETNAM).toInstant().toEpochMilli();
        goal.updatedAt = 0L;
        goal.syncStatus = "pending";
        goal.isDeleted = false;
        return goal;
    }
}
