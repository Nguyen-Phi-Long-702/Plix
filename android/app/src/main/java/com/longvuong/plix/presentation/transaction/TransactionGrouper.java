package com.longvuong.plix.presentation.transaction;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Gom danh sách giao dịch thành các nhóm theo ngày (mỗi nhóm có 1 tiêu đề phía trên). Thứ tự nhóm và thứ tự giao dịch trong nhóm giữ đúng thứ tự của danh sách đầu vào (đã sắp xếp mới nhất trước)
public final class TransactionGrouper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TransactionGrouper() {
    }

    public static List<TransactionListItem> group(List<TransactionEntity> transactions, LocalDate today) {
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, List<TransactionEntity>> transactionsByDate = new LinkedHashMap<>();
        for (TransactionEntity transaction : transactions) {
            LocalDate date = Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate();
            transactionsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(transaction);
        }

        List<TransactionListItem> items = new ArrayList<>();
        for (Map.Entry<LocalDate, List<TransactionEntity>> entry : transactionsByDate.entrySet()) {
            items.add(new TransactionListItem.Header(titleOf(entry.getKey(), today)));
            for (TransactionEntity transaction : entry.getValue()) {
                items.add(new TransactionListItem.Row(transaction));
            }
        }
        return items;
    }

    private static String titleOf(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "Hôm nay";
        }
        if (date.equals(today.minusDays(1))) {
            return "Hôm qua";
        }
        return date.format(DATE_FORMATTER);
    }
}