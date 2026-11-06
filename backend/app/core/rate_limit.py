import time
from math import ceil

import asyncpg

from app.core.config import EXPORT_COOLDOWN_SECONDS, RETRAIN_COOLDOWN_SECONDS

class RetrainRateLimitedError(Exception):
    def __init__(self, retry_after_seconds: int):
        self.retry_after_seconds = retry_after_seconds
        super().__init__(
            f"Còn phải đợi {retry_after_seconds} giây trước khi được retrain lại"
        )

async def check_retrain_cooldown(pool: asyncpg.Pool, user_id: str) -> None:
    row = await pool.fetchrow(
        """
        SELECT EXTRACT(EPOCH FROM (now() - trained_at))::int AS elapsed_seconds
        FROM ai_model_params
        WHERE user_id = $1
        """,
        user_id,
    )
    if row is None:
        return

    elapsed_seconds = row["elapsed_seconds"]
    if elapsed_seconds < RETRAIN_COOLDOWN_SECONDS:
        raise RetrainRateLimitedError(RETRAIN_COOLDOWN_SECONDS - elapsed_seconds)


class ExportRateLimitedError(Exception):
    def __init__(self, retry_after_seconds: int):
        self.retry_after_seconds = retry_after_seconds
        super().__init__(
            f"Còn phải đợi {retry_after_seconds} giây trước khi được xuất dữ liệu lại"
        )

# Thời điểm xuất gần nhất của từng user, lưu trong bộ nhớ tiến trình (không thêm cột DB cho tính năng Should Have);
# backend khởi động lại thì danh sách này rỗng, chỉ làm giới hạn nới ra, không gây lỗi.
_last_export_at: dict[str, float] = {}

def check_export_cooldown(user_id: str) -> None:
    now = time.monotonic()
    last_export_at = _last_export_at.get(user_id)
    if last_export_at is not None:
        elapsed_seconds = now - last_export_at
        if elapsed_seconds < EXPORT_COOLDOWN_SECONDS:
            raise ExportRateLimitedError(ceil(EXPORT_COOLDOWN_SECONDS - elapsed_seconds))
    _last_export_at[user_id] = now