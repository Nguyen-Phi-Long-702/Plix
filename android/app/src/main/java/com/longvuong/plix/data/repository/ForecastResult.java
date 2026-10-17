package com.longvuong.plix.data.repository;

import androidx.annotation.Nullable;

public class ForecastResult {
    public enum Status {
        OK,
        INSUFFICIENT_DATA
    }

    public final Status status;

    @Nullable
    public final Long forecastNetAmount; //NET thu - chi dự kiến cuối tháng (VNĐ, có thể âm); luôn có khi OK, null khi INSUFFICIENT_DATA

    public ForecastResult(Status status, @Nullable Long forecastNetAmount) {
        this.status = status;
        this.forecastNetAmount = forecastNetAmount;
    }
}