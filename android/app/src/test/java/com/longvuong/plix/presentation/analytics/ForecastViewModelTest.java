package com.longvuong.plix.presentation.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.ForecastResult;
import com.longvuong.plix.domain.usecase.analytics.CalculateMonthToDateNetUseCase;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ForecastViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private FakeTransactionRepository fakeTransactionRepository;
    private ForecastViewModel viewModel;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository = new FakeTransactionRepository();
        viewModel = new ForecastViewModel(fakeAiRepository, fakeTransactionRepository, new CalculateMonthToDateNetUseCase());
    }

    private Result<ForecastResult> okResult(long amount) {
        return new Result.Success<>(new ForecastResult(ForecastResult.Status.OK, amount));
    }

    private TransactionEntity incomeOnFirstDayOfThisMonth(long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = "user-1";
        entity.amount = amount;
        entity.type = "income";
        entity.occurredAt = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        entity.updatedAt = entity.occurredAt;
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void init_doesNotCallRepositoryUntilLoadForecast() {
        assertNull(viewModel.getForecastState().getValue());
        assertEquals(0, fakeAiRepository.getForecastCallCount);
    }

    @Test
    public void loadForecast_showsLoadingThenSuccess() {
        viewModel.loadForecast();
        assertTrue(viewModel.getForecastState().getValue() instanceof UiState.Loading);

        fakeAiRepository.completeForecast(okResult(10_450_000L));

        UiState<ForecastUiModel> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Success);
        ForecastUiModel data = ((UiState.Success<ForecastUiModel>) state).data;
        assertEquals(10_450_000L, data.forecastNetAmount);
        assertEquals("10.450.000đ", data.forecastNetAmountText);
    }

    @Test
    public void loadForecast_success_includesMonthToDateNetSeries() {
        List<TransactionEntity> transactions = new ArrayList<>();
        transactions.add(incomeOnFirstDayOfThisMonth(10_000_000L));
        fakeTransactionRepository.allOnceResult = new Result.Success<>(transactions);

        viewModel.loadForecast();
        fakeAiRepository.completeForecast(okResult(10_450_000L));

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        UiState<ForecastUiModel> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Success);
        ForecastUiModel data = ((UiState.Success<ForecastUiModel>) state).data;
        assertEquals(today.getDayOfMonth(), data.actualNetByDay.size());
        assertEquals(10_000_000L, (long) data.actualNetByDay.get(0));
        assertEquals(10_000_000L, (long) data.actualNetByDay.get(data.actualNetByDay.size() - 1));
        assertEquals(YearMonth.from(today).lengthOfMonth(), data.daysInMonth);
    }

    @Test
    public void loadForecast_insufficientData_becomesEmptyNotError() {
        viewModel.loadForecast();
        fakeAiRepository.completeForecast(new Result.Success<>(new ForecastResult(ForecastResult.Status.INSUFFICIENT_DATA, null)));

        assertTrue(viewModel.getForecastState().getValue() instanceof UiState.Empty);
        assertEquals(0, fakeTransactionRepository.getAllOnceCallCount); //thiếu dữ liệu thì không cần đọc giao dịch để vẽ
    }

    @Test
    public void loadForecast_error_becomesErrorWithMessage() {
        viewModel.loadForecast();
        fakeAiRepository.completeForecast(new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", null));

        UiState<ForecastUiModel> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không có kết nối mạng", ((UiState.Error<ForecastUiModel>) state).message);
    }

    @Test
    public void loadForecast_readingTransactionsFails_becomesErrorWithMessage() {
        fakeTransactionRepository.allOnceResult = new Result.Error<>(ErrorType.UNKNOWN, "Không đọc được dữ liệu", null);

        viewModel.loadForecast();
        fakeAiRepository.completeForecast(okResult(10_450_000L));

        UiState<ForecastUiModel> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không đọc được dữ liệu", ((UiState.Error<ForecastUiModel>) state).message);
    }

    @Test
    public void loadForecast_whileLoading_doesNotCallRepositoryAgain() {
        viewModel.loadForecast();
        viewModel.loadForecast();

        assertEquals(1, fakeAiRepository.getForecastCallCount);
    }

    @Test
    public void loadForecast_afterCompleted_canLoadAgain() {
        viewModel.loadForecast();
        fakeAiRepository.completeForecast(okResult(1_000_000L));
        viewModel.loadForecast();

        assertEquals(2, fakeAiRepository.getForecastCallCount);
        assertTrue(viewModel.getForecastState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void onCleared_cancelsPendingForecast() {
        viewModel.onCleared();

        assertTrue(fakeAiRepository.cancelForecastCalled);
    }
}