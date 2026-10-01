package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TransactionPullResponseDto {
    @SerializedName("records")
    public final List<TransactionSyncRecordDto> records;

    @SerializedName("has_more")
    public final boolean hasMore;

    public TransactionPullResponseDto(List<TransactionSyncRecordDto> records, boolean hasMore) {
        this.records = records;
        this.hasMore = hasMore;
    }
}