package com.longvuong.plix.domain.usecase.budget;

import java.time.LocalDate;
import java.time.YearMonth;

import javax.inject.Inject;

//Dự báo ngày chạm hạn mức của ngân sách tháng hiện tại từ nhịp chi tiêu (FIN-15). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 5.4: giả định chi đều theo ngày; đây là ước tính thống kê, không phải dự đoán
public class ProjectBudgetExhaustionUseCase {
    private static final int MIN_DAYS_ELAPSED = 7; //ít hơn 7 ngày thì nhịp chi quá nhiễu

    @Inject
    public ProjectBudgetExhaustionUseCase() {
    }

    //Trả về null khi không có dự báo
    public BudgetProjection execute(long limit, long spent, LocalDate today, YearMonth period) {
        if (!YearMonth.from(today).equals(period)) {
            return null; //chỉ dự báo cho kỳ hiện tại
        }
        int daysElapsed = today.getDayOfMonth();
        if (daysElapsed < MIN_DAYS_ELAPSED) {
            return null;
        }
        if (spent <= 0L || spent >= limit) {
            return null; //chưa chi, hoặc đã chạm/vượt hạn mức
        }
        //X = ceil(limit * ngày hiện tại / spent), tính bằng số nguyên; so sánh khi còn là long để không tràn khi ép sang int
        long exhaustionDay = (limit * daysElapsed + spent - 1L) / spent;
        if (exhaustionDay >= period.lengthOfMonth()) {
            return null; //dự kiến không chạm hạn mức trong tháng
        }
        return new BudgetProjection(period.atDay((int) exhaustionDay), (int) exhaustionDay);
    }
}