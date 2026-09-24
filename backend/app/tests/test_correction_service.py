import asyncio

import pytest

from app.services.ai.correction_service import TransactionNotOwnedError, save_correction


class FakeCorrectionPool:
    """Giả lập asyncpg.Pool cho correction_service: fetchrow tra transaction
    theo id, execute chỉ ghi lại tham số đã insert để assert trong test."""

    def __init__(self, transaction_rows):
        self._transaction_rows = transaction_rows
        self.inserted = []
        self.fetchrow_queries = []

    async def fetchrow(self, query, *args):
        self.fetchrow_queries.append(query)
        (transaction_id,) = args
        return self._transaction_rows.get(transaction_id)

    async def execute(self, query, *args):
        self.inserted.append(args)


def test_saves_correction_when_transaction_belongs_to_user():
    pool = FakeCorrectionPool(transaction_rows={"tx-1": {"user_id": "user-1"}})

    correction_id = asyncio.run(
        save_correction(pool, "user-1", "tx-1", "cat-predicted", "cat-corrected")
    )

    assert correction_id
    assert len(pool.inserted) == 1
    saved_id, user_id, transaction_id, predicted_id, corrected_id, timestamp_ms = pool.inserted[0]
    assert saved_id == correction_id
    assert user_id == "user-1"
    assert transaction_id == "tx-1"
    assert predicted_id == "cat-predicted"
    assert corrected_id == "cat-corrected"
    assert isinstance(timestamp_ms, int)


def test_allows_missing_predicted_category_id():
    pool = FakeCorrectionPool(transaction_rows={"tx-1": {"user_id": "user-1"}})

    asyncio.run(save_correction(pool, "user-1", "tx-1", None, "cat-corrected"))

    predicted_id = pool.inserted[0][3]
    assert predicted_id is None


def test_raises_when_transaction_belongs_to_another_user():
    pool = FakeCorrectionPool(transaction_rows={"tx-2": {"user_id": "user-2"}})

    with pytest.raises(TransactionNotOwnedError):
        asyncio.run(save_correction(pool, "user-1", "tx-2", None, "cat-corrected"))

    assert pool.inserted == []


def test_raises_when_transaction_does_not_exist():
    pool = FakeCorrectionPool(transaction_rows={})

    with pytest.raises(TransactionNotOwnedError):
        asyncio.run(save_correction(pool, "user-1", "tx-missing", None, "cat-corrected"))

    assert pool.inserted == []


def test_transaction_lookup_query_filters_soft_deleted_transactions():
    pool = FakeCorrectionPool(transaction_rows={"tx-1": {"user_id": "user-1"}})

    asyncio.run(save_correction(pool, "user-1", "tx-1", None, "cat-corrected"))

    assert len(pool.fetchrow_queries) == 1
    query = pool.fetchrow_queries[0]
    assert "FROM transactions" in query
    assert "is_deleted = false" in query