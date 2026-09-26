package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.CategorySuggestion;

class FakeAiRepository implements AiRepository {
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
}