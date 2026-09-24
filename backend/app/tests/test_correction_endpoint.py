from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import correction


class FakeCorrectionPool:
    def __init__(self, transaction_rows):
        self._transaction_rows = transaction_rows
        self.inserted = []

    async def fetchrow(self, query, *args):
        (transaction_id,) = args
        return self._transaction_rows.get(transaction_id)

    async def execute(self, query, *args):
        self.inserted.append(args)


def _build_test_app(pool: FakeCorrectionPool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(correction.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def test_correction_is_saved_when_transaction_belongs_to_current_user():
    pool = FakeCorrectionPool(transaction_rows={"tx-1": {"user_id": "user-1"}})
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/correction",
        json={
            "transaction_id": "tx-1",
            "predicted_category_id": "cat-predicted",
            "corrected_category_id": "cat-corrected",
        },
    )

    assert response.status_code == 201
    assert "id" in response.json()
    assert len(pool.inserted) == 1


def test_correction_is_rejected_when_transaction_belongs_to_another_user():
    pool = FakeCorrectionPool(transaction_rows={"tx-2": {"user_id": "user-2"}})
    client = TestClient(_build_test_app(pool))

    response = client.post(
        "/api/v1/correction",
        json={"transaction_id": "tx-2", "corrected_category_id": "cat-corrected"},
    )

    assert response.status_code == 403
    assert pool.inserted == []


def test_correction_requires_corrected_category_id():
    pool = FakeCorrectionPool(transaction_rows={"tx-1": {"user_id": "user-1"}})
    client = TestClient(_build_test_app(pool))

    response = client.post("/api/v1/correction", json={"transaction_id": "tx-1"})

    assert response.status_code == 422