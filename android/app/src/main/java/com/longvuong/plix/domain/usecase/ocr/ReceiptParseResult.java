package com.longvuong.plix.domain.usecase.ocr;

import java.time.LocalDate;

//Kết quả phân tích văn bản hoá đơn; Java thuần, không phụ thuộc Android
public class ReceiptParseResult {
    public enum TotalSource {
        KEYWORD,      //tổng tiền lấy từ dòng có từ khoá tổng
        FALLBACK_MAX, //không có từ khoá tổng, lấy số tiền lớn nhất trên hoá đơn (chưa chắc chắn)
        NONE          //không đọc được số tiền nào
    }

    public final Long totalAmount;       //null khi totalSource = NONE
    public final TotalSource totalSource;
    public final LocalDate date;         //null khi không có ngày hợp lệ
    public final String storeName;       //null khi không tìm được tên cửa hàng

    public ReceiptParseResult(Long totalAmount, TotalSource totalSource, LocalDate date, String storeName) {
        this.totalAmount = totalAmount;
        this.totalSource = totalSource;
        this.date = date;
        this.storeName = storeName;
    }
}