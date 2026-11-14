package com.longvuong.plix.data.repository;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.ocr.ReceiptImageStore;
import com.longvuong.plix.data.ocr.ReceiptOcrEngine;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

//Điều phối ảnh tạm và đọc chữ hoá đơn; không ghi log nội dung hoá đơn hay đường dẫn ảnh (SEC-10)
@Singleton
public class ReceiptRepositoryImpl implements ReceiptRepository {

    private final ReceiptImageStore imageStore;
    private final ReceiptOcrEngine ocrEngine;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    //token -> tệp ảnh chụp đang giữ; chỉ xoá được tệp do createCapture tạo, token không đoán ra đường dẫn
    private final Map<String, File> captureFiles = new ConcurrentHashMap<>();

    @Inject
    public ReceiptRepositoryImpl(ReceiptImageStore imageStore, ReceiptOcrEngine ocrEngine,
                                 AppExecutors appExecutors, ErrorMapper errorMapper) {
        this.imageStore = imageStore;
        this.ocrEngine = ocrEngine;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
    }

    @Override
    public void createCapture(RepositoryCallback<ReceiptCaptureTarget> callback) {
        appExecutors.diskIO().execute(() -> {
            Result<ReceiptCaptureTarget> result;
            try {
                File file = imageStore.newCaptureFile();
                String uriString = imageStore.uriFor(file).toString();
                String token = file.getName();
                captureFiles.put(token, file);
                result = new Result.Success<>(new ReceiptCaptureTarget(uriString, token));
            } catch (Exception e) {
                result = errorMapper.mapThrowable(e);
            }
            Result<ReceiptCaptureTarget> finalResult = result;
            appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    @Override
    public void recognize(String uriString, RepositoryCallback<List<String>> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                //engine trả kết quả bất đồng bộ; luôn chuyển về luồng chính trước khi báo cho ViewModel
                ocrEngine.recognize(uriString, result -> appExecutors.mainThread().execute(() -> callback.onResult(result)));
            } catch (Exception e) {
                Result<List<String>> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void deleteCapture(String token) {
        File file = captureFiles.remove(token);
        if (file == null) {
            return;
        }
        appExecutors.diskIO().execute(() -> imageStore.delete(file));
    }
}