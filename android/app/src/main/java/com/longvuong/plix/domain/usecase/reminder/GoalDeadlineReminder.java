package com.longvuong.plix.domain.usecase.reminder;

import com.longvuong.plix.data.local.entity.GoalEntity;

//Một mục tiêu tiết kiệm sắp đến hạn cùng số ngày còn lại tới hạn (FIN-12)
public class GoalDeadlineReminder {
    public final GoalEntity goal;
    public final int daysLeft;

    public GoalDeadlineReminder(GoalEntity goal, int daysLeft) {
        this.goal = goal;
        this.daysLeft = daysLeft;
    }
}
