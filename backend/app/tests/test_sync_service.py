import asyncio

from app.models.sync import (
    BudgetSyncRecord,
    CorrectionSyncRecord,
    GoalSyncRecord,
    SyncTable,
    TransactionSyncRecord,
)
from app.services.sync_service import SYNC_TABLES, pull_records, push_records

_TRANSACTIONS_SPEC = SYNC_TABLES[SyncTable.transactions]
_UPSERT_TRANSACTION_SQL = _TRANSACTIONS_SPEC.upsert_sql
_PULL_TRANSACTIONS_SQL = _TRANSACTIONS_SPEC.pull_sql
_HAS_MORE_TRANSACTIONS_SQL = _TRANSACTIONS_SPEC.has_more_sql


def push_transactions(pool, user_id, records):
    return push_records(pool, SyncTable.transactions, user_id, records)


def pull_transactions(pool, user_id, since, limit):
    return pull_records(pool, SyncTable.transactions, user_id, since, limit)


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


class FakePullPool:
    """Giả lập asyncpg.Pool cho pull_transactions: fetch trả sẵn danh sách dòng,
    fetchval trả sẵn kết quả has_more; ghi lại mọi (query, args)."""

    def __init__(self, rows, has_more_result=False):
        self._rows = rows
        self._has_more_result = has_more_result
        self.fetch_calls = []
        self.fetchval_calls = []

    async def fetch(self, query, *args):
        self.fetch_calls.append((query, args))
        return self._rows

    async def fetchval(self, query, *args):
        self.fetchval_calls.append((query, args))
        return self._has_more_result


def _row(record_id="tx-1", updated_at=2000, is_deleted=False):
    return {
        "id": record_id,
        "updated_at": updated_at,
        "is_deleted": is_deleted,
        "amount": 50000,
        "type": "expense",
        "category_id": None,
        "note": "",
        "payment_method": None,
        "occurred_at": 1790000000000,
        "is_recurring": False,
        "recurrence_rule": None,
        "recurrence_parent_id": None,
    }


def test_pull_returns_rows_as_records_in_the_same_order():
    pool = FakePullPool([_row("tx-1", 1000), _row("tx-2", 2000)])

    response = asyncio.run(pull_transactions(pool, "user-1", 0, 500))

    assert [record.id for record in response.records] == ["tx-1", "tx-2"]
    assert [record.updated_at for record in response.records] == [1000, 2000]
    assert response.records[0].amount == 50000


def test_pull_passes_user_id_since_and_limit_to_the_query():
    pool = FakePullPool([_row()])

    asyncio.run(pull_transactions(pool, "user-1", 1500, 200))

    _, args = pool.fetch_calls[0]
    assert args == ("user-1", 1500, 200)


def test_pull_includes_tombstone_records():
    pool = FakePullPool([_row("tx-1", 1000), _row("tx-2", 2000, is_deleted=True)])

    response = asyncio.run(pull_transactions(pool, "user-1", 0, 500))

    assert [record.is_deleted for record in response.records] == [False, True]


def test_pull_with_no_rows_returns_empty_and_skips_has_more_query():
    pool = FakePullPool([])

    response = asyncio.run(pull_transactions(pool, "user-1", 0, 500))

    assert response.records == []
    assert response.has_more is False
    assert pool.fetchval_calls == []


def test_has_more_is_checked_against_max_updated_at_of_the_batch():
    pool = FakePullPool([_row("tx-1", 1000), _row("tx-2", 3000)], has_more_result=True)

    response = asyncio.run(pull_transactions(pool, "user-1", 0, 2))

    assert response.has_more is True
    _, args = pool.fetchval_calls[0]
    assert args == ("user-1", 3000)


def test_has_more_is_false_when_nothing_newer_remains():
    pool = FakePullPool([_row("tx-1", 1000)], has_more_result=False)

    response = asyncio.run(pull_transactions(pool, "user-1", 0, 500))

    assert response.has_more is False


def test_pull_sql_uses_gte_orders_deterministically_and_never_filters_tombstones():
    assert "updated_at >= $2" in _PULL_TRANSACTIONS_SQL
    assert "ORDER BY updated_at ASC, id ASC" in _PULL_TRANSACTIONS_SQL
    assert "LIMIT $3" in _PULL_TRANSACTIONS_SQL
    where_clause = _PULL_TRANSACTIONS_SQL.split("WHERE")[1]
    assert "is_deleted" not in where_clause
    assert "user_id = $1" in where_clause
    assert "user_id" not in _PULL_TRANSACTIONS_SQL.split("FROM")[0]


def test_has_more_sql_uses_strict_greater_than_and_filters_by_user():
    assert "updated_at > $2" in _HAS_MORE_TRANSACTIONS_SQL
    assert "updated_at >= " not in _HAS_MORE_TRANSACTIONS_SQL
    assert "user_id = $1" in _HAS_MORE_TRANSACTIONS_SQL


# ---------------------------------------------------------------------------
# Ngày 31: cơ chế đồng bộ chung cho cả 5 bảng
# ---------------------------------------------------------------------------


def test_sync_tables_cover_exactly_the_five_synced_tables():
    assert {table.value for table in SYNC_TABLES} == {
        "transactions",
        "categories",
        "budgets",
        "goals",
        "corrections",
    }
    assert set(SYNC_TABLES) == set(SyncTable)


def test_every_table_upsert_sql_is_owner_guarded_strict_and_never_deletes():
    for table, spec in SYNC_TABLES.items():
        sql = spec.upsert_sql
        name = table.value
        assert f"INSERT INTO {name}" in sql, name
        assert f"{name}.updated_at < EXCLUDED.updated_at" in sql, name
        assert "<=" not in sql, name
        assert f"{name}.user_id = EXCLUDED.user_id" in sql, name
        assert "DELETE FROM" not in sql.upper(), name


