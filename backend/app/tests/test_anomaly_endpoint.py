from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import anomaly


class FakeAnomalyPool:
    def __init__(self, amounts, category_row):
        self._amounts = amounts
        self._category_row = category_row
        self.fetch_args = None

    async def fetch(self, query, *args):
        assert "FROM transactions" in query
        self.fetch_args = args
        return [{"amount": amount} for amount in self._amounts]

    async def fetchrow(self, query, *args):
        assert "FROM categories" in query
        return self._category_row


def _build_test_app(pool: FakeAnomalyPool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(anomaly.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def test_anomaly_returns_flagged_high_with_explanation():
    pool = FakeAnomalyPool(
        amounts=[40000] * 5 + [50000] * 5,
        category_row={"name": "Ăn uống"},
    )
    client = TestClient(_build_test_app(pool))

    response = client.get(
        "/api/v1/anomaly", params={"category_id": "cat-an-uong", "amount": 60000}
    )

    assert response.status_code == 200
    assert response.json() == {
        "status": "flagged_high",
        "explanation": (
            "Giao dịch này cao hơn mức chi tiêu trung bình cho danh mục [Ăn uống] của bạn "
            "(trung bình 45.000đ, giao dịch này 60.000đ)."
        ),
    }
    assert pool.fetch_args == ("user-1", "cat-an-uong")


def test_anomaly_requires_amount():
    pool = FakeAnomalyPool(amounts=[], category_row=None)
    client = TestClient(_build_test_app(pool))

    response = client.get("/api/v1/anomaly", params={"category_id": "cat-an-uong"})

    assert response.status_code == 422


def test_anomaly_returns_insufficient_data_for_new_category():
    pool = FakeAnomalyPool(amounts=[], category_row=None)
    client = TestClient(_build_test_app(pool))

    response = client.get(
        "/api/v1/anomaly", params={"category_id": "cat-moi", "amount": 50000}
    )

    assert response.status_code == 200
    assert response.json() == {
        "status": "insufficient_data",
        "explanation": (
            "Chưa đủ dữ liệu để đánh giá bất thường cho danh mục này "
            "(cần ít nhất 5 giao dịch đã đồng bộ)."
        ),
    }
    assert pool.fetch_args == ("user-1", "cat-moi")


def test_anomaly_returns_flagged_low_with_iqr_explanation():
    pool = FakeAnomalyPool(
        amounts=[40000, 45000, 50000, 55000, 60000],
        category_row={"name": "Ăn uống"},
    )
    client = TestClient(_build_test_app(pool))

    response = client.get(
        "/api/v1/anomaly", params={"category_id": "cat-an-uong", "amount": 20000}
    )

    assert response.status_code == 200
    assert response.json() == {
        "status": "flagged_low",
        "explanation": (
            "Giao dịch này thấp hơn mức chi tiêu thông thường cho danh mục [Ăn uống] của bạn "
            "(trung vị 50.000đ, giao dịch này 20.000đ)."
        ),
    }
    assert pool.fetch_args == ("user-1", "cat-an-uong")