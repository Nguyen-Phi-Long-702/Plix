import asyncio

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import retrain
from app.services.ai.retrain_service import NoTrainingDataError, retrain_user_model


class FakeNoDataPool:
    def __init__(self):
        self.executed = []
        self.saved = False
        self.is_training = False

    async def fetchrow(self, query, *args):
        self.is_training = True
        return {"user_id": args[0]}

    async def fetch(self, query, *args):
        return []

    async def execute(self, query, *args):
        self.executed.append(query)
        if "UPDATE ai_model_params SET is_training = false" in query:
            self.is_training = False
        if "INSERT INTO ai_model_params" in query:
            self.saved = True


class FakeNoCooldownPool:
    async def fetchrow(self, query, *args):
        return None


def test_retrain_without_any_transaction_raises_and_cleans_up():
    pool = FakeNoDataPool()

    with pytest.raises(NoTrainingDataError):
        asyncio.run(retrain_user_model(pool, "user-1"))

    assert pool.saved is False
    assert pool.is_training is False
    assert any("DELETE FROM ai_model_params" in query for query in pool.executed)


def test_retrain_endpoint_returns_400_when_user_has_no_training_data(monkeypatch):
    async def _raise_no_data(pool, user_id):
        raise NoTrainingDataError(user_id)

    monkeypatch.setattr(retrain, "retrain_user_model", _raise_no_data)
    test_app = FastAPI()
    test_app.state.db_pool = FakeNoCooldownPool()
    test_app.include_router(retrain.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"

    response = TestClient(test_app).post("/api/v1/retrain")

    assert response.status_code == 400
    assert "giao dịch" in response.json()["detail"]