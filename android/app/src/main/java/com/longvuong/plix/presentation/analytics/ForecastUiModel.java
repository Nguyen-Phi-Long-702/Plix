package com.longvuong.plix.presentation.analytics;

import java.util.List;

public class ForecastUiModel {
    public final long forecastNetAmount; //NET thu - chi dự kiến cuối tháng (VNĐ, có thể âm); dùng làm điểm cuối của đường dự kiến
    public final String forecastNetAmountText; //số tiền đã format sẵn để hiển thị, vd "10.450.000đ"
    public final List<Long> actualNetByDay; //NET tích luỹ thực tế từ ngày 1 đến hôm nay; phần tử thứ i = ngày i + 1
    public final int daysInMonth; //số ngày của tháng hiện tại, dùng làm điểm cuối trục X

    public ForecastUiModel(long forecastNetAmount, String forecastNetAmountText, List<Long> actualNetByDay, int daysInMonth) {
        this.forecastNetAmount = forecastNetAmount;
        this.forecastNetAmountText = forecastNetAmountText;
        this.actualNetByDay = actualNetByDay;
        this.daysInMonth = daysInMonth;
    }
}