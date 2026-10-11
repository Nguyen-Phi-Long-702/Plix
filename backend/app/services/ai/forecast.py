from datetime import date, datetime, timedelta, timezone
from typing import List, Optional, Sequence, Tuple

import asyncpg

from app.services.ai.anomaly_stats import STATUS_INSUFFICIENT_DATA

ALPHA = 0.3
MIN_WEEKS_OF_HISTORY = 4

STATUS_OK = "ok"

TYPE_INCOME = "income"
TYPE_EXPENSE = "expense"

# Giờ Việt Nam (UTC+7): dùng để xác định ranh giới tuần/tháng.
VN_TZ = timezone(timedelta(hours=7))
MS_PER_WEEK = 7 * 24 * 60 * 60 * 1000

# (type, amount, occurred_at) của 1 giao dịch; occurred_at là epoch ms.
Transaction = Tuple[str, int, int]


def _signed_amount(tx_type: str, amount: int) -> Optional[int]:
    """Thu cộng, chi trừ. Loại khác (không có trong app) bị bỏ qua."""
    if tx_type == TYPE_INCOME:
        return amount
    if tx_type == TYPE_EXPENSE:
        return -amount
    return None


def _week_start(epoch_ms: int) -> date:
    """Ngày thứ Hai (giờ VN) của tuần chứa mốc thời gian này."""
    local_date = datetime.fromtimestamp(epoch_ms / 1000, VN_TZ).date()
    return local_date - timedelta(days=local_date.weekday())


def _month_of(epoch_ms: int) -> str:
    return datetime.fromtimestamp(epoch_ms / 1000, VN_TZ).strftime("%Y-%m")


def _month_bounds_ms(month: str) -> Tuple[int, int]:
    """[đầu tháng, đầu tháng sau) theo giờ VN, đơn vị epoch ms. month dạng YYYY-MM."""
    year, month_number = int(month[:4]), int(month[5:7])
    start = datetime(year, month_number, 1, tzinfo=VN_TZ)
    if month_number == 12:
        end = datetime(year + 1, 1, 1, tzinfo=VN_TZ)
    else:
        end = datetime(year, month_number + 1, 1, tzinfo=VN_TZ)
    return round(start.timestamp() * 1000), round(end.timestamp() * 1000)


def weekly_net_series(transactions: Sequence[Transaction], now_ms: int) -> List[int]:
    """Chuỗi NET (thu - chi) theo tuần, từ cũ đến mới, chỉ gồm các tuần ĐÃ TRỌN VẸN
    (tuần hiện tại chưa hết nên không đưa vào). Chuỗi chạy liên tục từ tuần có giao
    dịch đầu tiên; tuần không có giao dịch tính NET = 0. Chưa có tuần nào: trả []."""
    current_week_start = _week_start(now_ms)
    net_by_week = {}
    for tx_type, amount, occurred_at in transactions:
        week_start = _week_start(occurred_at)
        signed = _signed_amount(tx_type, amount)
        if week_start >= current_week_start or signed is None:
            continue
        net_by_week[week_start] = net_by_week.get(week_start, 0) + signed
    if not net_by_week:
        return []

    series = []
    week_start = min(net_by_week)
    while week_start < current_week_start:
        series.append(net_by_week.get(week_start, 0))
        week_start += timedelta(days=7)
    return series


def exponential_smoothing(series: Sequence[int], alpha: float = ALPHA) -> float:
    """Dự báo NET của tuần kế tiếp: forecast = alpha x NET tuần gần nhất
    + (1 - alpha) x forecast tuần trước. Forecast khởi đầu = NET tuần đầu tiên."""
    forecast = float(series[0])
    for net in series[1:]:
        forecast = alpha * net + (1 - alpha) * forecast
    return forecast


def compute_forecast_net(
    transactions: Sequence[Transaction], month: str, now_ms: int
) -> Optional[int]:
    """NET dự kiến của `month` = NET thực tế từ đầu tháng đến hiện tại
    + NET dự báo mỗi tuần x số tuần còn lại của tháng (tính theo ngày/7).
    Dưới MIN_WEEKS_OF_HISTORY tuần trọn vẹn: trả None (chưa đủ dữ liệu)."""
    series = weekly_net_series(transactions, now_ms)
    if len(series) < MIN_WEEKS_OF_HISTORY:
        return None

    month_start_ms, month_end_ms = _month_bounds_ms(month)
    actual_net = 0
    for tx_type, amount, occurred_at in transactions:
        signed = _signed_amount(tx_type, amount)
        if signed is None:
            continue
        if month_start_ms <= occurred_at < month_end_ms and occurred_at <= now_ms:
            actual_net += signed

    remaining_ms = max(0, month_end_ms - max(now_ms, month_start_ms))
    remaining_weeks = remaining_ms / MS_PER_WEEK
    return round(actual_net + exponential_smoothing(series) * remaining_weeks)


async def check_forecast(
    pool: asyncpg.Pool, user_id: str, month: Optional[str], now_ms: int
) -> Tuple[str, Optional[int]]:
    rows = await pool.fetch(
        """
        SELECT type, amount, occurred_at FROM transactions
        WHERE user_id = $1
          AND is_deleted = false
        """,
        user_id,
    )
    transactions = [(row["type"], row["amount"], row["occurred_at"]) for row in rows]
    forecast_net = compute_forecast_net(transactions, month or _month_of(now_ms), now_ms)
    if forecast_net is None:
        return STATUS_INSUFFICIENT_DATA, None
    return STATUS_OK, forecast_net