def test_every_table_pull_sql_filters_by_user_and_never_filters_tombstones():
    for table, spec in SYNC_TABLES.items():
        sql = spec.pull_sql
        name = table.value
        assert f"FROM {name}" in sql, name
        assert "updated_at >= $2" in sql, name
        assert "ORDER BY updated_at ASC, id ASC" in sql, name
        assert "LIMIT $3" in sql, name
        where_clause = sql.split("WHERE")[1]
        assert "user_id = $1" in where_clause, name
        assert "is_deleted" not in where_clause, name
        assert "user_id" not in sql.split("FROM")[0], name
        assert "updated_at > $2" in spec.has_more_sql, name
        assert "user_id = $1" in spec.has_more_sql, name


def test_every_table_owner_sql_looks_up_user_id_by_record_id():
    for table, spec in SYNC_TABLES.items():
        assert spec.owner_sql == f"SELECT user_id FROM {table.value} WHERE id = $1"


def test_budget_upsert_receives_user_id_and_record_fields_in_order():
    pool = FakeSyncPool(fetchval_results=["bud-1"])
    record = BudgetSyncRecord(
        id="bud-1",
        updated_at=2000,
        is_deleted=False,
        period="2026-10",
        limit_amount=2000000,
    )

    response = asyncio.run(push_records(pool, SyncTable.budgets, "user-1", [record]))

    assert response.upserted_ids == ["bud-1"]
    _, args = pool.calls[0]
    assert args == (
        "bud-1",  # id
        "user-1",  # user_id lấy từ tham số (JWT), không nằm trong record
        "2026-10",  # period
        None,  # category_id (mặc định)
        2000000,  # limit_amount
        80,  # threshold_percent (mặc định)
        2000,  # updated_at
        False,  # is_deleted
    )


def test_goal_upsert_receives_user_id_and_record_fields_in_order():
    pool = FakeSyncPool(fetchval_results=["goal-1"])
    record = GoalSyncRecord(
        id="goal-1",
        updated_at=2000,
        is_deleted=False,
        name="Mua laptop",
        target_amount=20000000,
        deadline=1800000000000,
    )

    response = asyncio.run(push_records(pool, SyncTable.goals, "user-1", [record]))

    assert response.upserted_ids == ["goal-1"]
    _, args = pool.calls[0]
    assert args == (
        "goal-1",  # id
        "user-1",  # user_id lấy từ tham số (JWT)
        "Mua laptop",  # name
        20000000,  # target_amount
        0,  # current_amount (mặc định)
        1800000000000,  # deadline
        2000,  # updated_at
        False,  # is_deleted
    )


def test_correction_upsert_receives_user_id_and_record_fields_in_order():
    pool = FakeSyncPool(fetchval_results=["cor-1"])
    record = CorrectionSyncRecord(
        id="cor-1",
        updated_at=2000,
        is_deleted=False,
        transaction_id="tx-1",
        corrected_category_id="sys_an_uong",
        created_at=1790000000000,
    )

    response = asyncio.run(
        push_records(pool, SyncTable.corrections, "user-1", [record])
    )

    assert response.upserted_ids == ["cor-1"]
    _, args = pool.calls[0]
    assert args == (
        "cor-1",  # id
        "user-1",  # user_id lấy từ tham số (JWT)
        "tx-1",  # transaction_id
        None,  # predicted_category_id (mặc định)
        "sys_an_uong",  # corrected_category_id
        1790000000000,  # created_at
        2000,  # updated_at
        False,  # is_deleted
    )


def test_budget_owned_by_another_user_is_rejected_as_forbidden():
    pool = FakeSyncPool(fetchval_results=[None, "user-2"])
    record = BudgetSyncRecord(
        id="bud-1",
        updated_at=2000,
        is_deleted=False,
        period="2026-10",
        limit_amount=2000000,
    )

    response = asyncio.run(push_records(pool, SyncTable.budgets, "user-1", [record]))

    assert response.upserted_ids == []
    assert [(item.id, item.reason) for item in response.rejected] == [
        ("bud-1", "forbidden")
    ]
    owner_query, owner_args = pool.calls[1]
    assert "FROM budgets" in owner_query
    assert owner_args == ("bud-1",)


def test_pull_reads_from_the_requested_table_and_keeps_its_own_columns():
    rows = {
        SyncTable.budgets: {
            "id": "bud-1",
            "updated_at": 1000,
            "is_deleted": False,
            "period": "2026-10",
            "category_id": None,
            "limit_amount": 2000000,
            "threshold_percent": 80,
        },
        SyncTable.goals: {
            "id": "goal-1",
            "updated_at": 1000,
            "is_deleted": True,
            "name": "Mua laptop",
            "target_amount": 20000000,
            "current_amount": 500000,
            "deadline": 1800000000000,
        },
        SyncTable.corrections: {
            "id": "cor-1",
            "updated_at": 1000,
            "is_deleted": False,
            "transaction_id": "tx-1",
            "predicted_category_id": None,
            "corrected_category_id": "sys_an_uong",
            "created_at": 999,
        },
    }
    for table, row in rows.items():
        pool = FakePullPool([row])

        response = asyncio.run(pull_records(pool, table, "user-1", 0, 500))

        query, args = pool.fetch_calls[0]
        assert f"FROM {table.value}" in query, table
        assert args == ("user-1", 0, 500), table
        assert len(response.records) == 1, table
        assert response.records[0].id == row["id"], table
        assert response.records[0].is_deleted is row["is_deleted"], table