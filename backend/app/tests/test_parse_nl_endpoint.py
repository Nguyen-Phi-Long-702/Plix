import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import verify_jwt
from app.routers import parse_nl
from app.services.ai.gemini_fallback import GeminiUnavailableError
from app.services.validation_service import ValidatedItem

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


# Câu không có chữ số nào -> parser rule-based không tìm được số tiền -> toàn bộ câu confidence 0.0
COMPLEX_SENTENCE = "tuần trước mình tặng quà sinh nhật bạn hết hai triệu"


def test_parse_nl_calls_gemini_when_whole_sentence_has_low_confidence(monkeypatch):
    async def fake_parse_with_gemini(pool, user_id, text):
        assert (user_id, text) == ("user-1", COMPLEX_SENTENCE)
        return [ValidatedItem(2000000, "sys_an_uong", 0.8)]

    monkeypatch.setattr(parse_nl, "parse_with_gemini", fake_parse_with_gemini)
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": COMPLEX_SENTENCE, "allow_gemini": True}
    )
    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {"amount": 2000000, "category_id": "sys_an_uong", "confidence": 0.8, "source": "gemini"}
        ]
    }


def test_parse_nl_does_not_call_gemini_when_rule_parser_is_confident(monkeypatch):
    async def fail_if_called(pool, user_id, text):
        raise AssertionError("không được gọi Gemini khi parser tự viết đã đủ tin cậy")

    monkeypatch.setattr(parse_nl, "parse_with_gemini", fail_if_called)
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": "phở 50k, grab 35k"})

    assert response.status_code == 200
    assert [item["source"] for item in response.json()["items"]] == ["rule", "rule"]


def test_parse_nl_returns_ai_busy_when_gemini_is_unavailable(monkeypatch):
    async def fake_parse_with_gemini(pool, user_id, text):
        raise GeminiUnavailableError("http_429")

    monkeypatch.setattr(parse_nl, "parse_with_gemini", fake_parse_with_gemini)
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": COMPLEX_SENTENCE, "allow_gemini": True})

    assert response.status_code == 503
    assert response.json() == {"error_code": "AI_BUSY", "message": "AI đang bận, vui lòng nhập tay"}


def test_parse_nl_keeps_rule_result_when_gemini_returns_no_valid_item(monkeypatch):
    async def fake_parse_with_gemini(pool, user_id, text):
        return []

    monkeypatch.setattr(parse_nl, "parse_with_gemini", fake_parse_with_gemini)
    client = TestClient(_build_test_app(FakeCategoryPool(WHITELIST)))

    response = client.post("/api/v1/parse-nl", json={"text": COMPLEX_SENTENCE, "allow_gemini": True})

    assert response.status_code == 200
    assert response.json() == {
        "items": [{"amount": None, "category_id": None, "confidence": 0.0, "source": "rule"}]
    }