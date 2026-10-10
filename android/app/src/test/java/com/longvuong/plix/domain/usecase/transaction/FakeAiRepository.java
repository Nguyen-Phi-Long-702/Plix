package com.longvuong.plix.domain.usecase.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.data.repository.CategorySuggestion;

class FakeAiRepository implements AiRepository {
    Result<AnomalyResult> anomalyResult;
    String lastCategoryId;
    long lastAmount;

    @Override
    public void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback) {
        lastCategoryId = categoryId;
        lastAmount = amount;
        callback.onResult(anomalyResult);
    }

    @Override
    public void cancelPendingAnomalyCheck() {
    }

    @Override
    public void categorize(String note, RepositoryCallback<CategorySuggestion> callback) {
    }

    @Override
    public void cancelPendingCategorize() {
    }

    @Override
    public void submitCorrection(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId, RepositoryCallback<Void> callback) {
    }

    @Override
    public void retrain(RepositoryCallback<Void> callback) {
    }

    @Override
    public int getPendingCorrectionCount() {
        return 0;
    }

    @Override
    public LiveData<Boolean> observeConnectivity() {
        return null;
    }
}