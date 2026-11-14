package com.longvuong.plix.data.repository;

import com.longvuong.plix.core.error.RepositoryCallback;

import java.util.List;

public interface ReceiptRepository {

    //Tạo tệp ảnh tạm receipt_<millis>.jpg trong cache và trả URI + token
    void createCapture(RepositoryCallback<ReceiptCaptureTarget> callback);

    //Đọc chữ trên ảnh (ảnh vừa chụp hoặc ảnh chọn từ thư viện), trả danh sách dòng theo thứ tự trên xuống
    void recognize(String uriString, RepositoryCallback<List<String>> callback);

    //Xoá ảnh tạm theo token; token không còn hoặc đã xoá thì bỏ qua
    void deleteCapture(String token);
}