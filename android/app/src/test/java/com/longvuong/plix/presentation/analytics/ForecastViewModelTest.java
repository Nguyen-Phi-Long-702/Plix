package com.longvuong.plix.presentation.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ForecastResult;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ForecastViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private ForecastViewModel viewModel;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        viewModel = new ForecastViewModel(fakeAiRepository);
    }

    private Result<ForecastResult> okResult(long amount) {
        return new Result.Success<>(new ForecastResult(ForecastResult.Status.OK, amount));
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

        UiState<ForecastResult> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Success);
        assertEquals(Long.valueOf(10_450_000L), ((UiState.Success<ForecastResult>) state).data.forecastNetAmount);
    }

    @Test
    public void loadForecast_insufficientData_becomesEmptyNotError() {
        viewModel.loadForecast();
        fakeAiRepository.completeForecast(new Result.Success<>(new ForecastResult(ForecastResult.Status.INSUFFICIENT_DATA, null)));

        assertTrue(viewModel.getForecastState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void loadForecast_error_becomesErrorWithMessage() {
        viewModel.loadForecast();
        fakeAiRepository.completeForecast(new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", null));

        UiState<ForecastResult> state = viewModel.getForecastState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không có kết nối mạng", ((UiState.Error<ForecastResult>) state).message);
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