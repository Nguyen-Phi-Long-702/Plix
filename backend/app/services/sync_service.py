from typing import List

import asyncpg

from app.models.sync import RejectedRecord, SyncPushResponse, TransactionSyncRecord

# Upsert 1 giao dịch theo id. Quy tắc last-write-wins (Master Plan Mục 18.3):
# chỉ ghi đè khi incoming.updated_at LỚN HƠN HẲN bản đang lưu (so sánh `<`
# nghiêm ngặt, không phải `<=`) — trùng updated_at thì bản đang lưu thắng.
# Điều kiện user_id đảm bảo không bao giờ ghi đè giao dịch của user khác.
# RETURNING id chỉ trả về dòng khi thật sự có INSERT hoặc UPDATE; nếu bị bỏ qua
# (WHERE sai) thì không trả gì. Không có lệnh DELETE nào — không xoá vật lý.
_UPSERT_TRANSACTION_SQL = """
INSERT INTO transactions
    (id, user_id, amount, type, category_id, note, payment_method,
     occurred_at, is_recurring, recurrence_rule, recurrence_parent_id,
     updated_at, is_deleted)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
ON CONFLICT (id) DO UPDATE SET
    amount = EXCLUDED.amount,
    type = EXCLUDED.type,
    category_id = EXCLUDED.category_id,
    note = EXCLUDED.note,
    payment_method = EXCLUDED.payment_method,
    occurred_at = EXCLUDED.occurred_at,
    is_recurring = EXCLUDED.is_recurring,
    recurrence_rule = EXCLUDED.recurrence_rule,
    recurrence_parent_id = EXCLUDED.recurrence_parent_id,
    updated_at = EXCLUDED.updated_at,
    is_deleted = EXCLUDED.is_deleted
WHERE transactions.user_id = EXCLUDED.user_id
  AND transactions.updated_at < EXCLUDED.updated_at
RETURNING id
"""

_FIND_TRANSACTION_OWNER_SQL = "SELECT user_id FROM transactions WHERE id = $1"


async def push_transactions(
    pool: asyncpg.Pool,
    user_id: str,
    records: List[TransactionSyncRecord],
) -> SyncPushResponse:
    """Upsert từng giao dịch trong batch lên Postgres. `user_id` luôn lấy từ JWT
    (do router truyền vào), không bao giờ lấy từ dữ liệu client gửi lên.

    Kết quả từng record:
    - Được INSERT/UPDATE (mới, hoặc updated_at lớn hơn bản đang lưu) -> upserted_ids.
    - Bị bỏ qua vì bản đang lưu thắng (cùng user, updated_at bằng hoặc cũ hơn)
      -> vẫn nằm trong upserted_ids: server đã xử lý xong, client đánh dấu
      synced; gửi lại cùng batch không gây lỗi, không tạo trùng.
    - Bị bỏ qua vì id đã thuộc user khác -> rejected (forbidden), không làm
      fail cả batch.
    """
    upserted_ids: List[str] = []
    rejected: List[RejectedRecord] = []

    for record in records:
        written_id = await pool.fetchval(
            _UPSERT_TRANSACTION_SQL,
            record.id,
            user_id,
            record.amount,
            record.type,
            record.category_id,
            record.note,
            record.payment_method,
            record.occurred_at,
            record.is_recurring,
            record.recurrence_rule,
            record.recurrence_parent_id,
            record.updated_at,
            record.is_deleted,
        )
        if written_id is not None:
            upserted_ids.append(record.id)
            continue

        owner_id = await pool.fetchval(_FIND_TRANSACTION_OWNER_SQL, record.id)
        if owner_id == user_id:
            upserted_ids.append(record.id)
        else:
            rejected.append(RejectedRecord(id=record.id, reason="forbidden"))

    return SyncPushResponse(upserted_ids=upserted_ids, rejected=rejected)