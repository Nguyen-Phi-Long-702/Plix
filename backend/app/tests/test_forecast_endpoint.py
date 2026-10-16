from datetime import datetime

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import forecast
from app.services.ai.forecast import VN_TZ


def ms(year, month, day, hour=12):
    return round(datetime(year, month, day, hour, tzinfo=VN_TZ).timestamp() * 1000)


# "Bây giờ" cố định: thứ Tư 14/10/2026 12:00 (giờ VN). Cùng bộ dữ liệu với test_forecast.py:
# tuần NET [+700.000, -200.000, +700.000, -400.000] -> dự báo 237.700/tuần.
NOW = ms(2026, 10, 14)
TRANSACTIONS = [
    ("income", 1000000, ms(2026, 9, 15)),
    ("expense", 300000, ms(2026, 9, 15)),
    ("income", 500000, ms(2026, 9, 22)),
    ("expense", 700000, ms(2026, 9, 22)),
    ("income", 800000, ms(2026, 9, 29)),
    ("expense", 100000, ms(2026, 10, 2)),
    ("expense", 400000, ms(2026, 10, 6)),
    ("expense", 50000, ms(2026, 10, 13)),
]


class FakeForecastPool:
    def __init__(self, transactions):
        self._transactions = transactions
        self.fetch_args = None

    async def fetch(self, query, *args):
        assert "FROM transactions" in query
        self.fetch_args = args
        return [
            {"type": tx_type, "amount": amount, "occurred_at": occurred_at}
            for tx_type, amount, occurred_at in self._transactions
        ]


def _build_test_app(pool: FakeForecastPool, monkeypatch) -> FastAPI:
    monkeypatch.setattr(forecast, "_now_ms", lambda: NOW)
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(forecast.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def test_forecast_defaults_to_current_month(monkeypatch):
    pool = FakeForecastPool(TRANSACTIONS)
    client = TestClient(_build_test_app(pool, monkeypatch))

    response = client.get("/api/v1/forecast")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "forecast_net_amount": 44250}
    assert pool.fetch_args == ("user-1",)


def test_forecast_with_month_param_computes_that_month(monkeypatch):
    pool = FakeForecastPool(TRANSACTIONS)
    client = TestClient(_build_test_app(pool, monkeypatch))

    response = client.get("/api/v1/forecast", params={"month": "2026-11"})

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "forecast_net_amount": 1018714}


def test_forecast_returns_insufficient_data_with_null_amount(monkeypatch):
    pool = FakeForecastPool([])
    client = TestClient(_build_test_app(pool, monkeypatch))

    response = client.get("/api/v1/forecast")

    assert response.status_code == 200
    assert response.json() == {"status": "insufficient_data", "forecast_net_amount": None}


def test_forecast_rejects_invalid_month_format(monkeypatch):
    pool = FakeForecastPool(TRANSACTIONS)
    client = TestClient(_build_test_app(pool, monkeypatch))

    for bad_month in ("2026-13", "2026-1", "10-2026", "abc"):
        response = client.get("/api/v1/forecast", params={"month": bad_month})

        assert response.status_code == 422