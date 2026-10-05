import asyncio

from app.models.sync import SyncTable
from app.services.sync_service import SYNC_TABLES, pull_records, push_records

USER_1 = "user-1"
USER_2 = "user-2"


class InMemorySyncPool:
    """Giả lập Postgres cho đúng 1 bảng đồng bộ. Mô phỏng lại quy tắc của câu
    upsert trong sync_service._build_spec: id chưa có -> thêm mới; id đã có thì
    chỉ ghi đè khi cùng user_id VÀ updated_at mới LỚN HƠN HẲN, ngược lại bỏ qua
    (RETURNING không trả gì). Không bao giờ xoá dòng nào.
    Bản thân câu SQL được bảo vệ bởi các test chuỗi SQL trong test_sync_service.py."""

    def __init__(self, table):
        self.columns = SYNC_TABLES[table].columns
        self.rows = {}  # id -> {"user_id", <cột riêng>, "updated_at", "is_deleted"}

    async def fetchrow(self, query, *args):
        row = self.rows.get(args[0])
        return None if row is None else {"user_id": row["user_id"]}

    async def fetchval(self, query, *args):
        if query.lstrip().startswith("INSERT"):
            return self._upsert(args)
        if "SELECT EXISTS" in query:
            user_id, max_updated_at = args
            return any(
                row["user_id"] == user_id and row["updated_at"] > max_updated_at
                for row in self.rows.values()
            )
        row = self.rows.get(args[0])  # tra chủ sở hữu theo id
        return None if row is None else row["user_id"]

    async def fetch(self, query, *args):
        user_id, since, limit = args
        wanted = sorted(
            (
                (record_id, row)
                for record_id, row in self.rows.items()
                if row["user_id"] == user_id and row["updated_at"] >= since
            ),
            key=lambda item: (item[1]["updated_at"], item[0]),
        )
        keys = ("updated_at", "is_deleted") + self.columns
        return [
            dict(id=record_id, **{key: row[key] for key in keys})
            for record_id, row in wanted[:limit]
        ]

    def _upsert(self, args):
        record_id, user_id = args[0], args[1]
        values = dict(zip(self.columns, args[2:-2]))
        row = dict(user_id=user_id, updated_at=args[-2], is_deleted=args[-1], **values)
        existing = self.rows.get(record_id)
        if existing is None or (
            existing["user_id"] == user_id and existing["updated_at"] < row["updated_at"]
        ):
            self.rows[record_id] = row
            return record_id
        return None


# Các trường riêng bắt buộc của mỗi bảng (đúng Sync Payload Schema).
_EXTRA_FIELDS = {
    SyncTable.transactions: {"amount": 50000, "occurred_at": 1790000000000},
    SyncTable.categories: {"name": "Cafe"},
    SyncTable.budgets: {"period": "2026-10", "limit_amount": 2000000},
    SyncTable.goals: {
        "name": "Mua laptop",
        "target_amount": 20000000,
        "deadline": 1800000000000,
    },
    SyncTable.corrections: {
        "transaction_id": "tx-1",
        "corrected_category_id": "sys_an_uong",
        "created_at": 1790000000000,
    },
}


def _record(table, updated_at, is_deleted=False, record_id="rec-1"):
    return SYNC_TABLES[table].record_model(
        id=record_id,
        updated_at=updated_at,
        is_deleted=is_deleted,
        **_EXTRA_FIELDS[table],
    )


def _push(pool, table, user_id, *records):
    return asyncio.run(push_records(pool, table, user_id, list(records)))


def _pull(pool, table, user_id, since=0):
    return asyncio.run(pull_records(pool, table, user_id, since, 500))


def test_tombstone_push_keeps_the_row_and_marks_it_deleted_for_every_table():
    for table in SyncTable:
        pool = InMemorySyncPool(table)
        _push(pool, table, USER_1, _record(table, 1000))

        response = _push(pool, table, USER_1, _record(table, 2000, is_deleted=True))

        assert response.upserted_ids == ["rec-1"], table
        assert response.rejected == [], table
        assert list(pool.rows) == ["rec-1"], table
        assert pool.rows["rec-1"]["is_deleted"] is True, table
        assert pool.rows["rec-1"]["updated_at"] == 2000, table


def test_tombstone_of_a_record_never_pushed_before_is_stored_as_tombstone():
    for table in SyncTable:
        pool = InMemorySyncPool(table)

        response = _push(pool, table, USER_1, _record(table, 2000, is_deleted=True))

        assert response.upserted_ids == ["rec-1"], table
        assert pool.rows["rec-1"]["is_deleted"] is True, table


