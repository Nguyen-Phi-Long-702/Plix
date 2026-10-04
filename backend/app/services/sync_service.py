from dataclasses import dataclass
from typing import Dict, List, Tuple, Type

import asyncpg

from app.models.sync import (
    BudgetSyncRecord,
    CategorySyncRecord,
    CorrectionSyncRecord,
    GoalSyncRecord,
    RejectedRecord,
    SyncPullResponse,
    SyncPushResponse,
    SyncRecordBase,
    SyncTable,
    TransactionSyncRecord,
)


@dataclass(frozen=True)
class SyncTableSpec:
    """Mô tả 1 bảng tham gia đồng bộ. `columns` là các cột RIÊNG của bảng,
    không gồm id, user_id, updated_at, is_deleted (4 cột này bảng nào cũng có).
    Tên field của record_model trùng đúng tên cột trong Postgres."""

    table: SyncTable
    record_model: Type[SyncRecordBase]
    columns: Tuple[str, ...]
    upsert_sql: str
    owner_sql: str
    pull_sql: str
    has_more_sql: str


def _build_spec(
    table: SyncTable,
    record_model: Type[SyncRecordBase],
    columns: Tuple[str, ...],
) -> SyncTableSpec:
    """Dựng 4 câu SQL cho 1 bảng. Tên bảng và tên cột chỉ lấy từ enum SyncTable
    và danh sách cột viết cứng bên dưới, không bao giờ từ dữ liệu client gửi lên.

    upsert_sql — Quy tắc last-write-wins: chỉ ghi đè khi
    incoming.updated_at LỚN HƠN HẲN bản đang lưu (so sánh `<` nghiêm ngặt, không
    phải `<=`) — trùng updated_at thì bản đang lưu thắng. Điều kiện user_id đảm
    bảo không bao giờ ghi đè bản ghi của user khác. RETURNING id chỉ trả về dòng
    khi thật sự có INSERT hoặc UPDATE; nếu bị bỏ qua (WHERE sai) thì không trả
    gì. Không có lệnh DELETE nào — không xoá vật lý.

    has_more_sql — Sync Payload Schema, mục 4: true nếu còn bản ghi có
    updated_at LỚN HƠN updated_at lớn nhất của batch vừa trả về.
    """
    name = table.value
    all_columns = ("id", "user_id") + columns + ("updated_at", "is_deleted")
    insert_columns = ", ".join(all_columns)
    placeholders = ", ".join("$%d" % index for index in range(1, len(all_columns) + 1))
    assignments = ",\n    ".join(
        "%s = EXCLUDED.%s" % (column, column)
        for column in columns + ("updated_at", "is_deleted")
    )
    select_columns = ", ".join(("id", "updated_at", "is_deleted") + columns)

    upsert_sql = f"""
INSERT INTO {name}
    ({insert_columns})
VALUES ({placeholders})
ON CONFLICT (id) DO UPDATE SET
    {assignments}
WHERE {name}.user_id = EXCLUDED.user_id
  AND {name}.updated_at < EXCLUDED.updated_at
RETURNING id
"""
    pull_sql = f"""
SELECT {select_columns}
FROM {name}
WHERE user_id = $1 AND updated_at >= $2
ORDER BY updated_at ASC, id ASC
LIMIT $3
"""
    has_more_sql = f"""
SELECT EXISTS (
    SELECT 1 FROM {name}
    WHERE user_id = $1 AND updated_at > $2
)
"""
    return SyncTableSpec(
        table=table,
        record_model=record_model,
        columns=columns,
        upsert_sql=upsert_sql,
        owner_sql=f"SELECT user_id FROM {name} WHERE id = $1",
        pull_sql=pull_sql,
        has_more_sql=has_more_sql,
    )


# Nguồn duy nhất cho 5 bảng đồng bộ — khoá là enum SyncTable, nên bảng nào
# ngoài enum đều không có route. Cột lấy từ Master Plan Mục 14.3.
SYNC_TABLES: Dict[SyncTable, SyncTableSpec] = {
    spec.table: spec
    for spec in (
        _build_spec(
            SyncTable.transactions,
            TransactionSyncRecord,
            (
                "amount",
                "type",
                "category_id",
                "note",
                "payment_method",
                "occurred_at",
                "is_recurring",
                "recurrence_rule",
                "recurrence_parent_id",
            ),
        ),
        _build_spec(SyncTable.categories, CategorySyncRecord, ("name", "type")),
        _build_spec(
            SyncTable.budgets,
            BudgetSyncRecord,
            ("period", "category_id", "limit_amount", "threshold_percent"),
        ),
        _build_spec(
            SyncTable.goals,
            GoalSyncRecord,
            ("name", "target_amount", "current_amount", "deadline"),
        ),
        _build_spec(
            SyncTable.corrections,
            CorrectionSyncRecord,
            (
                "transaction_id",
                "predicted_category_id",
                "corrected_category_id",
                "created_at",
            ),
        ),
    )
}


async def push_records(
    pool: asyncpg.Pool,
    table: SyncTable,
    user_id: str,
    records: List[SyncRecordBase],
) -> SyncPushResponse:
    """Upsert từng bản ghi trong batch của 1 bảng lên Postgres. `user_id` luôn
    lấy từ JWT (do router truyền vào), không bao giờ lấy từ dữ liệu client gửi lên.

    Kết quả từng record:
    - Được INSERT/UPDATE (mới, hoặc updated_at lớn hơn bản đang lưu) -> upserted_ids.
    - Bị bỏ qua vì bản đang lưu thắng (cùng user, updated_at bằng hoặc cũ hơn)
      -> vẫn nằm trong upserted_ids: server đã xử lý xong, client đánh dấu
      synced; gửi lại cùng batch không gây lỗi, không tạo trùng.
    - Bị bỏ qua vì id đã thuộc user khác -> rejected (forbidden), không làm
      fail cả batch.
    """
    spec = SYNC_TABLES[table]
    upserted_ids: List[str] = []
    rejected: List[RejectedRecord] = []

    for record in records:
        written_id = await pool.fetchval(
            spec.upsert_sql,
            record.id,
            user_id,
            *[getattr(record, column) for column in spec.columns],
            record.updated_at,
            record.is_deleted,
        )
        if written_id is not None:
            upserted_ids.append(record.id)
            continue

        owner_id = await pool.fetchval(spec.owner_sql, record.id)
        if owner_id == user_id:
            upserted_ids.append(record.id)
        else:
            rejected.append(RejectedRecord(id=record.id, reason="forbidden"))

    return SyncPushResponse(upserted_ids=upserted_ids, rejected=rejected)


async def pull_records(
    pool: asyncpg.Pool,
    table: SyncTable,
    user_id: str,
    since: int,
    limit: int,
) -> SyncPullResponse:
    """Trả tối đa `limit` bản ghi của user có updated_at >= since (kể cả
    tombstone), sắp xếp tăng dần theo updated_at, kèm cờ has_more.
    `user_id` luôn lấy từ JWT (do router truyền vào)."""
    spec = SYNC_TABLES[table]
    response_model = SyncPullResponse[spec.record_model]

    rows = await pool.fetch(spec.pull_sql, user_id, since, limit)
    records = [spec.record_model(**dict(row)) for row in rows]

    if not records:
        return response_model(records=[], has_more=False)

    max_updated_at = records[-1].updated_at
    has_more = await pool.fetchval(spec.has_more_sql, user_id, max_updated_at)
    return response_model(records=records, has_more=has_more)