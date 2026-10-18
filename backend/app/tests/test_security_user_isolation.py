import json
from datetime import datetime

import pytest
from fastapi.testclient import TestClient

from app.core.security import get_jwks_client
from app.main import app
from app.routers import forecast
from app.services.ai.forecast import VN_TZ
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.tfidf import TfidfVectorizer
from app.services.ai.train_seed_model import SEED_USER_ID
from app.tests.security_helpers import (
    ENDPOINT_IDS,
    PROTECTED_ENDPOINTS,
    USER_A,
    USER_B,
    FakeJwksClient,
    RecordingPool,
    auth_headers,
    make_token,
)


def ms(year, month, day, hour=12):
    return round(datetime(year, month, day, hour, tzinfo=VN_TZ).timestamp() * 1000)


# "Bây giờ" cố định: thứ Tư 14/10/2026 12:00 (giờ VN).
NOW = ms(2026, 10, 14)


def _category(category_id, user_id, name):
    return {
        "id": category_id,
        "user_id": user_id,
        "name": name,
        "type": "expense",
        "updated_at": 1000,
        "is_deleted": False,
    }


# user_id None = category hệ thống (ai cũng thấy); còn lại chỉ chủ sở hữu thấy.
CATEGORIES = [
    _category("sys_an_uong", None, "Ăn uống"),
    _category("cat-a", USER_A, "Cà phê của A"),
    _category("cat-b", USER_B, "Quỹ riêng của B"),
    _category("cat-b-di-chuyen", USER_B, "Di chuyển"),
]


def _transaction(transaction_id, user_id, category_id, amount, occurred_at):
    return {
        "id": transaction_id,
        "user_id": user_id,
        "category_id": category_id,
        "type": "expense",
        "amount": amount,
        "occurred_at": occurred_at,
        "is_deleted": False,
    }


# A: 5 giao dịch 40.000đ + 5 giao dịch 50.000đ cho cat-a (trung bình 45.000đ), cộng 4 tuần liên tiếp
# mỗi tuần chi 100.000đ (đủ lịch sử để dự báo). B: 10 giao dịch 5.000.000đ cũng gắn cat-a nhưng chỉ
# nằm trong tuần hiện tại (không đủ lịch sử để dự báo).
TRANSACTIONS = (
    [
        _transaction(f"a-{i}", USER_A, "cat-a", 40000 if i < 5 else 50000, NOW)
        for i in range(10)
    ]
    + [
        _transaction(f"a-week-{i}", USER_A, None, 100000, occurred_at)
        for i, occurred_at in enumerate(
            [ms(2026, 9, 15), ms(2026, 9, 22), ms(2026, 9, 29), ms(2026, 10, 6)]
        )
    ]
    + [
        _transaction(f"b-{i}", USER_B, "cat-a", 5000000, ms(2026, 10, 13))
        for i in range(10)
    ]
)

# Model seed dùng chung: "xe buýt" -> "Di chuyển".
_SEED_NOTES = ["phở bò", "cơm gà", "xe buýt", "tàu hoả"]
_SEED_LABELS = ["Ăn uống", "Ăn uống", "Di chuyển", "Di chuyển"]


def _seed_model_row():
    vectorizer = TfidfVectorizer()
    vectorizer.fit(_SEED_NOTES)
    classifier = NaiveBayesClassifier()
    classifier.fit(_SEED_NOTES, _SEED_LABELS)
    return {
        "idf": json.dumps(vectorizer._idf),
        "class_priors": json.dumps(classifier._log_prior),
        "likelihoods": json.dumps(classifier._log_likelihood),
        "trained_at": None,
        "training_sample_count": 0,
        "is_training": False,
    }


