package com.longvuong.plix.domain.usecase.transaction;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import javax.inject.Inject;
public class FindSimilarTransactionUseCase {
    private final TransactionRepository transactionRepository;

    @Inject
    public FindSimilarTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void execute(long amount, @Nullable String categoryId, long occurredAt,
                        RepositoryCallback<TransactionEntity> callback) {
        if (categoryId == null) {
            callback.onResult(new Result.Success<>(null)); //chưa chọn danh mục thì không có gì để so sánh
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate();
        long dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(); //đầu ngày kế tiếp, không tính vào khoảng
        transactionRepository.findSimilar(amount, categoryId, dayStart, dayEnd, callback);
    }
}