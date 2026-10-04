from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.config import MAX_PULL_LIMIT
from app.core.security import verify_jwt
from app.routers import sync

# 1 bản ghi đầy đủ trường cho mỗi bảng (đúng Sync Payload Schema, không có
# user_id). Dùng làm body push và cũng là dòng trả về của pull.
_RECORDS = {
    "transactions": {
        "id": "rec-1",
        "updated_at": 2000,
        "is_deleted": False,
        "amount": 50000,
        "type": "expense",
        "category_id": None,
        "note": "",
        "payment_method": None,
        "occurred_at": 1790000000000,
        "is_recurring": False,
        "recurrence_rule": None,
        "recurrence_parent_id": None,
    },
    "categories": {
        "id": "rec-1",
        "updated_at": 2000,
        "is_deleted": False,
        "name": "Cafe",
        "type": "expense",
    },
    "budgets": {
        "id": "rec-1",
        "updated_at": 2000,
        "is_deleted": False,
        "period": "2026-10",
        "category_id": None,
        "limit_amount": 2000000,
        "threshold_percent": 80,
    },
    "goals": {
        "id": "rec-1",
        "updated_at": 2000,
        "is_deleted": False,
        "name": "Mua laptop",
        "target_amount": 20000000,
        "current_amount": 0,
        "deadline": 1800000000000,
    },
    "corrections": {
        "id": "rec-1",
        "updated_at": 2000,
        "is_deleted": False,
        "transaction_id": "tx-1",
        "predicted_category_id": None,
        "corrected_category_id": "sys_an_uong",
        "created_at": 1790000000000,
    },
}

# 1 trường bắt buộc (không có giá trị mặc định) của mỗi bảng.
_REQUIRED_FIELD = {
    "transactions": "amount",
    "categories": "name",
    "budgets": "limit_amount",
    "goals": "deadline",
    "corrections": "corrected_category_id",
}


class FakePool:
    """Giả lập asyncpg.Pool cho toàn bộ route sync.
    - owners: id -> user_id đang lưu trong Postgres (None = category hệ thống);
      id không có trong owners = chưa tồn tại.
    - rows / has_more: dữ liệu trả về cho pull.
    Mọi lần upsert đều coi như ghi thành công (trả về id) và được ghi lại."""

    def __init__(self, owners=None, rows=None, has_more=False):
        self.owners = owners or {}
        self.rows = rows or []
        self.has_more = has_more
        self.upsert_args = []
        self.fetch_calls = []

    async def fetchrow(self, query, *args):
        record_id = args[0]
        if record_id not in self.owners:
            return None
        return {"user_id": self.owners[record_id]}

    async def fetchval(self, query, *args):
        if query.lstrip().startswith("INSERT"):
            self.upsert_args.append(args)
            return args[0]
        if "SELECT EXISTS" in query:
            return self.has_more
        return self.owners.get(args[0])

    async def fetch(self, query, *args):
        self.fetch_calls.append((query, args))
        return self.rows


def _build_test_app(pool: FakePool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(sync.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def test_push_every_table_returns_upserted_ids_in_contract_shape():
    for table, record in _RECORDS.items():
        pool = FakePool()
        client = TestClient(_build_test_app(pool))

        response = client.post(
            f"/api/v1/sync/{table}/push", json={"records": [record]}
        )

        assert response.status_code == 200, table
        assert response.json() == {"upserted_ids": ["rec-1"], "rejected": []}, table


def test_push_every_table_ignores_user_id_sent_by_client_and_uses_jwt():
    for table, record in _RECORDS.items():
        pool = FakePool()
        client = TestClient(_build_test_app(pool))

        response = client.post(
            f"/api/v1/sync/{table}/push",
            json={"records": [dict(record, user_id="hacker")]},
        )

        assert response.status_code == 200, table
        assert pool.upsert_args[0][1] == "user-1", table


def test_push_every_table_rejects_record_missing_a_required_field():
    for table, record in _RECORDS.items():
        pool = FakePool()
        client = TestClient(_build_test_app(pool))
        invalid_record = dict(record)
        del invalid_record[_REQUIRED_FIELD[table]]

        response = client.post(
            f"/api/v1/sync/{table}/push", json={"records": [invalid_record]}
        )

        assert response.status_code == 422, table
        assert pool.upsert_args == [], table


def test_pull_every_table_returns_records_and_has_more_in_contract_shape():
    for table, record in _RECORDS.items():
        pool = FakePool(rows=[record], has_more=True)
        client = TestClient(_build_test_app(pool))

        response = client.get(f"/api/v1/sync/{table}/pull?since=0")

        assert response.status_code == 200, table
        assert response.json() == {"records": [record], "has_more": True}, table


def test_pull_every_table_reads_its_own_table_for_the_jwt_user():
    for table in _RECORDS:
        pool = FakePool()
        client = TestClient(_build_test_app(pool))

        response = client.get(f"/api/v1/sync/{table}/pull?since=1500&user_id=hacker")

        assert response.status_code == 200, table
        query, args = pool.fetch_calls[0]
        assert f"FROM {table}" in query, table
        assert args == ("user-1", 1500, min(500, MAX_PULL_LIMIT)), table


def test_table_outside_the_five_synced_tables_has_no_route():
    client = TestClient(_build_test_app(FakePool()))

    push_response = client.post("/api/v1/sync/unknown/push", json={"records": []})
    pull_response = client.get("/api/v1/sync/unknown/pull?since=0")

    assert push_response.status_code == 404
    assert pull_response.status_code == 404


def _category(record_id, name="Cafe"):
    return dict(_RECORDS["categories"], id=record_id, name=name)


def test_push_category_of_another_user_is_rejected_through_the_endpoint():
    pool = FakePool(owners={"cat-1": "user-2"})
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/sync/categories/push", json={"records": [_category("cat-1")]}
    )

    assert response.status_code == 200
    assert response.json() == {
        "upserted_ids": [],
        "rejected": [{"id": "cat-1", "reason": "forbidden"}],
    }
    assert pool.upsert_args == []


def test_push_system_category_is_rejected_through_the_endpoint():
    pool = FakePool(owners={"sys_an_uong": None})
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/sync/categories/push", json={"records": [_category("sys_an_uong")]}
    )

    assert response.status_code == 200
    assert response.json() == {
        "upserted_ids": [],
        "rejected": [{"id": "sys_an_uong", "reason": "forbidden"}],
    }
    assert pool.upsert_args == []


def test_push_category_batch_with_one_forbidden_record_still_upserts_the_rest():
    pool = FakePool(owners={"cat-2": "user-2"})
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/sync/categories/push",
        json={
            "records": [
                _category("cat-1", "Cafe"),
                _category("cat-2", "Xang"),
                _category("cat-3", "An vat"),
            ]
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "upserted_ids": ["cat-1", "cat-3"],
        "rejected": [{"id": "cat-2", "reason": "forbidden"}],
    }
    assert [args[0] for args in pool.upsert_args] == ["cat-1", "cat-3"]