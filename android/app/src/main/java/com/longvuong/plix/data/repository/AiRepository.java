package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;

public interface AiRepository {

    void categorize(String note, RepositoryCallback<CategorySuggestion> callback);

    void cancelPendingCategorize();

    void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback);

    void cancelPendingAnomalyCheck();
    void submitCorrection(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId, RepositoryCallback<Void> callback);

    void retrain(RepositoryCallback<Void> callback);

    int getPendingCorrectionCount();
    LiveData<Boolean> observeConnectivity();
}