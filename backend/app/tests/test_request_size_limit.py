from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.middleware import MAX_REQUEST_BODY_BYTES, MaxBodySizeMiddleware


def _build_test_app() -> FastAPI:
    """App FastAPI tối giản, chỉ gắn middleware cần test — không đụng tới
    app.main (tránh phải khởi tạo pool kết nối Postgres thật khi chạy test)."""
    test_app = FastAPI()
    test_app.add_middleware(MaxBodySizeMiddleware)

    @test_app.post("/echo")
    def echo():
        return {"ok": True}

    return test_app


def test_request_within_limit_is_allowed():
    client = TestClient(_build_test_app())

    response = client.post("/echo", content=b"a" * 1000)

    assert response.status_code == 200


def test_request_over_limit_is_rejected():
    client = TestClient(_build_test_app())
    oversized_body = b"a" * (MAX_REQUEST_BODY_BYTES + 1)

    response = client.post("/echo", content=oversized_body)

    assert response.status_code == 413
    data = response.json()
    assert data["error_code"] == "PAYLOAD_TOO_LARGE"
    assert "message" in data