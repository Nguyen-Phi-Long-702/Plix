package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.RepositoryCallback;

public interface AiRepository {

    void categorize(String note, RepositoryCallback<CategorySuggestion> callback);

    void cancelPendingCategorize();
    void submitCorrection(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId, RepositoryCallback<Void> callback);
}