class TwoUserPool:
    """Giả lập asyncpg.Pool có dữ liệu của 2 user. Mỗi query trả đúng phần dữ liệu của user_id
    mà code truyền vào tham số — nên nếu endpoint truyền nhầm user_id thì kết quả lộ dữ liệu user kia."""

    def __init__(self):
        self.inserted = []
        self._seed_row = _seed_model_row()

    @staticmethod
    def _visible_categories(user_id):
        return [
            c for c in CATEGORIES
            if not c["is_deleted"] and (c["user_id"] is None or c["user_id"] == user_id)
        ]

    async def fetch(self, query, *args):
        if "SELECT id, user_id, name, type, updated_at, is_deleted" in query:  # GET /categories
            return self._visible_categories(args[0])
        if "SELECT id, name FROM categories" in query:  # /parse-nl
            return [{"id": c["id"], "name": c["name"]} for c in self._visible_categories(args[0])]
        if "SELECT amount FROM transactions" in query:  # /anomaly
            user_id, category_id = args
            return [
                {"amount": t["amount"]} for t in TRANSACTIONS
                if t["user_id"] == user_id and t["category_id"] == category_id
            ]
        if "SELECT type, amount, occurred_at FROM transactions" in query:  # /forecast
            return [t for t in TRANSACTIONS if t["user_id"] == args[0]]
        raise AssertionError(f"fetch không mong đợi trong test: {query}")

    async def fetchrow(self, query, *args):
        if "SELECT name FROM categories" in query:  # /anomaly lấy tên danh mục
            category_id, user_id = args
            for c in self._visible_categories(user_id):
                if c["id"] == category_id:
                    return {"name": c["name"]}
            return None
        if "ai_model_params" in query:  # /categorize: chỉ có model seed dùng chung
            return self._seed_row if args[0] == SEED_USER_ID else None
        if "SELECT id FROM categories" in query:  # /categorize đổi tên danh mục -> id
            user_id, name = args
            for c in self._visible_categories(user_id):
                if c["name"] == name:
                    return {"id": c["id"]}
            return None
        if "SELECT user_id FROM transactions" in query:  # /correction kiểm tra chủ giao dịch
            (transaction_id,) = args
            owners = {"tx-of-a": USER_A, "tx-of-b": USER_B}
            return {"user_id": owners[transaction_id]} if transaction_id in owners else None
        raise AssertionError(f"fetchrow không mong đợi trong test: {query}")

    async def execute(self, query, *args):
        self.inserted.append(args)


@pytest.fixture
def api(monkeypatch):
    # Dùng app thật (app.main.app) + verify_jwt thật; chỉ thay nguồn public key và pool bằng bản giả.
    monkeypatch.setattr(forecast, "_now_ms", lambda: NOW)
    pool = TwoUserPool()
    app.state.db_pool = pool
    app.dependency_overrides[get_jwks_client] = lambda: FakeJwksClient()
    yield TestClient(app), pool
    app.dependency_overrides.clear()


@pytest.fixture
def recording_api():
    pool = RecordingPool()
    app.state.db_pool = pool
    app.dependency_overrides[get_jwks_client] = lambda: FakeJwksClient()
    yield TestClient(app), pool
    app.dependency_overrides.clear()


def _as(user_id):
    return auth_headers(make_token(sub=user_id))


# ---------- 1. Mọi endpoint: user_id giả do client gửi không bao giờ xuống DB ----------


@pytest.mark.parametrize("method,path,kwargs", PROTECTED_ENDPOINTS, ids=ENDPOINT_IDS)
def test_user_id_sent_by_client_never_reaches_the_database(
    recording_api, method, path, kwargs
):
    client, pool = recording_api
    # Giả lập user A đăng nhập thật nhưng cố tình khai mã người dùng khác (user-b) trong cả query lẫn body.
    request_kwargs = dict(kwargs)
    request_kwargs["params"] = {**kwargs.get("params", {}), "user_id": USER_B}
    if "json" in kwargs:
        request_kwargs["json"] = {**kwargs["json"], "user_id": USER_B}

    response = client.request(method, path, headers=_as(USER_A), **request_kwargs)

    assert response.status_code != 401  # token của A hợp lệ, request đã vào tới handler
    assert USER_B not in repr(pool.calls)
    assert USER_B not in response.text


def test_whoami_returns_the_user_of_the_token_not_the_one_sent_by_client(api):
    client, _ = api

    response = client.get("/api/v1/whoami", params={"user_id": USER_B}, headers=_as(USER_A))

    assert response.json() == {"user_id": USER_A}


# ---------- 2. Từng endpoint đọc dữ liệu: mỗi user chỉ thấy dữ liệu của chính mình ----------


