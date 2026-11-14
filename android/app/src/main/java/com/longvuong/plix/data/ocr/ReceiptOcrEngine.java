package com.longvuong.plix.data.ocr;

import com.longvuong.plix.core.error.RepositoryCallback;

import java.util.List;

//Giao diện đọc chữ từ ảnh hoá đơn; cài đặt chạy hoàn toàn trên thiết bị (SEC-10)
public interface ReceiptOcrEngine {

    //imageUriString: URI dạng chuỗi của ảnh (FileProvider hoặc content:// từ thư viện); callback có thể được gọi trên luồng bất kỳ
    void recognize(String imageUriString, RepositoryCallback<List<String>> callback);
}