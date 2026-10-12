package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ParseNlItem;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class QuickAddNlViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private QuickAddNlViewModel viewModel;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        viewModel = new QuickAddNlViewModel(fakeAiRepository);
    }

    private Result<List<ParseNlItem>> twoItemsResult() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, null, 0.9f));
        items.add(new ParseNlItem(35_000L, null, 0.9f));
        return new Result.Success<>(items);
    }

    @Test
    public void init_doesNotCallRepositoryUntilSubmit() {
        assertNull(viewModel.getParseState().getValue());
        assertEquals(0, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_blankText_becomesErrorAndDoesNotCallRepository() {
        viewModel.submit("   ");

        UiState<String> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Vui lòng nhập nội dung cần phân tích", ((UiState.Error<String>) state).message);
        assertEquals(0, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_trimsTextAndShowsLoading() {
        viewModel.submit("  phở 50k, grab 35k  ");

        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Loading);
        assertEquals(1, fakeAiRepository.parseNlCallCount);
        assertEquals("phở 50k, grab 35k", fakeAiRepository.lastParseNlText);
    }

    @Test
    public void submit_success_becomesSuccessWithSummaryMessage() {
        viewModel.submit("phở 50k, grab 35k");
        fakeAiRepository.completeParseNl(twoItemsResult());

        UiState<String> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Success);
        assertEquals("Đã nhận diện 2 giao dịch", ((UiState.Success<String>) state).data);
    }

    @Test
    public void submit_error_becomesErrorWithMessage() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", null));

        UiState<String> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không có kết nối mạng", ((UiState.Error<String>) state).message);
    }

    @Test
    public void submit_whileLoading_doesNotCallRepositoryAgain() {
        viewModel.submit("phở 50k");
        viewModel.submit("phở 50k");

        assertEquals(1, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_afterCompleted_canSubmitAgain() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(twoItemsResult());
        viewModel.submit("grab 35k");

        assertEquals(2, fakeAiRepository.parseNlCallCount);
        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Loading);
    }
}