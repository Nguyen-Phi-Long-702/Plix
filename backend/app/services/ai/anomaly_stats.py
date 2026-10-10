import statistics
from dataclasses import dataclass
from typing import List, Optional

MIN_SAMPLES_FOR_IQR = 5
MIN_SAMPLES_FOR_MEAN_STD = 10
STD_MULTIPLIER = 2
IQR_MULTIPLIER = 1.5

STATUS_INSUFFICIENT_DATA = "insufficient_data"
STATUS_NORMAL = "normal"
STATUS_FLAGGED_HIGH = "flagged_high"
STATUS_FLAGGED_LOW = "flagged_low"

METHOD_MEAN_STD = "mean_std"
METHOD_IQR = "iqr"


@dataclass(frozen=True)
class AnomalyResult:
    status: str
    method: Optional[str]
    center: Optional[float]


def detect_anomaly(amounts: List[int], amount: int) -> AnomalyResult:
    """Hàm thuần: so `amount` với các `amounts` đã có của cùng (user, category).
    < 5 mẫu: insufficient_data; 5-9 mẫu: median + IQR; >= 10 mẫu: mean + std.
    Kiểm tra cả 2 chiều: cao bất thường và thấp bất thường."""
    sample_count = len(amounts)
    if sample_count < MIN_SAMPLES_FOR_IQR:
        return AnomalyResult(STATUS_INSUFFICIENT_DATA, None, None)

    if sample_count >= MIN_SAMPLES_FOR_MEAN_STD:
        method = METHOD_MEAN_STD
        center = statistics.fmean(amounts)
        std = statistics.pstdev(amounts)
        upper_bound = center + STD_MULTIPLIER * std
        lower_bound = center - STD_MULTIPLIER * std
    else:
        method = METHOD_IQR
        q1, center, q3 = statistics.quantiles(amounts, n=4, method="inclusive")
        iqr = q3 - q1
        upper_bound = q3 + IQR_MULTIPLIER * iqr
        lower_bound = q1 - IQR_MULTIPLIER * iqr

    if amount > upper_bound:
        return AnomalyResult(STATUS_FLAGGED_HIGH, method, center)
    if amount < lower_bound:
        return AnomalyResult(STATUS_FLAGGED_LOW, method, center)
    return AnomalyResult(STATUS_NORMAL, method, center)