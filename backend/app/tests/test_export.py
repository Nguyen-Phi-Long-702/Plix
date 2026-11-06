from datetime import datetime

from fastapi import FastAPI
from fastapi.testclient import TestClient

import app.core.rate_limit as rate_limit_module
from app.core.rate_limit import ExportRateLimitedError, check_export_cooldown
from app.core.security import verify_jwt
from app.routers import export
from app.services.ai.forecast import VN_TZ
from app.services.export_service import CSV_BOM, CSV_HEADER, build_transactions_csv


def ms(year, month, day, hour, minute=0):
    return round(datetime(year, month, day, hour, minute, tzinfo=VN_TZ).timestamp() * 1000)


def _row(**overrides):
    row = {
        "occurred_at": ms(2026, 11, 4, 9, 5),
        "type": "expense",
        "category_id": "sys_an_uong",
        "category_name": "Ăn uống",
        "amount": 45000,
        "note": "phở bò",
        "payment_method": "cash",
    }
    row.update(overrides)
    return row


HEADER_LINE = ",".join(CSV_HEADER) + "\r\n"


# ---------- 1. Dựng nội dung CSV ----------


def test_csv_without_transactions_has_only_bom_and_header():
    assert build_transactions_csv([]) == CSV_BOM + HEADER_LINE


def test_csv_row_uses_vietnam_time_and_vietnamese_labels():
    csv_text = build_transactions_csv([_row()])

    assert csv_text == CSV_BOM + HEADER_LINE + "04/11/2026 09:05,Chi,Ăn uống,45000,phở bò,Tiền mặt\r\n"


def test_csv_income_row_and_missing_payment_method():
    csv_text = build_transactions_csv(
        [_row(type="income", category_id="sys_luong", category_name="Lương", note="lương tháng", payment_method=None)]
    )

    assert csv_text.endswith("04/11/2026 09:05,Thu,Lương,45000,lương tháng,\r\n")


def test_csv_category_labels_for_uncategorized_and_deleted_category():
    uncategorized = _row(category_id=None, category_name=None)
    deleted = _row(category_id="demo-cat-da-xoa", category_name=None)

    csv_text = build_transactions_csv([uncategorized, deleted])

    assert ",Chưa phân loại," in csv_text
    assert ",Danh mục không xác định," in csv_text


def test_csv_quotes_note_containing_comma_and_handles_empty_note():
    csv_text = build_transactions_csv([_row(note="cơm, nước"), _row(note=None)])

    assert ',"cơm, nước",' in csv_text
    assert ",45000,,Tiền mặt\r\n" in csv_text


# ---------- 2. Giới hạn tần suất ----------


class FakeClock:
    def __init__(self, now):
        self.now = now

    def monotonic(self):
        return self.now


def test_export_cooldown_blocks_second_export_of_same_user(monkeypatch):
    monkeypatch.setattr(rate_limit_module, "_last_export_at", {})
    monkeypatch.setattr(rate_limit_module, "EXPORT_COOLDOWN_SECONDS", 60)
    clock = FakeClock(1000.0)
    monkeypatch.setattr(rate_limit_module, "time", clock)
    check_export_cooldown("user-1")

    clock.now = 1020.5
    try:
        check_export_cooldown("user-1")
        assert False, "phải raise ExportRateLimitedError"
    except ExportRateLimitedError as error:
        assert error.retry_after_seconds == 40


def test_export_cooldown_is_per_user_and_expires(monkeypatch):
    monkeypatch.setattr(rate_limit_module, "_last_export_at", {})
    monkeypatch.setattr(rate_limit_module, "EXPORT_COOLDOWN_SECONDS", 60)
    clock = FakeClock(1000.0)
    monkeypatch.setattr(rate_limit_module, "time", clock)
    check_export_cooldown("user-1")

    check_export_cooldown("user-2")  # user khác không bị ảnh hưởng
    clock.now = 1060.0
    check_export_cooldown("user-1")  # đủ 60 giây thì được xuất lại


# ---------- 3. Endpoint ----------


class FakeExportPool:
    def __init__(self, rows):
        self._rows = rows
        self.fetch_calls = []

    async def fetch(self, query, *args):
        assert "FROM transactions" in query
        self.fetch_calls.append(args)
        return self._rows


def _build_client(pool, monkeypatch):
    monkeypatch.setattr(rate_limit_module, "_last_export_at", {})
    test_app = FastAPI()
    test_app.state.db_pool = pool
    test_app.include_router(export.router, prefix="/api/v1")
    test_app.dependency_overrides[verify_jwt] = lambda: "user-1"
    return TestClient(test_app)


def test_export_endpoint_returns_csv_file_of_the_callers_transactions(monkeypatch):
    pool = FakeExportPool([_row()])
    client = _build_client(pool, monkeypatch)

    response = client.get("/api/v1/export/csv")

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/csv")
    assert response.headers["content-disposition"] == 'attachment; filename="plix-giao-dich.csv"'
    assert response.content.decode("utf-8") == build_transactions_csv([_row()])
    assert pool.fetch_calls == [("user-1",)]


def test_export_endpoint_rejects_second_export_within_cooldown(monkeypatch):
    pool = FakeExportPool([_row()])
    client = _build_client(pool, monkeypatch)

    first = client.get("/api/v1/export/csv")
    second = client.get("/api/v1/export/csv")

    assert first.status_code == 200
    assert second.status_code == 429
    assert "giây" in second.json()["detail"]
    assert len(pool.fetch_calls) == 1  # lần bị chặn không chạm DB