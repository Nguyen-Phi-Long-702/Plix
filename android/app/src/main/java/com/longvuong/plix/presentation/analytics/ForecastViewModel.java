package com.longvuong.plix.presentation.analytics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.ForecastResult;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.analytics.CalculateMonthToDateNetUseCase;
import com.longvuong.plix.presentation.common.UiState;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ForecastViewModel extends ViewModel {

    private final AiRepository aiRepository;
    private final TransactionRepository transactionRepository;
    private final CalculateMonthToDateNetUseCase calculateMonthToDateNetUseCase;
    //null = chưa gọi loadForecast(); Loading = đang gọi /forecast; Success = có số dự báo + NET tích luỹ thực tế để vẽ biểu đồ;
    //Empty = chưa đủ dữ liệu để dự báo (không phải lỗi); Error = lỗi mạng/máy chủ hoặc lỗi đọc dữ liệu cục bộ
    private final MutableLiveData<UiState<ForecastUiModel>> forecastState = new MutableLiveData<>();
    //true = hiện thẻ dự báo; false = đang mất mạng nên ẩn hẳn thẻ. Mỗi lần trạng thái mạng đổi sẽ tự gọi lại loadForecast()
    private final MediatorLiveData<Boolean> forecastVisible = new MediatorLiveData<>();

    @Inject
    public ForecastViewModel(AiRepository aiRepository,
                             TransactionRepository transactionRepository,
                             CalculateMonthToDateNetUseCase calculateMonthToDateNetUseCase) {
        this.aiRepository = aiRepository;
        this.transactionRepository = transactionRepository;
        this.calculateMonthToDateNetUseCase = calculateMonthToDateNetUseCase;
        forecastVisible.setValue(true);
        forecastVisible.addSource(aiRepository.observeConnectivity(), isConnected -> loadForecast());
    }

    public LiveData<UiState<ForecastUiModel>> getForecastState() {
        return forecastState;
    }

    public LiveData<Boolean> getForecastVisible() {
        return forecastVisible;
    }

    public void loadForecast() {
        if (Boolean.FALSE.equals(aiRepository.observeConnectivity().getValue())) {
            forecastVisible.setValue(false); //AI-10: mất mạng -> ẩn thẻ dự báo, không gọi API, không báo lỗi
            return;
        }
        forecastVisible.setValue(true);
        if (forecastState.getValue() instanceof UiState.Loading) {
            return; //đang gọi rồi, bỏ qua lần gọi thứ hai
        }
        forecastState.setValue(new UiState.Loading<>());
        aiRepository.getForecast(this::onForecastResult);
    }

    private void onForecastResult(Result<ForecastResult> result) {
        if (result instanceof Result.Error) {
            Result.Error<ForecastResult> error = (Result.Error<ForecastResult>) result;
            forecastState.setValue(new UiState.Error<>(error.message));
            return;
        }
        ForecastResult forecast = ((Result.Success<ForecastResult>) result).data;
        if (forecast.status == ForecastResult.Status.INSUFFICIENT_DATA) {
            forecastState.setValue(new UiState.Empty<>());
            return;
        }
        long forecastNetAmount = forecast.forecastNetAmount; //luôn có khi status = OK
        transactionRepository.getAllOnce(transactionsResult -> onTransactionsLoaded(transactionsResult, forecastNetAmount));
    }

    private void onTransactionsLoaded(Result<List<TransactionEntity>> result, long forecastNetAmount) {
        if (result instanceof Result.Error) {
            Result.Error<List<TransactionEntity>> error = (Result.Error<List<TransactionEntity>>) result;
            forecastState.setValue(new UiState.Error<>(error.message));
            return;
        }
        List<TransactionEntity> transactions = ((Result.Success<List<TransactionEntity>>) result).data;
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<Long> actualNetByDay = calculateMonthToDateNetUseCase.execute(transactions, today);
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        forecastState.setValue(new UiState.Success<>(
                new ForecastUiModel(forecastNetAmount, formatCurrency(forecastNetAmount), actualNetByDay, daysInMonth)));
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        aiRepository.cancelPendingForecast();
    }
}