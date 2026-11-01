import base64
import json
import time

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import ec
from fastapi import Depends, FastAPI
from fastapi.testclient import TestClient

from app.core.security import get_jwks_client, verify_jwt

_PRIVATE_KEY = ec.generate_private_key(ec.SECP256R1())
_ATTACKER_KEY = ec.generate_private_key(ec.SECP256R1())

_INVALID_MESSAGE = "Token không hợp lệ"


class FakeSigningKey:
    def __init__(self, key):
        self.key = key


class FakeJwksClient:
    """Giả lập PyJWKClient: luôn trả public key test cục bộ, không gọi mạng tới Supabase."""

    def get_signing_key_from_jwt(self, token):
        return FakeSigningKey(_PRIVATE_KEY.public_key())


def _build_client():
    # Không override verify_jwt: để verify_jwt thật chạy, chỉ thay nguồn public key bằng key test.
    test_app = FastAPI()

    @test_app.get("/protected")
    def protected(user_id: str = Depends(verify_jwt)):
        return {"user_id": user_id}

    test_app.dependency_overrides[get_jwks_client] = lambda: FakeJwksClient()
    return TestClient(test_app)


def _payload(sub="user-1", exp_delta_seconds=3600):
    return {
        "sub": sub,
        "aud": "authenticated",
        "exp": int(time.time()) + exp_delta_seconds,
    }


def _auth(token):
    return {"Authorization": f"Bearer {token}"}


def _b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _assert_rejected_as_invalid(response):
    assert response.status_code == 401
    assert response.json()["detail"] == _INVALID_MESSAGE


def test_valid_token_is_accepted():
    # Đối chứng: chứng minh dựng test đúng, để các test 401 bên dưới không "đỗ" vì lỗi dựng.
    client = _build_client()
    token = jwt.encode(_payload(sub="user-1"), _PRIVATE_KEY, algorithm="ES256")

    response = client.get("/protected", headers=_auth(token))

    assert response.status_code == 200
    assert response.json() == {"user_id": "user-1"}


@pytest.mark.parametrize("malformed_token", ["not-a-jwt", "aaa.bbb.ccc"])
def test_malformed_token_is_rejected(malformed_token):
    client = _build_client()

    response = client.get("/protected", headers=_auth(malformed_token))

    _assert_rejected_as_invalid(response)


def test_token_without_sub_is_rejected():
    client = _build_client()
    payload = _payload()
    del payload["sub"]
    token = jwt.encode(payload, _PRIVATE_KEY, algorithm="ES256")

    response = client.get("/protected", headers=_auth(token))

    _assert_rejected_as_invalid(response)


def test_forged_token_signed_with_other_key_is_rejected():
    client = _build_client()
    token = jwt.encode(_payload(), _ATTACKER_KEY, algorithm="ES256")

    response = client.get("/protected", headers=_auth(token))

    _assert_rejected_as_invalid(response)


def test_forged_token_with_tampered_payload_is_rejected():
    # Lấy token thật của user-1, thay payload thành user khác nhưng giữ nguyên chữ ký cũ.
    client = _build_client()
    real_token = jwt.encode(_payload(sub="user-1"), _PRIVATE_KEY, algorithm="ES256")
    header, _, signature = real_token.split(".")
    forged_payload = _b64url(json.dumps(_payload(sub="victim-user")).encode())
    forged_token = ".".join([header, forged_payload, signature])

    response = client.get("/protected", headers=_auth(forged_token))

    _assert_rejected_as_invalid(response)


def test_forged_unsigned_token_alg_none_is_rejected():
    client = _build_client()
    token = jwt.encode(_payload(), None, algorithm="none")

    response = client.get("/protected", headers=_auth(token))

    _assert_rejected_as_invalid(response)


def test_forged_token_signed_with_hs256_is_rejected():
    # Kẻ tấn công ký bằng HS256 (kiểu secret dùng chung) trong khi server chỉ chấp nhận ES256.
    client = _build_client()
    token = jwt.encode(
        _payload(), "fake-hs256-secret-0123456789-abcdefghij", algorithm="HS256"
    )

    response = client.get("/protected", headers=_auth(token))

    _assert_rejected_as_invalid(response)