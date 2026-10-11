package com.longvuong.plix.domain.usecase.analytics;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CalculateMonthToDateNetUseCase {
    @Inject
    public CalculateMonthToDateNetUseCase() {
    }

    //Trả về NET tích luỹ (thu − chi) của tháng chứa `today`, tính đến hết từng ngày.
    //Phần tử thứ i ứng với ngày i + 1 của tháng; độ dài = ngày hôm nay trong tháng (không tính các ngày sau hôm nay)
    public List<Long> execute(List<TransactionEntity> transactions, LocalDate today) {
        int dayCount = today.getDayOfMonth();
        long[] netPerDay = new long[dayCount];
        YearMonth currentMonth = YearMonth.from(today);

        if (transactions != null) {
            for (TransactionEntity transaction : transactions) {
                if (transaction.isDeleted) {
                    continue;
                }
                LocalDate date = Instant.ofEpochMilli(transaction.occurredAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                if (!YearMonth.from(date).equals(currentMonth) || date.isAfter(today)) {
                    continue;
                }
                int index = date.getDayOfMonth() - 1;
                if ("income".equals(transaction.type)) {
                    netPerDay[index] += transaction.amount;
                } else if ("expense".equals(transaction.type)) {
                    netPerDay[index] -= transaction.amount;
                }
            }
        }

        List<Long> cumulative = new ArrayList<>(dayCount);
        long running = 0L;
        for (long dayNet : netPerDay) {
            running += dayNet;
            cumulative.add(running);
        }
        return cumulative;
    }
}