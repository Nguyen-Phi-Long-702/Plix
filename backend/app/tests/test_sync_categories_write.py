import asyncio

from app.models.sync import CategorySyncRecord, SyncTable
from app.services.sync_service import push_records


class FakeCategoryPool:
    """Giả lập asyncpg.Pool cho push bảng categories.
    - owners: id -> user_id đang lưu trong Postgres (None = category hệ thống);
      id không có trong owners = chưa tồn tại.
    - skipped_by_tie_breaker: các id mà upsert không ghi gì (bản đang lưu thắng).
    Ghi lại mọi lần upsert để kiểm tra upsert có bị gọi hay không."""

    def __init__(self, owners, skipped_by_tie_breaker=()):
        self.owners = owners
        self.skipped = set(skipped_by_tie_breaker)
        self.upsert_args = []

    async def fetchrow(self, query, *args):
        record_id = args[0]
        if record_id not in self.owners:
            return None
        return {"user_id": self.owners[record_id]}

    async def fetchval(self, query, *args):
        if "INSERT INTO categories" in query:
            self.upsert_args.append(args)
            return None if args[0] in self.skipped else args[0]
        return self.owners.get(args[0])


def _category(record_id="cat-1", updated_at=2000, name="Cafe"):
    return CategorySyncRecord(
        id=record_id, updated_at=updated_at, is_deleted=False, name=name
    )


def test_new_category_is_created_for_the_jwt_user():
    pool = FakeCategoryPool(owners={})

    response = asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("cat-1")])
    )

    assert response.upserted_ids == ["cat-1"]
    assert response.rejected == []


def test_category_upsert_receives_user_id_from_argument_and_fields_in_order():
    pool = FakeCategoryPool(owners={})

    asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("cat-1", 2000)])
    )

    assert pool.upsert_args == [
        (
            "cat-1",  # id
            "user-1",  # user_id lấy từ tham số (JWT), không nằm trong record
            "Cafe",  # name
            "expense",  # type (mặc định)
            2000,  # updated_at
            False,  # is_deleted
        )
    ]


def test_own_existing_category_is_upserted():
    pool = FakeCategoryPool(owners={"cat-1": "user-1"})

    response = asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("cat-1")])
    )

    assert response.upserted_ids == ["cat-1"]
    assert response.rejected == []
    assert [args[0] for args in pool.upsert_args] == ["cat-1"]


def test_own_category_skipped_by_tie_breaker_is_still_reported_as_upserted():
    pool = FakeCategoryPool(
        owners={"cat-1": "user-1"}, skipped_by_tie_breaker=["cat-1"]
    )

    response = asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("cat-1")])
    )

    assert response.upserted_ids == ["cat-1"]
    assert response.rejected == []


def test_system_category_is_rejected_without_attempting_upsert():
    pool = FakeCategoryPool(owners={"sys_an_uong": None})

    response = asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("sys_an_uong")])
    )

    assert response.upserted_ids == []
    assert [(item.id, item.reason) for item in response.rejected] == [
        ("sys_an_uong", "forbidden")
    ]
    assert pool.upsert_args == []


def test_category_of_another_user_is_rejected_without_attempting_upsert():
    pool = FakeCategoryPool(owners={"cat-1": "user-2"})

    response = asyncio.run(
        push_records(pool, SyncTable.categories, "user-1", [_category("cat-1")])
    )

    assert response.upserted_ids == []
    assert [(item.id, item.reason) for item in response.rejected] == [
        ("cat-1", "forbidden")
    ]
    assert pool.upsert_args == []


def test_rejected_category_does_not_block_other_records_in_the_same_batch():
    pool = FakeCategoryPool(owners={"cat-2": "user-2"})

    response = asyncio.run(
        push_records(
            pool,
            SyncTable.categories,
            "user-1",
            [_category("cat-1"), _category("cat-2", name="Xang"), _category("cat-3", name="An vat")],
        )
    )

    assert response.upserted_ids == ["cat-1", "cat-3"]
    assert [item.id for item in response.rejected] == ["cat-2"]
    assert [args[0] for args in pool.upsert_args] == ["cat-1", "cat-3"]