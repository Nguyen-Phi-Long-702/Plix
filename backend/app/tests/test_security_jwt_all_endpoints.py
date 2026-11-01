import pytest
from fastapi.testclient import TestClient

from app.core.security import get_jwks_client
from app.main import app
from app.tests.security_helpers import (
    ATTACKER_KEY,
    ENDPOINT_IDS,
    PROTECTED_ENDPOINTS,
    USER_A,
    FakeJwksClient,
    RecordingPool,
    auth_headers,
    make_token,
)

PUBLIC_ENDPOINTS = {("GET", "/api/v1/health")}

_UNAUTHORIZED = "UNAUTHORIZED"

# Mỗi loại token xấu: (hàm tạo token, thông báo lỗi mong đợi).
_BAD_TOKENS = {
    "expired": (lambda: make_token(exp_delta_seconds=-10), "Token đã hết hạn"),
    "forged_other_key": (
        lambda: make_token(sub="victim-user", key=ATTACKER_KEY),
        "Token không hợp lệ",
    ),
    "malformed": (lambda: "not-a-jwt", "Token không hợp lệ"),
}


@pytest.fixture
def client_and_pool():
    # Dùng app thật của dự án (app.main.app, không chạy lifespan nên không kết nối Postgres thật).
    # Không override verify_jwt: để verify_jwt thật chạy, chỉ thay nguồn public key bằng key test.
    pool = RecordingPool()
    app.state.db_pool = pool
    app.dependency_overrides[get_jwks_client] = lambda: FakeJwksClient()
    yield TestClient(app), pool
    app.dependency_overrides.clear()


def test_security_catalog_covers_every_route_of_the_app():
    # Chặn quên: thêm endpoint mới vào app mà chưa đưa vào PROTECTED_ENDPOINTS thì test này đỏ.
    methods_of_http = {"get", "post", "put", "patch", "delete"}
    actual = {
        (method.upper(), path)
        for path, operations in app.openapi()["paths"].items()
        for method in operations
        if method in methods_of_http
    }
    expected = {(method, path) for method, path, _ in PROTECTED_ENDPOINTS} | PUBLIC_ENDPOINTS

    assert actual == expected


def test_valid_token_is_accepted(client_and_pool):
    # Đối chứng: chứng minh dựng test đúng, để các test 401 bên dưới không "đỗ" vì lỗi dựng.
    client, _ = client_and_pool

    response = client.get("/api/v1/whoami", headers=auth_headers(make_token(sub=USER_A)))

    assert response.status_code == 200
    assert response.json() == {"user_id": USER_A}


@pytest.mark.parametrize("token_kind", list(_BAD_TOKENS))
@pytest.mark.parametrize("method,path,kwargs", PROTECTED_ENDPOINTS, ids=ENDPOINT_IDS)
def test_bad_token_is_rejected_on_every_endpoint(
    client_and_pool, method, path, kwargs, token_kind
):
    client, pool = client_and_pool
    make_bad_token, expected_message = _BAD_TOKENS[token_kind]

    response = client.request(method, path, headers=auth_headers(make_bad_token()), **kwargs)

    assert response.status_code == 401
    assert response.json() == {"error_code": _UNAUTHORIZED, "message": expected_message}
    assert pool.calls == []  # bị chặn trước khi chạm DB


@pytest.mark.parametrize("method,path,kwargs", PROTECTED_ENDPOINTS, ids=ENDPOINT_IDS)
def test_missing_authorization_header_is_rejected_on_every_endpoint(
    client_and_pool, method, path, kwargs
):
    client, pool = client_and_pool

    response = client.request(method, path, **kwargs)

    # FastAPI bản mới trả 401, bản cũ trả 403 khi thiếu header — cả hai đều là từ chối.
    assert response.status_code in (401, 403)
    assert pool.calls == []