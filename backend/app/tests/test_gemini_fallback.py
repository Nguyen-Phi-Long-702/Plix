import asyncio

import httpx
import pytest

import app.services.ai.gemini_fallback as gemini_fallback
from app.services.ai.gemini_fallback import (
    GeminiUnavailableError,
    build_request_body,
    call_gemini,
    extract_response_text,
    parse_with_gemini,
    should_call_gemini,
)
from app.services.validation_service import ValidatedItem


def test_should_call_gemini_when_every_part_is_below_threshold():
    assert should_call_gemini([(None, None, 0.0), (50000, None, 0.0), (35000, "sys_di_chuyen", 0.3)])


def test_should_not_call_gemini_when_any_part_reaches_threshold():
    assert not should_call_gemini([(None, None, 0.0), (50000, "sys_an_uong", 1.0)])


def test_should_not_call_gemini_when_confidence_equals_threshold():
    assert not should_call_gemini([(50000, "sys_an_uong", 0.4)])


def test_should_not_call_gemini_when_there_is_no_part():
    assert not should_call_gemini([])


def test_request_body_has_text_category_names_and_fixed_json_schema():
    body = build_request_body("hôm qua đi ăn hết 200 ngàn", ["Ăn uống", "Di chuyển"])

    prompt = body["contents"][0]["parts"][0]["text"]
    assert "hôm qua đi ăn hết 200 ngàn" in prompt
    assert "- Ăn uống" in prompt
    assert "- Di chuyển" in prompt
    assert body["generationConfig"]["responseMimeType"] == "application/json"
    assert body["generationConfig"]["responseSchema"] is gemini_fallback.RESPONSE_SCHEMA


def test_extract_response_text_reads_first_candidate_text():
    payload = {"candidates": [{"content": {"parts": [{"text": '{"items": []}'}]}}]}

    assert extract_response_text(payload) == '{"items": []}'


@pytest.mark.parametrize(
    "payload",
    [
        {},
        {"candidates": []},
        {"candidates": [{"content": {"parts": []}}]},
        {"candidates": [{"content": {"parts": [{}]}}]},
        {"candidates": [{"content": {"parts": [{"text": 5}]}}]},
        None,
        [],
    ],
)
def test_extract_response_text_returns_none_for_unexpected_shape(payload):
    assert extract_response_text(payload) is None


class FakeResponse:
    def __init__(self, status_code=200, payload=None, json_error=False):
        self.status_code = status_code
        self._payload = payload
        self._json_error = json_error

    def json(self):
        if self._json_error:
            raise ValueError("không phải JSON")
        return self._payload


def _install_fake_client(monkeypatch, response=None, error=None, delay=0.0):
    calls = []

    class FakeAsyncClient:
        def __init__(self, timeout=None):
            calls.append({"timeout": timeout})

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def post(self, url, headers=None, json=None):
            calls[0].update({"url": url, "headers": headers, "json": json})
            if delay:
                await asyncio.sleep(delay)
            if error is not None:
                raise error
            return response

    monkeypatch.setattr(gemini_fallback.httpx, "AsyncClient", FakeAsyncClient)
    return calls


def test_call_gemini_returns_text_and_sends_key_in_header_only(monkeypatch):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", "test-key")
    payload = {"candidates": [{"content": {"parts": [{"text": '{"items": []}'}]}}]}
    calls = _install_fake_client(monkeypatch, response=FakeResponse(200, payload))

    text = asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))

    assert text == '{"items": []}'
    assert calls[0]["url"] == gemini_fallback.GEMINI_URL
    assert calls[0]["headers"] == {"x-goog-api-key": "test-key"}
    assert "test-key" not in calls[0]["url"]
    assert calls[0]["timeout"] == 8.0


def test_call_gemini_without_api_key_raises_unavailable(monkeypatch):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", None)
    calls = _install_fake_client(monkeypatch, response=FakeResponse(200, {}))

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))
    assert calls == []


@pytest.mark.parametrize("status_code", [429, 500, 503, 400, 403, 404])
def test_call_gemini_non_200_raises_unavailable(monkeypatch, status_code):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", "test-key")
    _install_fake_client(monkeypatch, response=FakeResponse(status_code, {}))

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))


@pytest.mark.parametrize("error", [httpx.ReadTimeout("quá giờ"), httpx.ConnectError("mất mạng")])
def test_call_gemini_network_error_raises_unavailable(monkeypatch, error):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", "test-key")
    _install_fake_client(monkeypatch, error=error)

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))


def test_call_gemini_total_timeout_raises_unavailable(monkeypatch):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", "test-key")
    monkeypatch.setattr(gemini_fallback, "GEMINI_TIMEOUT_SECONDS", 0.05)
    _install_fake_client(monkeypatch, response=FakeResponse(200, {}), delay=1.0)

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))


def test_call_gemini_non_json_body_raises_unavailable(monkeypatch):
    monkeypatch.setattr(gemini_fallback, "GEMINI_API_KEY", "test-key")
    _install_fake_client(monkeypatch, response=FakeResponse(200, json_error=True))

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(call_gemini("phở 50k", ["Ăn uống"]))


class FakeCategoryPool:
    def __init__(self, rows):
        self._rows = rows
        self.fetch_args = None

    async def fetch(self, query, *args):
        assert "FROM categories" in query
        self.fetch_args = args
        return self._rows


WHITELIST_ROWS = [
    {"id": "sys_an_uong", "name": "Ăn uống"},
    {"id": "sys_di_chuyen", "name": "Di chuyển"},
]


def test_parse_with_gemini_keeps_only_valid_items_from_whitelist(monkeypatch):
    sent = {}

    async def fake_call_gemini(text, category_names):
        sent["text"] = text
        sent["category_names"] = category_names
        return (
            '{"items": ['
            '{"amount": 200000, "category": "Ăn uống", "confidence": 0.8},'
            '{"amount": 50000, "category": "Du lịch", "confidence": 0.9}'
            "]}"
        )

    monkeypatch.setattr(gemini_fallback, "call_gemini", fake_call_gemini)
    pool = FakeCategoryPool(WHITELIST_ROWS)

    items = asyncio.run(parse_with_gemini(pool, "user-1", "hôm qua đi ăn hết 200 ngàn"))

    assert items == [ValidatedItem(200000, "sys_an_uong", 0.8)]
    assert pool.fetch_args == ("user-1",)
    assert sent == {"text": "hôm qua đi ăn hết 200 ngàn", "category_names": ["Ăn uống", "Di chuyển"]}


def test_parse_with_gemini_returns_empty_when_gemini_gives_no_content(monkeypatch):
    async def fake_call_gemini(text, category_names):
        return None

    monkeypatch.setattr(gemini_fallback, "call_gemini", fake_call_gemini)

    assert asyncio.run(parse_with_gemini(FakeCategoryPool(WHITELIST_ROWS), "user-1", "abc")) == []


def test_parse_with_gemini_propagates_unavailable(monkeypatch):
    async def fake_call_gemini(text, category_names):
        raise GeminiUnavailableError("http_429")

    monkeypatch.setattr(gemini_fallback, "call_gemini", fake_call_gemini)

    with pytest.raises(GeminiUnavailableError):
        asyncio.run(parse_with_gemini(FakeCategoryPool(WHITELIST_ROWS), "user-1", "abc"))