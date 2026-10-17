package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ForecastResponseDto {
    @SerializedName("status")
    public String status;

    @Nullable
    @SerializedName("forecast_net_amount")
    public Double forecastNetAmount; //NET thu - chi dự kiến cuối tháng (VNĐ, có thể âm); không có khi status = insufficient_data
}