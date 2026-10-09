package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//Body đẩy dữ liệu chung cho mọi bảng, D là DTO bản ghi của bảng đó
public class SyncPushRequestDto<D> {
    @SerializedName("records")
    public final List<D> records;

    public SyncPushRequestDto(List<D> records) {
        this.records = records;
    }
}