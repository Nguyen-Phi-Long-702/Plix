import time

import jwt
from cryptography.hazmat.primitives.asymmetric import ec
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.security import get_jwks_client
from app.routers import sync

_PRIVATE_KEY = ec.generate_private_key(ec.SECP256R1())


class FakeSigningKey:
    def __init__(self, key):
        self.key = key


class FakeJwksClient:
    """Giả lập PyJWKClient: luôn trả public key test cục bộ, không gọi mạng tới Supabase."""

    def get_signing_key_from_jwt(self, token):
        return FakeSigningKey(_PRIVATE_KEY.public_key())


class FakeSyncPool:
    """Giả lập asyncpg.Pool: ghi lại mọi lần chạm vào DB để chứng minh request bị chặn thì DB không bị đụng tới."""

    def __init__(self):
        self.calls = []

    async def fetchval(self, query, *args):
        self.calls.append((query, args))
        return args[0]

    async def fetch(self, query, *args):
        self.calls.append((query, args))
        return []


def _make_token(exp_delta_seconds):
    payload = {
        "sub": "user-1",
        "aud": "authenticated",
        "exp": int(time.time()) + exp_delta_seconds,
    }
    return jwt.encode(payload, _PRIVATE_KEY, algorithm="ES256")


def _auth(token):
    return {"Authorization": f"Bearer {token}"}


def _build_client(pool):
    # Không override verify_jwt: để verify_jwt thật chạy, chỉ thay nguồn public key bằng key test.
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(sync.router, prefix="/api/v1")
    test_app.dependency_overrides[get_jwks_client] = lambda: FakeJwksClient()
    return TestClient(test_app)


_RECORD = {
    "id": "tx-1",
    "updated_at": 2000,
    "is_deleted": False,
    "amount": 50000,
    "occurred_at": 1790000000000,
}


def test_push_with_expired_token_gets_401_then_retry_with_fresh_token_succeeds():
    pool = FakeSyncPool()
    client = _build_client(pool)
    expired = _make_token(exp_delta_seconds=-10)
    fresh = _make_token(exp_delta_seconds=3600)

    first = client.post(
        "/api/v1/sync/transactions/push",
        json={"records": [_RECORD]},
        headers=_auth(expired),
    )

    assert first.status_code == 401
    assert first.json()["detail"] == "Token đã hết hạn"
    assert pool.calls == []  # bị chặn trước khi chạm DB

    retry = client.post(
        "/api/v1/sync/transactions/push",
        json={"records": [_RECORD]},
        headers=_auth(fresh),
    )

    assert retry.status_code == 200
    assert retry.json() == {"upserted_ids": ["tx-1"], "rejected": []}
    assert len(pool.calls) == 1


def test_pull_with_expired_token_gets_401_then_retry_with_fresh_token_succeeds():
    pool = FakeSyncPool()
    client = _build_client(pool)
    expired = _make_token(exp_delta_seconds=-10)
    fresh = _make_token(exp_delta_seconds=3600)

    first = client.get("/api/v1/sync/transactions/pull?since=0", headers=_auth(expired))

    assert first.status_code == 401
    assert first.json()["detail"] == "Token đã hết hạn"
    assert pool.calls == []

    retry = client.get("/api/v1/sync/transactions/pull?since=0", headers=_auth(fresh))

    assert retry.status_code == 200
    assert retry.json() == {"records": [], "has_more": False}