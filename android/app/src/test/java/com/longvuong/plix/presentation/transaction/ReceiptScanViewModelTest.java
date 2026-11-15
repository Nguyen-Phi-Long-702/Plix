package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.domain.usecase.ocr.ReceiptTextParser;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReceiptScanViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    //HD1 đúng như DP_02 mục 2.7
    private static final List<String> HD1 = Arrays.asList(
            "HIGHLANDS COFFEE",
            "DC: 12 Le Loi, Q1",
            "08/11/2026 09:15",
            "Ca phe sua da 45.000",
            "Banh mi 30.000",
            "Tam tinh 75.000",
            "Phi dich vu 60.000",
            "Tong cong 135.000",
            "Thanh toan 135.000");

    private FakeReceiptRepository fakeReceiptRepository;
    private ReceiptScanViewModel viewModel;

    @Before
    public void setUp() {
        fakeReceiptRepository = new FakeReceiptRepository();
        viewModel = new ReceiptScanViewModel(fakeReceiptRepository, new ReceiptTextParser());
    }

    private static long startOfDayMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * TN2-U20
     * OCR thành công với HD1: state = Success(amount 135000, type 'expense', occurredAt đầu ngày 2026-11-08, note 'HIGHLANDS COFFEE'); deleteCapture được gọi đúng 1 lần.
     */
    @Test
    public void onImageCaptured_hd1_emitsSuccessAndDeletesCaptureOnce() {
        fakeReceiptRepository.recognizeResult = new Result.Success<>(HD1);

        viewModel.onCaptureRequested();
        viewModel.onImageCaptured(FakeReceiptRepository.DEFAULT_TOKEN, FakeReceiptRepository.DEFAULT_URI);

        UiState<ReceiptPrefill> state = viewModel.getScanState().getValue();
        assertTrue(state instanceof UiState.Success);
        ReceiptPrefill prefill = ((UiState.Success<ReceiptPrefill>) state).data;
        assertEquals(135_000L, prefill.getAmount());
        assertEquals("expense", prefill.getType());
        assertEquals(startOfDayMillis(LocalDate.of(2026, 11, 8)), prefill.getOccurredAt());
        assertEquals("HIGHLANDS COFFEE", prefill.getNote());
        assertFalse(prefill.isLowConfidence()); //HD1 có dòng Tổng cộng nên số tiền chắc chắn
        assertEquals(Collections.singletonList(FakeReceiptRepository.DEFAULT_TOKEN), fakeReceiptRepository.deletedTokens);
    }

    /**
     * TN2-U21
     * OCR không đọc được số tiền (['CUA HANG ABC']): state = Error('Không đọc được số tiền trên hoá đơn') kèm canEnterManually = true; ảnh tạm bị xoá.
     */
    @Test
    public void onImageCaptured_noAmountFound_emitsErrorAllowsManualEntryAndDeletesCapture() {
        fakeReceiptRepository.recognizeResult = new Result.Success<>(Collections.singletonList("CUA HANG ABC"));

        viewModel.onCaptureRequested();
        viewModel.onImageCaptured(FakeReceiptRepository.DEFAULT_TOKEN, FakeReceiptRepository.DEFAULT_URI);

        UiState<ReceiptPrefill> state = viewModel.getScanState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không đọc được số tiền trên hoá đơn", ((UiState.Error<ReceiptPrefill>) state).message);
        assertTrue(viewModel.canEnterManually());
        assertEquals(Collections.singletonList(FakeReceiptRepository.DEFAULT_TOKEN), fakeReceiptRepository.deletedTokens);
    }
}