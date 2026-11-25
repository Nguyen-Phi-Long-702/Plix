package com.longvuong.plix.domain.usecase.budget;

import java.time.LocalDate;

//Kết quả dự báo ngày chạm hạn mức của một ngân sách tháng hiện tại (FIN-15)
public class BudgetProjection {
    public final LocalDate exhaustionDate;
    public final int exhaustionDay;

    public BudgetProjection(LocalDate exhaustionDate, int exhaustionDay) {
        this.exhaustionDate = exhaustionDate;
        this.exhaustionDay = exhaustionDay;
    }
}