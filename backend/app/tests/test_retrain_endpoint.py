import asyncio
from types import SimpleNamespace

from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import retrain

FIXED_NOTES = ["phở bò", "cơm gà", "xe buýt", "tàu hoả"]
FIXED_LABELS = ["Ăn uống", "Ăn uống", "Di chuyển", "Di chuyển"]

class FakeRetrainEndpointPool:
    def __init__(self, correction_count: int, elapsed_seconds=None):
        self._correction_count = correction_count
        self._rows = {}
        self._initial_elapsed_seconds = elapsed_seconds

    def _row(self, user_id: str) -> dict:
        return self._rows.setdefault(
            user_id,
            {
                "is_training": False,
                "elapsed_seconds": self._initial_elapsed_seconds,
                "training_sample_count": 0,
                "class_priors": "{}",
            },
        )

    async def fetchrow(self, query, *args):
        user_id = args[0]
        row = self._row(user_id)

        if "EXTRACT(EPOCH" in query:
            if row["elapsed_seconds"] is None:
                return None
            return {"elapsed_seconds": row["elapsed_seconds"]}

        if "INSERT INTO ai_model_params (user_id, is_training)" in query:
            if row["is_training"]:
                return None
            row["is_training"] = True
            return {"user_id": user_id}

        if "SELECT idf, class_priors, likelihoods, trained_at" in query:
            return {
                "idf": "{}",
                "class_priors": row["class_priors"],
                "likelihoods": "{}",
                "trained_at": "2026-09-28T10:00:00+00:00",
                "training_sample_count": row["training_sample_count"],
                "is_training": row["is_training"],
            }

        raise AssertionError(f"fetchrow không mong đợi trong test: {query}")

    async def fetch(self, query, *args):
        await asyncio.sleep(0)
        return [
            {"note": note, "category_name": label}
            for note, label in zip(FIXED_NOTES, FIXED_LABELS)
        ]

    async def fetchval(self, query, *args):
        return self._correction_count

    async def execute(self, query, *args):
        user_id = args[0]
        row = self._row(user_id)
        if "UPDATE ai_model_params SET is_training = false" in query:
            row["is_training"] = False
        elif "INSERT INTO ai_model_params" in query and "vocabulary" in query:
            row["is_training"] = False
            row["elapsed_seconds"] = 0 
            row["training_sample_count"] = args[5]
            row["class_priors"] = args[3]  


def _build_test_app(pool: FakeRetrainEndpointPool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(retrain.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def _post_retrain(test_app: FastAPI):
    client = TestClient(test_app)
    return client.post("/api/v1/retrain")


def test_first_retrain_succeeds_and_returns_trained_at_and_sample_count():
    pool = FakeRetrainEndpointPool(correction_count=2, elapsed_seconds=None)
    test_app = _build_test_app(pool)

    response = _post_retrain(test_app)

    assert response.status_code == 200
    body = response.json()
    assert body["training_sample_count"] == 2
    assert "trained_at" in body


def test_retrain_blocked_by_rate_limit_within_cooldown():
    pool = FakeRetrainEndpointPool(correction_count=2, elapsed_seconds=100)
    test_app = _build_test_app(pool)

    response = _post_retrain(test_app)

    assert response.status_code == 429
    assert "phút" in response.json()["detail"]


def test_concurrent_retrain_requests_only_one_succeeds():
    pool = FakeRetrainEndpointPool(correction_count=2, elapsed_seconds=None)
    fake_request = SimpleNamespace(app=SimpleNamespace(state=SimpleNamespace(db_pool=pool)))

    async def _call_twice():
        return await asyncio.gather(
            retrain.retrain(fake_request, user_id="user-1"),
            retrain.retrain(fake_request, user_id="user-1"),
            return_exceptions=True,
        )

    results = asyncio.run(_call_twice())

    succeeded = [r for r in results if not isinstance(r, Exception)]
    conflicted = [
        r for r in results
        if isinstance(r, HTTPException) and r.status_code == 409
    ]
    assert len(succeeded) == 1, "phải có đúng 1 lượt retrain thành công"
    assert len(conflicted) == 1, "lượt còn lại phải nhận HTTPException 409"