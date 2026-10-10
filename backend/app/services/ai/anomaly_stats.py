import statistics
from dataclasses import dataclass
from typing import List, Optional, Tuple
import asyncpg

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

UNKNOWN_CATEGORY_NAME = "không xác định"
INSUFFICIENT_DATA_EXPLANATION = (
    "Chưa đủ dữ liệu để đánh giá bất thường cho danh mục này "
    f"(cần ít nhất {MIN_SAMPLES_FOR_IQR} giao dịch đã đồng bộ)."
)

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


def _format_vnd(value: float) -> str:
    return f"{round(value):,}".replace(",", ".") + "đ"


def _build_explanation(result: AnomalyResult, category_name: str, amount: int) -> str:
    if result.status == STATUS_NORMAL:
        return f"Giao dịch này nằm trong mức chi tiêu thông thường của danh mục [{category_name}] của bạn."

    direction = "cao hơn" if result.status == STATUS_FLAGGED_HIGH else "thấp hơn"
    if result.method == METHOD_MEAN_STD:
        level_phrase, center_label = "trung bình", "trung bình"
    else:
        level_phrase, center_label = "thông thường", "trung vị"
    return (
        f"Giao dịch này {direction} mức chi tiêu {level_phrase} cho danh mục [{category_name}] của bạn "
        f"({center_label} {_format_vnd(result.center)}, giao dịch này {_format_vnd(amount)})."
    )


async def check_anomaly(
    pool: asyncpg.Pool, user_id: str, category_id: str, amount: int
) -> Tuple[str, str]:
    rows = await pool.fetch(
        """
        SELECT amount FROM transactions
        WHERE user_id = $1
          AND category_id = $2
          AND is_deleted = false
        """,
        user_id,
        category_id,
    )
    result = detect_anomaly([row["amount"] for row in rows], amount)
    if result.status == STATUS_INSUFFICIENT_DATA:
        return result.status, INSUFFICIENT_DATA_EXPLANATION

    category_row = await pool.fetchrow(
        """
        SELECT name FROM categories
        WHERE id = $1
          AND is_deleted = false
          AND (user_id IS NULL OR user_id = $2)
        """,
        category_id,
        user_id,
    )
    category_name = category_row["name"] if category_row is not None else UNKNOWN_CATEGORY_NAME
    return result.status, _build_explanation(result, category_name, amount)