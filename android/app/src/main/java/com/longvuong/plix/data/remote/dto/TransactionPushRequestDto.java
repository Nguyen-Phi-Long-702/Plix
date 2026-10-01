package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TransactionPushRequestDto {
    @SerializedName("records")
    public final List<TransactionSyncRecordDto> records;

    public TransactionPushRequestDto(List<TransactionSyncRecordDto> records) {
        this.records = records;
    }
}