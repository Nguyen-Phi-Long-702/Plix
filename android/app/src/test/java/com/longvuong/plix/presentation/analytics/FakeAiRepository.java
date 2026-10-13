package com.longvuong.plix.presentation.analytics;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.data.repository.CategorySuggestion;
import com.longvuong.plix.data.repository.ForecastResult;
import com.longvuong.plix.data.repository.ParseNlItem;

import java.util.List;

class FakeAiRepository implements AiRepository {
    private final MutableLiveData<Boolean> connectivity = new MutableLiveData<>(true);
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
    public void parseNl(String text, RepositoryCallback<List<ParseNlItem>> callback) {
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
        return connectivity;
    }

    void setConnected(boolean connected) {
        connectivity.setValue(connected);
    }

    @Override
    public LiveData<Boolean> observeQuickAddEnabled() {
        return null;
    }

    @Override
    public void setQuickAddEnabled(boolean enabled) {
    }

    @Override
    public LiveData<Boolean> observeGeminiFallbackEnabled() {
        return null;
    }

    @Override
    public void setGeminiFallbackEnabled(boolean enabled) {
    }
}