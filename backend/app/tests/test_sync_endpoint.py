from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.config import MAX_PULL_LIMIT
from app.core.security import verify_jwt
from app.routers import sync


class FakeSyncPool:
    """Giả lập asyncpg.Pool: mọi lần upsert đều coi như ghi thành công (trả về
    id, là tham số đầu tiên) và ghi lại tham số để kiểm tra user_id."""

    def __init__(self):
        self.upsert_args = []

    async def fetchval(self, query, *args):
        self.upsert_args.append(args)
        return args[0]


def _build_test_app(pool: FakeSyncPool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(sync.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def _valid_record(**overrides):
    record = {
        "id": "tx-1",
        "updated_at": 2000,
        "is_deleted": False,
        "amount": 50000,
        "occurred_at": 1790000000000,
    }
    record.update(overrides)
    return record


def test_push_returns_upserted_ids_in_contract_shape():
    pool = FakeSyncPool()
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/sync/transactions/push", json={"records": [_valid_record()]}
    )

    assert response.status_code == 200
    assert response.json() == {"upserted_ids": ["tx-1"], "rejected": []}


def test_user_id_sent_by_client_is_ignored_and_jwt_user_id_is_used():
    pool = FakeSyncPool()
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/sync/transactions/push",
        json={"records": [_valid_record(user_id="hacker")]},
    )

    assert response.status_code == 200
    assert pool.upsert_args[0][1] == "user-1"


def test_push_rejects_record_missing_required_field():
    pool = FakeSyncPool()
    client = TestClient(_build_test_app(pool))
    record = _valid_record()
    del record["amount"]

    response = client.post(
        "/api/v1/sync/transactions/push", json={"records": [record]}
    )

    assert response.status_code == 422
    assert pool.upsert_args == []


class FakePullPool:
    """Giả lập asyncpg.Pool cho endpoint pull: fetch trả sẵn các dòng, fetchval
    trả sẵn has_more; ghi lại tham số của lần fetch để kiểm tra user_id/limit."""

    def __init__(self, rows=None, has_more=False):
        self.rows = rows or []
        self.has_more = has_more
        self.fetch_args = []

    async def fetch(self, query, *args):
        self.fetch_args.append(args)
        return self.rows

    async def fetchval(self, query, *args):
        return self.has_more


def _pull_row(**overrides):
    row = {
        "id": "tx-1",
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
    }
    row.update(overrides)
    return row


def test_pull_returns_records_and_has_more_in_contract_shape():
    pool = FakePullPool(rows=[_pull_row()], has_more=True)
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/sync/transactions/pull?since=0")

    assert response.status_code == 200
    assert response.json() == {"records": [_pull_row()], "has_more": True}


def test_pull_returns_tombstone_records():
    pool = FakePullPool(rows=[_pull_row(is_deleted=True)])
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/sync/transactions/pull?since=0")

    assert response.status_code == 200
    assert response.json()["records"][0]["is_deleted"] is True


def test_pull_uses_jwt_user_id_and_ignores_user_id_sent_by_client():
    pool = FakePullPool()
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/sync/transactions/pull?since=0&user_id=hacker")

    assert response.status_code == 200
    assert pool.fetch_args[0][0] == "user-1"


def test_pull_default_limit_is_500_capped_by_max_pull_limit():
    pool = FakePullPool()
    client = TestClient(_build_test_app(pool))

    client.get("/api/v1/sync/transactions/pull?since=1500")

    assert pool.fetch_args[0] == ("user-1", 1500, min(500, MAX_PULL_LIMIT))


def test_pull_limit_larger_than_max_pull_limit_is_capped():
    pool = FakePullPool()
    client = TestClient(_build_test_app(pool))

    client.get(f"/api/v1/sync/transactions/pull?since=0&limit={MAX_PULL_LIMIT + 1000}")

    assert pool.fetch_args[0][2] == MAX_PULL_LIMIT


def test_pull_rejects_missing_since():
    pool = FakePullPool()
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/sync/transactions/pull")

    assert response.status_code == 422
    assert pool.fetch_args == []


def test_pull_rejects_limit_below_one():
    pool = FakePullPool()
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/sync/transactions/pull?since=0&limit=0")

    assert response.status_code == 422
    assert pool.fetch_args == []