package com.longvuong.plix.data.repository;

//Đích để ứng dụng máy ảnh ghi ảnh hoá đơn vào: URI chuỗi (cho TakePicture) và token (để xoá ảnh sau khi dùng)
public final class ReceiptCaptureTarget {

    public final String uriString;
    public final String token;

    public ReceiptCaptureTarget(String uriString, String token) {
        this.uriString = uriString;
        this.token = token;
    }
}