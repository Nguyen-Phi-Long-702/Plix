package com.longvuong.plix.presentation.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class ExportViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeExportRepository fakeExportRepository;
    private ExportViewModel viewModel;

    @Before
    public void setUp() {
        fakeExportRepository = new FakeExportRepository();
        viewModel = new ExportViewModel(fakeExportRepository);
    }

    @Test
    public void init_stateIsEmptyAndRepositoryNotCalled() {
        assertTrue(viewModel.getExportState().getValue() instanceof UiState.Empty);
        assertEquals(0, fakeExportRepository.exportCallCount);
    }

    @Test
    public void exportCsv_showsLoadingThenSuccessWithFile() {
        File file = new File("plix-giao-dich-2026-11-09.csv");

        viewModel.exportCsv();
        assertTrue(viewModel.getExportState().getValue() instanceof UiState.Loading);

        fakeExportRepository.complete(new Result.Success<>(file));

        UiState<File> state = viewModel.getExportState().getValue();
        assertTrue(state instanceof UiState.Success);
        assertSame(file, ((UiState.Success<File>) state).data);
    }

    @Test
    public void exportCsv_error_showsServerMessage() {
        String message = "Vui lòng đợi thêm khoảng 42 phút trước khi xuất dữ liệu lại";

        viewModel.exportCsv();
        fakeExportRepository.complete(new Result.Error<>(ErrorType.SERVER, message, null));

        UiState<File> state = viewModel.getExportState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals(message, ((UiState.Error<File>) state).message);
    }

    @Test
    public void exportCsv_whileLoading_doesNotCallRepositoryAgain() {
        viewModel.exportCsv();
        viewModel.exportCsv();

        assertEquals(1, fakeExportRepository.exportCallCount);
    }

    @Test
    public void onExportHandled_resetsStateToEmpty() {
        viewModel.exportCsv();
        fakeExportRepository.complete(new Result.Success<>(new File("a.csv")));

        viewModel.onExportHandled();

        assertTrue(viewModel.getExportState().getValue() instanceof UiState.Empty);
    }
}