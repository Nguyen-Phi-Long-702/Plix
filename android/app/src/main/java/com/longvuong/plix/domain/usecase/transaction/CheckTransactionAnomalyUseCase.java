package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.data.repository.TransactionRepository;

import javax.inject.Inject;

public class CheckTransactionAnomalyUseCase {
    private final AiRepository aiRepository;
    private final TransactionRepository transactionRepository;

    @Inject
    public CheckTransactionAnomalyUseCase(AiRepository aiRepository, TransactionRepository transactionRepository) {
        this.aiRepository = aiRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(String userId, String transactionId, String categoryId, long amount,
                        RepositoryCallback<AnomalyCheckOutcome> callback) {
        aiRepository.checkAnomaly(categoryId, amount, anomalyResult -> {
            if (anomalyResult instanceof Result.Error) {
                Result.Error<AnomalyResult> error = (Result.Error<AnomalyResult>) anomalyResult;
                callback.onResult(new Result.Error<>(error.type, error.message, error.cause));
                return;
            }
            AnomalyResult anomaly = ((Result.Success<AnomalyResult>) anomalyResult).data;
            if (!isFlagged(anomaly)) {
                callback.onResult(new Result.Success<>(new AnomalyCheckOutcome(anomaly, false)));
                return;
            }
            //Chỉ khi bị gắn cờ mới cần biết còn giao dịch cùng danh mục đang chờ đồng bộ hay không (loại trừ giao dịch vừa lưu)
            transactionRepository.countPendingByCategory(userId, categoryId, transactionId, countResult -> {
                boolean hasOtherPending = countResult instanceof Result.Success
                        && ((Result.Success<Integer>) countResult).data > 0;
                callback.onResult(new Result.Success<>(new AnomalyCheckOutcome(anomaly, hasOtherPending)));
            });
        });
    }

    private boolean isFlagged(AnomalyResult anomaly) {
        return anomaly.level == AnomalyResult.Level.HIGH || anomaly.level == AnomalyResult.Level.LOW;
    }
}