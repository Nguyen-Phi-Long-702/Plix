package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.CategorySuggestion;
import com.longvuong.plix.data.repository.AnomalyResult;

class FakeAiRepository implements AiRepository {

    private final MutableLiveData<Boolean> connectivity = new MutableLiveData<>(true);

    volatile boolean categorizeCalled;
    private volatile RepositoryCallback<CategorySuggestion> pendingCategorizeCallback;

    @Override
    public void categorize(String note, RepositoryCallback<CategorySuggestion> callback) {
        pendingCategorizeCallback = callback; //chưa gọi ngay - test tự quyết định lúc nào trả kết quả để bắt được UiState.Loading
        categorizeCalled = true; //set sau cùng (volatile) để test đợi cờ này an toàn giữa luồng debounce và luồng test
    }

    void completeCategorize(Result<CategorySuggestion> result) {
        RepositoryCallback<CategorySuggestion> callback = pendingCategorizeCallback;
        pendingCategorizeCallback = null;
        if (callback != null) {
            callback.onResult(result);
        }
    }

    @Override
    public void cancelPendingCategorize() {
        pendingCategorizeCallback = null; //giả lập request thật đã bị huỷ -> callback cũ không còn được gọi nữa
    }

    volatile boolean checkAnomalyCalled;
    String lastAnomalyCategoryId;
    long lastAnomalyAmount;
    private RepositoryCallback<AnomalyResult> pendingAnomalyCallback;

    @Override
    public void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback) {
        lastAnomalyCategoryId = categoryId;
        lastAnomalyAmount = amount;
        pendingAnomalyCallback = callback; //test tự quyết định lúc nào trả kết quả
        checkAnomalyCalled = true;
    }

    void completeAnomaly(Result<AnomalyResult> result) {
        RepositoryCallback<AnomalyResult> callback = pendingAnomalyCallback;
        pendingAnomalyCallback = null;
        if (callback != null) {
            callback.onResult(result);
        }
    }

    @Override
    public void cancelPendingAnomalyCheck() {
        pendingAnomalyCallback = null;
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
}