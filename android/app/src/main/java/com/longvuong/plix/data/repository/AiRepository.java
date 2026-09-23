package com.longvuong.plix.data.repository;

import com.longvuong.plix.core.error.RepositoryCallback;

public interface AiRepository {

    void categorize(String note, RepositoryCallback<CategorySuggestion> callback);

    void cancelPendingCategorize();
}