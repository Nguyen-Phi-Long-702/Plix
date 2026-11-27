package com.longvuong.plix.domain.usecase.reminder;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.LocalDate;

//Một mẫu giao dịch định kỳ sắp đến hạn cùng ngày đến hạn của nó (FIN-11)
public class UpcomingRecurringDue {
    public final TransactionEntity template;
    public final LocalDate dueDate;

    public UpcomingRecurringDue(TransactionEntity template, LocalDate dueDate) {
        this.template = template;
        this.dueDate = dueDate;
    }
}
