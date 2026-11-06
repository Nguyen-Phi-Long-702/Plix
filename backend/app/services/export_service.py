import csv
import io
from datetime import datetime
from typing import Iterable, Mapping

import asyncpg

from app.services.ai.forecast import VN_TZ

# BOM để Excel mở file UTF-8 hiển thị đúng tiếng Việt có dấu.
CSV_BOM = "\ufeff"
CSV_HEADER = ["Ngày giờ", "Loại", "Danh mục", "Số tiền (VND)", "Ghi chú", "Phương thức thanh toán"]

TYPE_LABELS = {"income": "Thu", "expense": "Chi"}
PAYMENT_METHOD_LABELS = {
    "cash": "Tiền mặt",
    "bank_transfer": "Chuyển khoản",
    "e_wallet": "Ví điện tử",
    "credit_card": "Thẻ tín dụng",
    "other": "Khác",
}
# Giống nhãn trong app: chưa chọn danh mục / danh mục đã bị xoá.
UNCATEGORIZED_LABEL = "Chưa phân loại"
UNKNOWN_CATEGORY_LABEL = "Danh mục không xác định"


def _category_label(row: Mapping) -> str:
    if row["category_id"] is None:
        return UNCATEGORIZED_LABEL
    return row["category_name"] or UNKNOWN_CATEGORY_LABEL


def build_transactions_csv(rows: Iterable[Mapping]) -> str:
    """Hàm thuần: dựng nội dung file CSV từ các dòng giao dịch (đã sắp xếp sẵn)."""
    buffer = io.StringIO()
    writer = csv.writer(buffer, lineterminator="\r\n")
    writer.writerow(CSV_HEADER)
    for row in rows:
        occurred_at = datetime.fromtimestamp(row["occurred_at"] / 1000, VN_TZ)
        writer.writerow(
            [
                occurred_at.strftime("%d/%m/%Y %H:%M"),
                TYPE_LABELS.get(row["type"], row["type"]),
                _category_label(row),
                row["amount"],
                row["note"] or "",
                PAYMENT_METHOD_LABELS.get(row["payment_method"], ""),
            ]
        )
    return CSV_BOM + buffer.getvalue()


async def fetch_transactions_for_export(pool: asyncpg.Pool, user_id: str) -> list:
    return await pool.fetch(
        """
        SELECT t.occurred_at, t.type, t.category_id, c.name AS category_name,
               t.amount, t.note, t.payment_method
        FROM transactions t
        LEFT JOIN categories c
          ON c.id = t.category_id
         AND c.is_deleted = false
         AND (c.user_id IS NULL OR c.user_id = $1)
        WHERE t.user_id = $1
          AND t.is_deleted = false
        ORDER BY t.occurred_at DESC
        """,
        user_id,
    )