package com.longvuong.plix.presentation.settings;

import com.longvuong.plix.data.reminder.ReminderScheduler;

//ReminderScheduler giả: không dùng WorkManager, chỉ đếm số lần gọi hai hàm mà test kiểm tra (applyForCurrentUser, cancelDaily)
public class FakeReminderScheduler implements ReminderScheduler {
    public int applyCalls;
    public int cancelDailyCalls;

    @Override
    public void applyForCurrentUser() {
        applyCalls++;
    }

    @Override
    public void scheduleNextDaily() {
    }

    @Override
    public void cancelDaily() {
        cancelDailyCalls++;
    }

    @Override
    public void cancelDue() {
    }

    @Override
    public void cancelAll() {
    }
}
