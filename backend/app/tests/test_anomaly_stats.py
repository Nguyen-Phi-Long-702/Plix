from app.services.ai.anomaly_stats import (
    METHOD_IQR,
    METHOD_MEAN_STD,
    STATUS_FLAGGED_HIGH,
    STATUS_FLAGGED_LOW,
    STATUS_NORMAL,
    detect_anomaly,
)

# 10 mẫu (>= 10 -> mean/std): mean = 45.000, std = 5.000
# -> ngưỡng cao = 55.000, ngưỡng thấp = 35.000
TEN_AMOUNTS = [40000] * 5 + [50000] * 5

# 5 mẫu (5-9 -> IQR): Q1 = 45.000, trung vị = 50.000, Q3 = 55.000, IQR = 10.000
# -> ngưỡng cao = Q3 + 1,5 x IQR = 70.000, ngưỡng thấp = Q1 - 1,5 x IQR = 30.000
FIVE_AMOUNTS = [40000, 45000, 50000, 55000, 60000]


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


def test_amount_within_thresholds_is_normal_with_mean_std():
    result = detect_anomaly(TEN_AMOUNTS, 45000)

    assert result.status == STATUS_NORMAL
    assert result.method == METHOD_MEAN_STD
    assert result.center == 45000


def test_iqr_amount_above_upper_threshold_is_flagged_high():
    result = detect_anomaly(FIVE_AMOUNTS, 80000)

    assert result.status == STATUS_FLAGGED_HIGH
    assert result.method == METHOD_IQR
    assert result.center == 50000


def test_iqr_amount_below_lower_threshold_is_flagged_low():
    result = detect_anomaly(FIVE_AMOUNTS, 20000)

    assert result.status == STATUS_FLAGGED_LOW
    assert result.method == METHOD_IQR
    assert result.center == 50000


def test_iqr_amount_within_thresholds_is_normal():
    result = detect_anomaly(FIVE_AMOUNTS, 50000)

    assert result.status == STATUS_NORMAL
    assert result.method == METHOD_IQR
    assert result.center == 50000