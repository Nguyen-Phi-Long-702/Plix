package com.longvuong.plix.presentation.analytics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.ForecastResult;
import com.longvuong.plix.presentation.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ForecastViewModel extends ViewModel {

    private final AiRepository aiRepository;
    //null = chưa gọi loadForecast(); Loading = đang gọi /forecast; Success = có số dự báo;
    //Empty = chưa đủ dữ liệu để dự báo (không phải lỗi); Error = lỗi mạng/máy chủ
    private final MutableLiveData<UiState<ForecastResult>> forecastState = new MutableLiveData<>();

    @Inject
    public ForecastViewModel(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
    }

    public LiveData<UiState<ForecastResult>> getForecastState() {
        return forecastState;
    }

    public void loadForecast() {
        if (forecastState.getValue() instanceof UiState.Loading) {
            return; //đang gọi rồi, bỏ qua lần gọi thứ hai
        }
        forecastState.setValue(new UiState.Loading<>());
        aiRepository.getForecast(result -> forecastState.setValue(toUiState(result)));
    }

    private UiState<ForecastResult> toUiState(Result<ForecastResult> result) {
        if (result instanceof Result.Success) {
            ForecastResult forecast = ((Result.Success<ForecastResult>) result).data;
            if (forecast.status == ForecastResult.Status.INSUFFICIENT_DATA) {
                return new UiState.Empty<>();
            }
            return new UiState.Success<>(forecast);
        }
        Result.Error<ForecastResult> error = (Result.Error<ForecastResult>) result;
        return new UiState.Error<>(error.message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        aiRepository.cancelPendingForecast();
    }
}