import asyncio

from app.models.sync import TransactionSyncRecord
from app.services.sync_service import _UPSERT_TRANSACTION_SQL, push_transactions


class FakeSyncPool:
    """Giả lập asyncpg.Pool cho sync_service: fetchval trả lần lượt các giá trị
    đã sắp sẵn (đúng thứ tự các lần gọi), đồng thời ghi lại mọi (query, args)."""

    def __init__(self, fetchval_results):
        self._results = list(fetchval_results)
        self.calls = []

    async def fetchval(self, query, *args):
        self.calls.append((query, args))
        return self._results.pop(0)


def _record(record_id="tx-1", updated_at=2000):
    return TransactionSyncRecord(
        id=record_id,
        updated_at=updated_at,
        is_deleted=False,
        amount=50000,
        occurred_at=1790000000000,
    )


def test_record_written_by_upsert_is_reported_as_upserted():
    pool = FakeSyncPool(fetchval_results=["tx-1"])

    response = asyncio.run(push_transactions(pool, "user-1", [_record("tx-1")]))

    assert response.upserted_ids == ["tx-1"]
    assert response.rejected == []
    assert len(pool.calls) == 1


def test_upsert_receives_user_id_from_argument_and_record_fields_in_order():
    pool = FakeSyncPool(fetchval_results=["tx-1"])

    asyncio.run(push_transactions(pool, "user-1", [_record("tx-1", updated_at=2000)]))

    _, args = pool.calls[0]
    assert args == (
        "tx-1",  # id
        "user-1",  # user_id lấy từ tham số (JWT), không nằm trong record
        50000,  # amount
        "expense",  # type (mặc định)
        None,  # category_id
        "",  # note (mặc định)
        None,  # payment_method
        1790000000000,  # occurred_at
        False,  # is_recurring
        None,  # recurrence_rule
        None,  # recurrence_parent_id
        2000,  # updated_at
        False,  # is_deleted
    )


def test_record_skipped_by_tie_breaker_but_owned_by_same_user_is_still_upserted():
    # Upsert không ghi gì (bản đang lưu thắng) -> None; tra chủ sở hữu = chính user này.
    pool = FakeSyncPool(fetchval_results=[None, "user-1"])

    response = asyncio.run(push_transactions(pool, "user-1", [_record("tx-1")]))

    assert response.upserted_ids == ["tx-1"]
    assert response.rejected == []
    owner_query, owner_args = pool.calls[1]
    assert "FROM transactions" in owner_query
    assert owner_args == ("tx-1",)


def test_record_whose_id_belongs_to_another_user_is_rejected_as_forbidden():
    pool = FakeSyncPool(fetchval_results=[None, "user-2"])

    response = asyncio.run(push_transactions(pool, "user-1", [_record("tx-1")]))

    assert response.upserted_ids == []
    assert len(response.rejected) == 1
    assert response.rejected[0].id == "tx-1"
    assert response.rejected[0].reason == "forbidden"


def test_forbidden_record_does_not_block_other_records_in_the_same_batch():
    # tx-1 ghi được; tx-2 thuộc user khác; tx-3 ghi được.
    pool = FakeSyncPool(fetchval_results=["tx-1", None, "user-2", "tx-3"])

    response = asyncio.run(
        push_transactions(
            pool, "user-1", [_record("tx-1"), _record("tx-2"), _record("tx-3")]
        )
    )

    assert response.upserted_ids == ["tx-1", "tx-3"]
    assert [item.id for item in response.rejected] == ["tx-2"]


def test_upsert_sql_uses_strict_greater_than_and_never_deletes():
    assert "transactions.updated_at < EXCLUDED.updated_at" in _UPSERT_TRANSACTION_SQL
    assert "<=" not in _UPSERT_TRANSACTION_SQL
    assert "transactions.user_id = EXCLUDED.user_id" in _UPSERT_TRANSACTION_SQL
    assert "DELETE FROM" not in _UPSERT_TRANSACTION_SQL.upper()