import time
import uuid
from typing import Optional

import asyncpg


class TransactionNotOwnedError(Exception):
    """Raise khi transaction_id không tồn tại, đã bị xoá mềm, hoặc không thuộc
    đúng user_id đang gửi yêu cầu correction."""


async def save_correction(
    pool: asyncpg.Pool,
    user_id: str,
    transaction_id: str,
    predicted_category_id: Optional[str],
    corrected_category_id: str,
) -> str:
    """Lưu 1 correction (danh mục gợi ý ban đầu + danh mục người dùng sửa lại)
    sau khi xác nhận transaction_id thuộc đúng user_id đang gửi yêu cầu."""
    transaction_row = await pool.fetchrow(
        "SELECT user_id FROM transactions WHERE id = $1 AND is_deleted = false",
        transaction_id,
    )
    if transaction_row is None or transaction_row["user_id"] != user_id:
        raise TransactionNotOwnedError(transaction_id)

    correction_id = str(uuid.uuid4())
    now_ms = int(time.time() * 1000)
    await pool.execute(
        """
        INSERT INTO corrections
            (id, user_id, transaction_id, predicted_category_id,
             corrected_category_id, created_at, updated_at, is_deleted)
        VALUES ($1, $2, $3, $4, $5, $6, $6, false)
        """,
        correction_id,
        user_id,
        transaction_id,
        predicted_category_id,
        corrected_category_id,
        now_ms,
    )
    return correction_id