package com.longvuong.plix.presentation.transaction;

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
}