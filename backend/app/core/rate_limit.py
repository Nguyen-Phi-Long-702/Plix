import asyncpg

from app.core.config import RETRAIN_COOLDOWN_SECONDS

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