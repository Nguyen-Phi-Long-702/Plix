import time

import jwt
from cryptography.hazmat.primitives.asymmetric import ec

from app.models.sync import SyncTable

PRIVATE_KEY = ec.generate_private_key(ec.SECP256R1())
ATTACKER_KEY = ec.generate_private_key(ec.SECP256R1())

USER_A = "user-a"
USER_B = "user-b"

# Mọi endpoint cần JWT của app.main.app: (method, path, tham số gửi kèm request).
# Endpoint public duy nhất là /api/v1/health nên không nằm trong danh sách này.
PROTECTED_ENDPOINTS = [
    ("GET", "/api/v1/whoami", {}),
    ("GET", "/api/v1/categories", {}),
    ("GET", "/api/v1/anomaly", {"params": {"category_id": "cat-1", "amount": 50000}}),
    ("GET", "/api/v1/forecast", {}),
    ("POST", "/api/v1/categorize", {"json": {"note": "cà phê"}}),
    (
        "POST",
        "/api/v1/correction",
        {"json": {"transaction_id": "tx-1", "corrected_category_id": "cat-1"}},
    ),
    ("POST", "/api/v1/retrain", {}),
    ("POST", "/api/v1/parse-nl", {"json": {"text": "phở 50k"}}),
]
for _table in SyncTable:
    PROTECTED_ENDPOINTS.append(
        ("POST", f"/api/v1/sync/{_table.value}/push", {"json": {"records": []}})
    )
    PROTECTED_ENDPOINTS.append(
        ("GET", f"/api/v1/sync/{_table.value}/pull", {"params": {"since": 0}})
    )

ENDPOINT_IDS = [f"{method} {path}" for method, path, _ in PROTECTED_ENDPOINTS]


class FakeSigningKey:
    def __init__(self, key):
        self.key = key


class FakeJwksClient:
    """Giả lập PyJWKClient: luôn trả public key test cục bộ, không gọi mạng tới Supabase."""

    def get_signing_key_from_jwt(self, token):
        return FakeSigningKey(PRIVATE_KEY.public_key())


class RecordingPool:
    """Giả lập asyncpg.Pool: ghi lại mọi lần chạm vào DB (query, args) và luôn trả kết quả rỗng."""

    def __init__(self):
        self.calls = []

    async def fetch(self, query, *args):
        self.calls.append((query, args))
        return []

    async def fetchrow(self, query, *args):
        self.calls.append((query, args))
        return None

    async def fetchval(self, query, *args):
        self.calls.append((query, args))
        return None

    async def execute(self, query, *args):
        self.calls.append((query, args))


def make_token(sub=USER_A, exp_delta_seconds=3600, key=PRIVATE_KEY):
    payload = {
        "sub": sub,
        "aud": "authenticated",
        "exp": int(time.time()) + exp_delta_seconds,
    }
    return jwt.encode(payload, key, algorithm="ES256")


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"}