def test_categories_returns_system_and_own_categories_only(api):
    client, _ = api

    ids_of_a = {c["id"] for c in client.get("/api/v1/categories", headers=_as(USER_A)).json()}
    ids_of_b = {c["id"] for c in client.get("/api/v1/categories", headers=_as(USER_B)).json()}

    assert ids_of_a == {"sys_an_uong", "cat-a"}
    assert ids_of_b == {"sys_an_uong", "cat-b", "cat-b-di-chuyen"}


def test_categories_ignores_user_id_sent_in_query(api):
    client, _ = api

    response = client.get(
        "/api/v1/categories", params={"user_id": USER_B}, headers=_as(USER_A)
    )

    assert {c["id"] for c in response.json()} == {"sys_an_uong", "cat-a"}


def test_anomaly_uses_only_the_callers_own_transactions(api):
    client, _ = api
    params = {"category_id": "cat-a", "amount": 60000}

    response_a = client.get("/api/v1/anomaly", params=params, headers=_as(USER_A))
    response_b = client.get("/api/v1/anomaly", params=params, headers=_as(USER_B))

    # A: dữ liệu riêng của A (trung bình 45.000đ) -> 60.000đ là cao bất thường, thấy tên danh mục của A.
    assert response_a.json() == {
        "status": "flagged_high",
        "explanation": (
            "Giao dịch này cao hơn mức chi tiêu trung bình cho danh mục [Cà phê của A] của bạn "
            "(trung bình 45.000đ, giao dịch này 60.000đ)."
        ),
    }
    # B: chỉ dữ liệu riêng của B (5.000.000đ) -> 60.000đ là thấp bất thường; không thấy tên danh mục của A.
    assert response_b.json() == {
        "status": "flagged_low",
        "explanation": (
            "Giao dịch này thấp hơn mức chi tiêu trung bình cho danh mục [không xác định] của bạn "
            "(trung bình 5.000.000đ, giao dịch này 60.000đ)."
        ),
    }


def test_forecast_uses_only_the_callers_own_transactions(api):
    client, _ = api

    response_a = client.get("/api/v1/forecast", headers=_as(USER_A))
    response_b = client.get("/api/v1/forecast", headers=_as(USER_B))

    assert response_a.json() == {"status": "ok", "forecast_net_amount": -800000}
    assert response_b.json() == {"status": "insufficient_data", "forecast_net_amount": None}


def test_parse_nl_maps_categories_only_from_the_callers_own_whitelist(api):
    client, _ = api
    body = {"text": "grab 35k"}

    items_a = client.post("/api/v1/parse-nl", json=body, headers=_as(USER_A)).json()["items"]
    items_b = client.post("/api/v1/parse-nl", json=body, headers=_as(USER_B)).json()["items"]

    assert items_a[0]["category_id"] is None  # A không có danh mục "Di chuyển"
    assert items_b[0]["category_id"] == "cat-b-di-chuyen"


def test_categorize_returns_only_category_ids_the_caller_can_see(api):
    client, _ = api
    body = {"note": "xe buýt"}

    response_a = client.post("/api/v1/categorize", json=body, headers=_as(USER_A))
    response_b = client.post("/api/v1/categorize", json=body, headers=_as(USER_B))

    assert response_a.json()["category_id"] is None  # A không có danh mục "Di chuyển"
    assert response_b.json()["category_id"] == "cat-b-di-chuyen"


# ---------- 3. Endpoint ghi: không thao tác được trên dữ liệu của user khác ----------


def test_correction_on_transaction_of_another_user_is_rejected(api):
    client, pool = api

    response = client.post(
        "/api/v1/correction",
        json={"transaction_id": "tx-of-b", "corrected_category_id": "sys_an_uong"},
        headers=_as(USER_A),
    )

    assert response.status_code == 403
    assert pool.inserted == []


def test_correction_on_own_transaction_is_saved_for_the_caller(api):
    client, pool = api

    response = client.post(
        "/api/v1/correction",
        json={"transaction_id": "tx-of-a", "corrected_category_id": "sys_an_uong"},
        headers=_as(USER_A),
    )

    assert response.status_code == 201
    assert len(pool.inserted) == 1
    assert pool.inserted[0][1] == USER_A  # tham số thứ 2 của INSERT là user_id