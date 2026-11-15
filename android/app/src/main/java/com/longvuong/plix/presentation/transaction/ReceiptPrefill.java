package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;

//Giá trị điền sẵn cho form Thêm giao dịch sau khi quét hoá đơn (AI-08). Form chỉ điền sẵn để người dùng kiểm tra, không tự lưu (AI-11)
public final class ReceiptPrefill {

    private final long amount;
    private final String type; //luôn là "expense"
    private final long occurredAt; //đầu ngày (mili giây) của ngày trên hoá đơn, hoặc của hôm nay nếu hoá đơn không có ngày
    @Nullable
    private final String note; //tên cửa hàng, null khi không tìm được
    private final boolean lowConfidence; //true = số tiền chưa chắc chắn (không có dòng tổng, lấy số lớn nhất)

    public ReceiptPrefill(long amount, String type, long occurredAt, @Nullable String note, boolean lowConfidence) {
        this.amount = amount;
        this.type = type;
        this.occurredAt = occurredAt;
        this.note = note;
        this.lowConfidence = lowConfidence;
    }

    public long getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    @Nullable
    public String getNote() {
        return note;
    }

    public boolean isLowConfidence() {
        return lowConfidence;
    }
}