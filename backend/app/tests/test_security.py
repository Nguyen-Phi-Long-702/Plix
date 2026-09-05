import time

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import ec

from app.core.security import decode_and_get_user_id


class FakeSigningKey:
    """Giả lập đối tượng key mà PyJWKClient trả về."""

    def __init__(self, key):
        self.key = key


class FakeJwksClient:
    """Giả lập PyJWKClient cho unit test — luôn trả về public key test cục bộ,
    không gọi mạng thật tới Supabase."""

    def __init__(self, public_key):
        self._public_key = public_key

    def get_signing_key_from_jwt(self, token):
        return FakeSigningKey(self._public_key)


@pytest.fixture(scope="module")
def keypair():
    private_key = ec.generate_private_key(ec.SECP256R1())
    public_key = private_key.public_key()
    return private_key, public_key


@pytest.fixture
def fake_jwks_client(keypair):
    _, public_key = keypair
    return FakeJwksClient(public_key)


def make_token(private_key, sub="test-user-id", exp_delta_seconds=3600):
    payload = {
        "sub": sub,
        "aud": "authenticated",
        "exp": int(time.time()) + exp_delta_seconds,
    }
    return jwt.encode(payload, private_key, algorithm="ES256")


def test_valid_token_returns_user_id(keypair, fake_jwks_client):
    private_key, _ = keypair
    token = make_token(private_key, sub="user-123")

    user_id = decode_and_get_user_id(token, fake_jwks_client)

    assert user_id == "user-123"


def test_expired_token_raises(keypair, fake_jwks_client):
    private_key, _ = keypair
    token = make_token(private_key, exp_delta_seconds=-10)

    with pytest.raises(jwt.ExpiredSignatureError):
        decode_and_get_user_id(token, fake_jwks_client)


def test_wrong_signature_raises(fake_jwks_client):
    other_private_key = ec.generate_private_key(ec.SECP256R1())
    token = make_token(other_private_key)

    with pytest.raises(jwt.PyJWTError):
        decode_and_get_user_id(token, fake_jwks_client)