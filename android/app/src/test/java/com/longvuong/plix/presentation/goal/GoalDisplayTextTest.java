package com.longvuong.plix.presentation.goal;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.R;
import com.longvuong.plix.domain.usecase.goal.GoalRequiredMonthly;
import com.longvuong.plix.domain.usecase.goal.GoalStatus;

import org.junit.Test;

public class GoalDisplayTextTest {
    @Test
    public void statusLabel_returnsDesignLabels() {
        assertEquals("Đang thực hiện", GoalDisplayText.statusLabel(GoalStatus.ACTIVE));
        assertEquals("Đã đạt", GoalDisplayText.statusLabel(GoalStatus.ACHIEVED));
        assertEquals("Quá hạn", GoalDisplayText.statusLabel(GoalStatus.EXPIRED));
    }

    @Test
    public void statusColors_matchStatus() {
        assertEquals(R.color.color_income, GoalDisplayText.statusTextColorRes(GoalStatus.ACTIVE));
        assertEquals(R.color.income_tint, GoalDisplayText.statusBackgroundColorRes(GoalStatus.ACTIVE));
        assertEquals(R.color.primary_dark, GoalDisplayText.statusTextColorRes(GoalStatus.ACHIEVED));
        assertEquals(R.color.primary_tint, GoalDisplayText.statusBackgroundColorRes(GoalStatus.ACHIEVED));
        assertEquals(R.color.budget_progress_danger, GoalDisplayText.statusTextColorRes(GoalStatus.EXPIRED));
        assertEquals(R.color.danger_tint, GoalDisplayText.statusBackgroundColorRes(GoalStatus.EXPIRED));
    }

    @Test
    public void requiredMonthlyText_normal_showsMonthlyAmount() {
        GoalRequiredMonthly monthly = new GoalRequiredMonthly(GoalRequiredMonthly.Case.NORMAL, 1400000L);

        assertEquals("Cần tiết kiệm thêm 1.400.000đ/tháng để đạt mục tiêu đúng hạn",
                GoalDisplayText.requiredMonthlyText(GoalStatus.ACTIVE, monthly, 3000000L, 10000000L));
        assertEquals(R.color.text_secondary, GoalDisplayText.requiredMonthlyColorRes(GoalStatus.ACTIVE, monthly));
    }

    @Test
    public void requiredMonthlyText_achieved_showsDoneMessage() {
        GoalRequiredMonthly monthly = new GoalRequiredMonthly(GoalRequiredMonthly.Case.ACHIEVED, 0L);

        assertEquals("Đã hoàn thành mục tiêu — không cần thêm!",
                GoalDisplayText.requiredMonthlyText(GoalStatus.ACHIEVED, monthly, 5000000L, 5000000L));
        assertEquals(R.color.primary_dark, GoalDisplayText.requiredMonthlyColorRes(GoalStatus.ACHIEVED, monthly));
    }

    @Test
    public void requiredMonthlyText_exceeded_showsExceededAmount() {
        GoalRequiredMonthly monthly = new GoalRequiredMonthly(GoalRequiredMonthly.Case.EXCEEDED, 0L);

        assertEquals("Đã vượt mục tiêu 1.000.000đ!",
                GoalDisplayText.requiredMonthlyText(GoalStatus.ACHIEVED, monthly, 23000000L, 22000000L));
    }

    @Test
    public void requiredMonthlyText_dueNowActive_isWarning() {
        GoalRequiredMonthly monthly = new GoalRequiredMonthly(GoalRequiredMonthly.Case.DUE_NOW, 2000000L);

        assertEquals("Đến hạn trong tháng này — cần bổ sung ngay 2.000.000đ",
                GoalDisplayText.requiredMonthlyText(GoalStatus.ACTIVE, monthly, 1000000L, 3000000L));
        assertEquals(R.color.warn_strong, GoalDisplayText.requiredMonthlyColorRes(GoalStatus.ACTIVE, monthly));
    }

    @Test
    public void requiredMonthlyText_dueNowExpired_isDanger() {
        GoalRequiredMonthly monthly = new GoalRequiredMonthly(GoalRequiredMonthly.Case.DUE_NOW, 2200000L);

        assertEquals("Đã quá hạn — cần bổ sung ngay 2.200.000đ",
                GoalDisplayText.requiredMonthlyText(GoalStatus.EXPIRED, monthly, 800000L, 3000000L));
        assertEquals(R.color.budget_progress_danger, GoalDisplayText.requiredMonthlyColorRes(GoalStatus.EXPIRED, monthly));
    }
}