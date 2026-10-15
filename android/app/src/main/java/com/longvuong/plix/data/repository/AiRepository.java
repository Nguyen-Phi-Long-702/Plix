package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import java.util.List;

public interface AiRepository {

    void categorize(String note, RepositoryCallback<CategorySuggestion> callback);

    void cancelPendingCategorize();

    void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback);

    void cancelPendingAnomalyCheck();

    void getForecast(RepositoryCallback<ForecastResult> callback);

    void cancelPendingForecast();

    void parseNl(String text, boolean allowGemini, RepositoryCallback<List<ParseNlItem>> callback);

    void submitCorrection(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId, RepositoryCallback<Void> callback);

    void retrain(RepositoryCallback<Void> callback);

    int getPendingCorrectionCount();
    LiveData<Boolean> observeConnectivity();

    LiveData<Boolean> observeQuickAddEnabled();

    void setQuickAddEnabled(boolean enabled);

    LiveData<Boolean> observeGeminiFallbackEnabled();

    void setGeminiFallbackEnabled(boolean enabled);
}