def test_pushing_the_same_payload_twice_is_idempotent_for_every_table():
    for table in SyncTable:
        for is_deleted in (False, True):
            pool = InMemorySyncPool(table)
            record = _record(table, 2000, is_deleted=is_deleted)

            first = _push(pool, table, USER_1, record)
            stored_after_first = dict(pool.rows["rec-1"])
            second = _push(pool, table, USER_1, record)

            label = (table, is_deleted)
            assert first.upserted_ids == ["rec-1"], label
            assert second.upserted_ids == ["rec-1"], label
            assert first.rejected == [] and second.rejected == [], label
            assert list(pool.rows) == ["rec-1"], label
            assert pool.rows["rec-1"] == stored_after_first, label


def test_older_or_equal_live_payload_cannot_resurrect_a_tombstone_for_every_table():
    for table in SyncTable:
        pool = InMemorySyncPool(table)
        _push(pool, table, USER_1, _record(table, 2000, is_deleted=True))

        for stale_updated_at in (1000, 2000):
            response = _push(pool, table, USER_1, _record(table, stale_updated_at))

            label = (table, stale_updated_at)
            assert response.upserted_ids == ["rec-1"], label
            assert response.rejected == [], label
            assert pool.rows["rec-1"]["is_deleted"] is True, label
            assert pool.rows["rec-1"]["updated_at"] == 2000, label


def test_user_cannot_tombstone_a_record_owned_by_another_user_for_every_table():
    for table in SyncTable:
        pool = InMemorySyncPool(table)
        _push(pool, table, USER_1, _record(table, 1000))

        response = _push(pool, table, USER_2, _record(table, 3000, is_deleted=True))

        assert response.upserted_ids == [], table
        assert [(item.id, item.reason) for item in response.rejected] == [
            ("rec-1", "forbidden")
        ], table
        assert pool.rows["rec-1"]["user_id"] == USER_1, table
        assert pool.rows["rec-1"]["is_deleted"] is False, table
        assert pool.rows["rec-1"]["updated_at"] == 1000, table


def test_pull_returns_the_tombstone_after_a_delete_push_for_every_table():
    for table in SyncTable:
        pool = InMemorySyncPool(table)
        _push(pool, table, USER_1, _record(table, 1000))
        _push(pool, table, USER_1, _record(table, 2000, is_deleted=True))

        # since so sánh >=, nên mốc đúng bằng updated_at của tombstone vẫn trả về nó.
        for since in (0, 2000):
            response = _pull(pool, table, USER_1, since=since)

            label = (table, since)
            assert [
                (item.id, item.updated_at, item.is_deleted) for item in response.records
            ] == [("rec-1", 2000, True)], label
            assert response.has_more is False, label

        assert _pull(pool, table, USER_2).records == [], table


def test_transaction_pointing_to_a_tombstoned_category_is_still_accepted_and_pulled():
    # Thiết bị A xoá category; thiết bị B (chưa pull) vẫn đẩy giao dịch dùng category đó.
    # Server không có khoá ngoại và không kiểm tra category_id, nên phải nhận bình thường.
    category_pool = InMemorySyncPool(SyncTable.categories)
    transaction_pool = InMemorySyncPool(SyncTable.transactions)
    _push(category_pool, SyncTable.categories, USER_1, _record(SyncTable.categories, 1000))
    _push(
        category_pool,
        SyncTable.categories,
        USER_1,
        _record(SyncTable.categories, 2000, is_deleted=True),
    )
    transaction = SYNC_TABLES[SyncTable.transactions].record_model(
        id="tx-1",
        updated_at=3000,
        is_deleted=False,
        amount=50000,
        occurred_at=1790000000000,
        category_id="rec-1",
    )

    response = _push(transaction_pool, SyncTable.transactions, USER_1, transaction)

    assert response.upserted_ids == ["tx-1"]
    assert response.rejected == []
    pulled_transactions = _pull(transaction_pool, SyncTable.transactions, USER_1)
    assert [(item.id, item.category_id) for item in pulled_transactions.records] == [
        ("tx-1", "rec-1")
    ]
    pulled_categories = _pull(category_pool, SyncTable.categories, USER_1)
    assert [(item.id, item.is_deleted) for item in pulled_categories.records] == [
        ("rec-1", True)
    ]