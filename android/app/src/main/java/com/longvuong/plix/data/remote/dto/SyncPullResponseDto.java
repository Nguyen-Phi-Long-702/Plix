package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//Kết quả kéo dữ liệu chung cho mọi bảng, D là DTO bản ghi của bảng đó
public class SyncPullResponseDto<D> {
    @SerializedName("records")
    public final List<D> records;

    @SerializedName("has_more")
    public final boolean hasMore;

    public SyncPullResponseDto(List<D> records, boolean hasMore) {
        this.records = records;
        this.hasMore = hasMore;
    }
}