package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FilterTransactionsUseCase {

    @Inject
    public FilterTransactionsUseCase() {
    }

    public List<TransactionEntity> execute(List<TransactionEntity> transactions, String categoryId, Long startDate, Long endDate) {
        List<TransactionEntity> result = new ArrayList<>();
        if (transactions == null) {
            return result;
        }
        for (TransactionEntity transaction : transactions) {
            if (transaction.isDeleted) {
                continue;
            }
            if (categoryId != null && !categoryId.equals(transaction.categoryId)) {
                continue;
            }
            if (startDate != null && transaction.occurredAt < startDate) {
                continue;
            }
            if (endDate != null && transaction.occurredAt > endDate) {
                continue;
            }
            result.add(transaction);
        }
        return result;
    }
}