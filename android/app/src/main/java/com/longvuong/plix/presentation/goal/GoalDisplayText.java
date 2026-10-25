package com.longvuong.plix.presentation.goal;

import androidx.annotation.ColorRes;

import com.longvuong.plix.R;
import com.longvuong.plix.domain.usecase.goal.GoalRequiredMonthly;
import com.longvuong.plix.domain.usecase.goal.GoalStatus;

import java.text.NumberFormat;
import java.util.Locale;

//Chữ và màu hiển thị của mục tiêu (nhãn trạng thái, dòng cần tiết kiệm mỗi tháng) dùng chung cho danh sách và form; chỉ đổi kết quả của UseCase thành chữ/màu, không tính lại
public final class GoalDisplayText {
    private GoalDisplayText() {
    }

    public static String statusLabel(GoalStatus status) {
        switch (status) {
            case ACHIEVED:
                return "Đã đạt";
            case EXPIRED:
                return "Quá hạn";
            case ACTIVE:
            default:
                return "Đang thực hiện";
        }
    }

    @ColorRes
    public static int statusTextColorRes(GoalStatus status) {
        switch (status) {
            case ACHIEVED:
                return R.color.primary_dark;
            case EXPIRED:
                return R.color.budget_progress_danger;
            case ACTIVE:
            default:
                return R.color.color_income;
        }
    }

    @ColorRes
    public static int statusBackgroundColorRes(GoalStatus status) {
        switch (status) {
            case ACHIEVED:
                return R.color.primary_tint;
            case EXPIRED:
                return R.color.danger_tint;
            case ACTIVE:
            default:
                return R.color.income_tint;
        }
    }

    public static String requiredMonthlyText(GoalStatus status, GoalRequiredMonthly requiredMonthly,
                                             long currentAmount, long targetAmount) {
        switch (requiredMonthly.caseType) {
            case ACHIEVED:
                return "Đã hoàn thành mục tiêu — không cần thêm!";
            case EXCEEDED:
                return "Đã vượt mục tiêu " + formatCurrency(currentAmount - targetAmount) + "!";
            case DUE_NOW:
                return (status == GoalStatus.EXPIRED
                        ? "Đã quá hạn — cần bổ sung ngay "
                        : "Đến hạn trong tháng này — cần bổ sung ngay ")
                        + formatCurrency(requiredMonthly.monthlyAmount);
            case NORMAL:
            default:
                return "Cần tiết kiệm thêm " + formatCurrency(requiredMonthly.monthlyAmount)
                        + "/tháng để đạt mục tiêu đúng hạn";
        }
    }

    @ColorRes
    public static int requiredMonthlyColorRes(GoalStatus status, GoalRequiredMonthly requiredMonthly) {
        switch (requiredMonthly.caseType) {
            case ACHIEVED:
            case EXCEEDED:
                return R.color.primary_dark;
            case DUE_NOW:
                return status == GoalStatus.EXPIRED ? R.color.budget_progress_danger : R.color.warn_strong;
            case NORMAL:
            default:
                return R.color.text_secondary;
        }
    }

    private static String formatCurrency(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }
}