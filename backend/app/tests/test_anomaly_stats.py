from app.services.ai.anomaly_stats import (
    METHOD_MEAN_STD,
    STATUS_FLAGGED_HIGH,
    STATUS_FLAGGED_LOW,
    detect_anomaly,
)

# 10 mẫu (>= 10 -> mean/std): mean = 45.000, std = 5.000
# -> ngưỡng cao = 55.000, ngưỡng thấp = 35.000
TEN_AMOUNTS = [40000] * 5 + [50000] * 5


def test_amount_above_upper_threshold_is_flagged_high():
    result = detect_anomaly(TEN_AMOUNTS, 60000)

    assert result.status == STATUS_FLAGGED_HIGH
    assert result.method == METHOD_MEAN_STD
    assert result.center == 45000


def test_amount_below_lower_threshold_is_flagged_low():
    result = detect_anomaly(TEN_AMOUNTS, 30000)

    assert result.status == STATUS_FLAGGED_LOW
    assert result.method == METHOD_MEAN_STD
    assert result.center == 45000