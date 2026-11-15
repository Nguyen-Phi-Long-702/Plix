package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ReceiptCaptureTarget;
import com.longvuong.plix.data.repository.ReceiptRepository;
import com.longvuong.plix.domain.usecase.ocr.ReceiptParseResult;
import com.longvuong.plix.domain.usecase.ocr.ReceiptTextParser;
import com.longvuong.plix.presentation.common.UiState;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

//Máy trạng thái của màn Quét hoá đơn (AI-08): xin đích chụp ảnh, đọc chữ, trích số tiền/ngày/cửa hàng và xoá ảnh tạm đúng lúc (SEC-10).
//Không log nội dung OCR, số tiền hay đường dẫn ảnh
@HiltViewModel
public class ReceiptScanViewModel extends ViewModel {
    private static final String NO_AMOUNT_MESSAGE = "Không đọc được số tiền trên hoá đơn";
    private static final String TYPE_EXPENSE = "expense"; //hoá đơn luôn là khoản chi

    private final ReceiptRepository receiptRepository;
    private final ReceiptTextParser receiptTextParser;

    //null = chưa quét; Loading = đang đọc hoá đơn; Success = giá trị điền sẵn cho form; Error = không đọc được số tiền hoặc lỗi (màn hiện thêm nút Nhập tay)
    private final MutableLiveData<UiState<ReceiptPrefill>> scanState = new MutableLiveData<>();
    //Đích để Fragment mở máy ảnh (URI + token); null = không có yêu cầu chụp đang chờ. Fragment gọi onCaptureTargetConsumed() sau khi mở máy ảnh
    private final MutableLiveData<ReceiptCaptureTarget> captureTarget = new MutableLiveData<>();

    //token của ảnh chụp đang giữ (chưa xoá); null = không giữ ảnh nào. Ảnh chọn từ thư viện không có token nên không bao giờ bị xoá
    @Nullable
    private String heldCaptureToken;

    @Inject
    public ReceiptScanViewModel(ReceiptRepository receiptRepository, ReceiptTextParser receiptTextParser) {
        this.receiptRepository = receiptRepository;
        this.receiptTextParser = receiptTextParser;
    }

    public LiveData<UiState<ReceiptPrefill>> getScanState() {
        return scanState;
    }

    public LiveData<ReceiptCaptureTarget> getCaptureTarget() {
        return captureTarget;
    }

    //true khi lần quét gần nhất lỗi: màn hiện nút Nhập tay để mở form trống
    public boolean canEnterManually() {
        return scanState.getValue() instanceof UiState.Error;
    }

    //Người dùng chạm Chụp ảnh: tạo tệp ảnh tạm và đưa đích cho Fragment mở máy ảnh
    public void onCaptureRequested() {
        receiptRepository.createCapture(this::onCreateCaptureResult);
    }

    //Fragment đã mở máy ảnh xong: bỏ đích để không mở lại máy ảnh khi màn hình xoay hoặc được tạo lại
    public void onCaptureTargetConsumed() {
        captureTarget.setValue(null);
    }

    public void onImageCaptured(String token, String uriString) {
        heldCaptureToken = token;
        recognize(uriString);
    }

    public void onImagePicked(String uriString) {
        recognize(uriString);
    }

    //Người dùng thoát máy ảnh mà không chụp: xoá tệp ảnh tạm đã tạo
    public void onCaptureCancelled() {
        deleteHeldCapture();
    }

    private void onCreateCaptureResult(Result<ReceiptCaptureTarget> result) {
        if (result instanceof Result.Error) {
            scanState.setValue(new UiState.Error<>(((Result.Error<ReceiptCaptureTarget>) result).message));
            return;
        }
        ReceiptCaptureTarget target = ((Result.Success<ReceiptCaptureTarget>) result).data;
        heldCaptureToken = target.token;
        captureTarget.setValue(target);
    }

    private void recognize(String uriString) {
        scanState.setValue(new UiState.Loading<>());
        receiptRepository.recognize(uriString, this::onRecognizeResult);
    }

    private void onRecognizeResult(Result<List<String>> result) {
        deleteHeldCapture(); //đọc xong (thành công hay lỗi) là xoá ảnh chụp ngay
        if (result instanceof Result.Error) {
            scanState.setValue(new UiState.Error<>(((Result.Error<List<String>>) result).message));
            return;
        }
        List<String> lines = ((Result.Success<List<String>>) result).data;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        ReceiptParseResult parsed = receiptTextParser.parse(lines, today);
        if (parsed.totalAmount == null) {
            scanState.setValue(new UiState.Error<>(NO_AMOUNT_MESSAGE));
            return;
        }
        LocalDate date = parsed.date != null ? parsed.date : today;
        long occurredAt = date.atStartOfDay(zone).toInstant().toEpochMilli();
        boolean lowConfidence = parsed.totalSource == ReceiptParseResult.TotalSource.FALLBACK_MAX;
        scanState.setValue(new UiState.Success<>(
                new ReceiptPrefill(parsed.totalAmount, TYPE_EXPENSE, occurredAt, parsed.storeName, lowConfidence)));
    }

    //Xoá ảnh chụp đang giữ (nếu có) đúng 1 lần rồi bỏ token
    private void deleteHeldCapture() {
        if (heldCaptureToken == null) {
            return;
        }
        String token = heldCaptureToken;
        heldCaptureToken = null;
        receiptRepository.deleteCapture(token);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        deleteHeldCapture();
    }
}