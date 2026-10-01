from fastapi import FastAPI
from fastapi.testclient import TestClient

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