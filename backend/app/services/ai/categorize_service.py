from typing import Optional, Tuple

import asyncpg

from app.services.ai.model_store import load_model_params
from app.services.ai.train_seed_model import SEED_USER_ID


class NoModelAvailableError(Exception):
    pass 

async def classify_note(
    pool: asyncpg.Pool, user_id: str, note: str
) -> Tuple[Optional[str], float]:
    """Trả về (category_id, confidence) gợi ý cho `note` của user_id."""
    model_params = await load_model_params(pool, user_id)
    if model_params is None:
        model_params = await load_model_params(pool, SEED_USER_ID)
    if model_params is None:
        raise NoModelAvailableError(user_id)

    predicted_category_name, confidence = model_params.classifier.predict(note)

    row = await pool.fetchrow(
        """
        SELECT id FROM categories
        WHERE is_deleted = false
          AND (user_id IS NULL OR user_id = $1)
          AND name = $2
        LIMIT 1
        """,
        user_id,
        predicted_category_name,
    )
    category_id = row["id"] if row is not None else None
    return category_id, confidence