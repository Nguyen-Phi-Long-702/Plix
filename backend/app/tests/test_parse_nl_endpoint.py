import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import parse_nl


class FakeCategoryPool:
    def __init__(self, rows):
        self._rows = rows
        self.fetch_args = None

    async def fetch(self, query, *args):
        assert "FROM categories" in query
        self.fetch_args = args
        return self._rows


WHITELIST = [
    {"id": "sys_an_uong", "name": "Ăn uống"},
    {"id": "sys_di_chuyen", "name": "Di chuyển"},
]


def _build_test_app(pool: FakeCategoryPool) -> FastAPI:
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(parse_nl.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return test_app


def test_parse_nl_example_sentence_returns_two_items():
    pool = FakeCategoryPool(WHITELIST)
    client = TestClient(_build_test_app(pool))

    response = client.post("/api/v1/parse-nl", json={"text": "phở 50k, grab 35k"})

    assert response.status_code == 200
    items = response.json()["items"]
    assert len(items) == 2
    assert (items[0]["amount"], items[0]["category_id"], items[0]["source"]) == (
        50000,
        "sys_an_uong",
        "rule",
    )
    assert items[0]["confidence"] == pytest.approx(1.0)
    assert (items[1]["amount"], items[1]["category_id"], items[1]["source"]) == (
        35000,
        "sys_di_chuyen",
        "rule",
    )
    assert items[1]["confidence"] == pytest.approx(0.75)
    assert pool.fetch_args == ("user-1",)


def test_parse_nl_part_without_money_is_marked_with_null_amount():
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": "xin chào, phở 50k"})

    assert response.status_code == 200
    assert response.json()["items"][0] == {
        "amount": None,
        "category_id": None,
        "confidence": 0.0,
        "source": "rule",
    }
    assert response.json()["items"][1]["amount"] == 50000


def test_parse_nl_empty_text_returns_no_items():
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": "   "})

    assert response.status_code == 200
    assert response.json() == {"items": []}


def test_parse_nl_requires_text():
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={})

    assert response.status_code == 422