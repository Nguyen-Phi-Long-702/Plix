package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class AnomalyResponseDto {
    @SerializedName("status")
    public String status;

    @Nullable
    @SerializedName("explanation")
    public String explanation; //có thể không có khi status = insufficient_data
}