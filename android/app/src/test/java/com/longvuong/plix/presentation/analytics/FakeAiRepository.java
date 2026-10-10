package com.longvuong.plix.presentation.analytics;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.data.repository.CategorySuggestion;
import com.longvuong.plix.data.repository.ForecastResult;

class FakeAiRepository implements AiRepository {

    int getForecastCallCount;
    boolean cancelForecastCalled;
    private RepositoryCallback<ForecastResult> pendingForecastCallback;

    @Override
    public void getForecast(RepositoryCallback<ForecastResult> callback) {
        getForecastCallCount++;
        pendingForecastCallback = callback; //chưa gọi ngay - test tự quyết định lúc nào trả kết quả để bắt được UiState.Loading
    }

    void completeForecast(Result<ForecastResult> result) {
        RepositoryCallback<ForecastResult> callback = pendingForecastCallback;
        pendingForecastCallback = null;
        if (callback != null) {
            callback.onResult(result);
        }
    }

    @Override
    public void cancelPendingForecast() {
        cancelForecastCalled = true;
    }

    @Override
    public void categorize(String note, RepositoryCallback<CategorySuggestion> callback) {
    }

    @Override
    public void cancelPendingCategorize() {
    }

    @Override
    public void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback) {
    }

    @Override
    public void cancelPendingAnomalyCheck() {
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