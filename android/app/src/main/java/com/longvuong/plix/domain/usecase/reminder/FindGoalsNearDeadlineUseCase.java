package com.longvuong.plix.domain.usecase.reminder;

import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.domain.usecase.goal.GoalStatus;
import com.longvuong.plix.domain.usecase.goal.GoalStatusUseCase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

//Chọn các mục tiêu tiết kiệm chưa đạt còn đúng 7 hoặc 1 ngày tới hạn (FIN-12). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 3.4: lọc mục tiêu của người dùng, chưa xoá, còn ACTIVE (dùng GoalStatusUseCase hiện có), rồi giữ mục tiêu có daysLeft thuộc {7, 1}
public class FindGoalsNearDeadlineUseCase {
    private static final int DAYS_LEFT_ONE_WEEK = 7;
    private static final int DAYS_LEFT_ONE_DAY = 1;

    private final GoalStatusUseCase goalStatusUseCase;

    @Inject
    public FindGoalsNearDeadlineUseCase(GoalStatusUseCase goalStatusUseCase) {
        this.goalStatusUseCase = goalStatusUseCase;
    }

    public List<GoalDeadlineReminder> execute(List<GoalEntity> goals, String userId, LocalDate today, ZoneId zone) {
        //Mốc "bây giờ" để xét trạng thái là 00:00 của today: deadline được lưu lúc 23:59:59 của ngày đã chọn
        //nên mục tiêu hết hạn hôm nay vẫn ACTIVE (bị loại ở bước daysLeft = 0), còn hết hạn từ hôm qua trở về trước là EXPIRED
        long nowEpochMs = today.atStartOfDay(zone).toInstant().toEpochMilli();
        List<GoalDeadlineReminder> result = new ArrayList<>();
        for (GoalEntity goal : goals) {
            if (goal.isDeleted || !Objects.equals(userId, goal.userId)) {
                continue;
            }
            if (goalStatusUseCase.execute(goal, nowEpochMs) != GoalStatus.ACTIVE) {
                continue; //đã đạt mục tiêu hoặc đã quá hạn thì không nhắc
            }
            LocalDate deadlineDate = Instant.ofEpochMilli(goal.deadline).atZone(zone).toLocalDate();
            int daysLeft = (int) ChronoUnit.DAYS.between(today, deadlineDate);
            if (daysLeft == DAYS_LEFT_ONE_WEEK || daysLeft == DAYS_LEFT_ONE_DAY) {
                result.add(new GoalDeadlineReminder(goal, daysLeft));
            }
        }
        return result;
    }
}
