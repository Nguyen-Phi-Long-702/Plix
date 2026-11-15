package com.longvuong.plix.presentation.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ReceiptCaptureTarget;
import com.longvuong.plix.data.repository.ReceiptRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Bản giả của ReceiptRepository cho test ViewModel quét hoá đơn: trả ngay kết quả đã đặt sẵn và ghi lại các token đã bị xoá
class FakeReceiptRepository implements ReceiptRepository {
    static final String DEFAULT_TOKEN = "receipt_1.jpg";
    static final String DEFAULT_URI = "content://fake/receipts/receipt_1.jpg";

    Result<ReceiptCaptureTarget> createCaptureResult =
            new Result.Success<>(new ReceiptCaptureTarget(DEFAULT_URI, DEFAULT_TOKEN));
    Result<List<String>> recognizeResult = new Result.Success<>(Collections.emptyList());

    //các token đã gọi deleteCapture, theo thứ tự gọi
    final List<String> deletedTokens = new ArrayList<>();

    @Override
    public void createCapture(RepositoryCallback<ReceiptCaptureTarget> callback) {
        callback.onResult(createCaptureResult);
    }

    @Override
    public void recognize(String uriString, RepositoryCallback<List<String>> callback) {
        callback.onResult(recognizeResult);
    }

    @Override
    public void deleteCapture(String token) {
        deletedTokens.add(token);
    }
}