"""Gemini fallback cho /parse-nl: gọi Gemini khi parser rule-based không xử lý được cả câu"""
import asyncio
import logging
from typing import Any, List, Optional, Sequence, Tuple

import asyncpg
import httpx

from app.core.config import GEMINI_API_KEY
from app.services.ai.nl_parser import load_category_id_by_name
from app.services.validation_service import ValidatedItem, validate_gemini_items

logger = logging.getLogger(__name__)

# Nguồn của item trong response /parse-nl: Gemini (xem SOURCE_RULE trong nl_parser.py).
SOURCE_GEMINI = "gemini"

# Gemini lỗi/hết hạn mức -> /parse-nl trả HTTP 503 với error_code này để app hiện "AI đang bận, nhập tay".
AI_BUSY_ERROR_CODE = "AI_BUSY"
AI_BUSY_MESSAGE = "AI đang bận, vui lòng nhập tay"

# Chỉ dùng Gemini Flash/Flash-Lite, KHÔNG dùng Pro (Pro đã tính phí từ lần gọi đầu).
GEMINI_MODEL = "gemini-3.5-flash-lite"
GEMINI_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent"
GEMINI_TIMEOUT_SECONDS = 8.0

# Chỉ gọi Gemini khi MỌI phần của câu đều có confidence dưới ngưỡng này.
# 0.4 = cùng ngưỡng "độ tin cậy thấp" mà app Android đang dùng (không tự điền category khi < 0.4).
RULE_CONFIDENCE_THRESHOLD = 0.4

# Schema cố định Gemini phải trả về: {"items": [{"amount", "category", "confidence"}]}
RESPONSE_SCHEMA = {
    "type": "OBJECT",
    "properties": {
        "items": {
            "type": "ARRAY",
            "items": {
                "type": "OBJECT",
                "properties": {
                    "amount": {"type": "INTEGER"},
                    "category": {"type": "STRING"},
                    "confidence": {"type": "NUMBER"},
                },
                "required": ["amount", "category", "confidence"],
            },
        }
    },
    "required": ["items"],
}


class GeminiUnavailableError(Exception):
    """Gemini không dùng được: hết hạn mức (429), quá thời gian chờ, lỗi mạng/máy chủ, thiếu API key."""


def should_call_gemini(parsed: Sequence[Tuple[Optional[int], Optional[str], float]]) -> bool:
    """Chỉ gọi Gemini khi parser rule-based có ít nhất 1 phần và TẤT CẢ các phần đều có
    confidence dưới ngưỡng (toàn bộ câu không xử lý được; không gọi cho từng phần riêng lẻ)."""
    return len(parsed) > 0 and all(
        confidence < RULE_CONFIDENCE_THRESHOLD for _, _, confidence in parsed
    )


def build_prompt(text: str, category_names: Sequence[str]) -> str:
    categories = "\n".join(f"- {name}" for name in category_names)
    return (
        "Bạn là bộ phân tích câu nhập nhanh chi tiêu/thu nhập tiếng Việt. "
        "Hãy tách câu của người dùng thành các giao dịch riêng lẻ.\n"
        "Quy tắc:\n"
        '- Chỉ trả về giao dịch có số tiền rõ ràng. amount là số nguyên VNĐ (ví dụ "50k" = 50000).\n'
        "- category chỉ được chọn đúng 1 tên trong danh sách danh mục bên dưới, giữ nguyên chữ.\n"
        "- confidence là số từ 0 đến 1: mức chắc chắn của bạn về category đã chọn.\n"
        "- Không có giao dịch nào rõ ràng thì trả items là danh sách rỗng.\n"
        f"Danh sách danh mục:\n{categories}\n"
        'Ví dụ định dạng kết quả: {"items": [{"amount": 50000, "category": "<tên danh mục>", "confidence": 0.9}]}\n'
        f"Câu của người dùng:\n{text}"
    )


def build_request_body(text: str, category_names: Sequence[str]) -> dict:
    return {
        "contents": [{"parts": [{"text": build_prompt(text, category_names)}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": RESPONSE_SCHEMA,
        },
    }


def extract_response_text(payload: Any) -> Optional[str]:
    """Lấy text JSON trong câu trả lời của Gemini; sai cấu trúc (vd bị chặn, không có candidate): None."""
    try:
        text = payload["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError, TypeError):
        return None
    return text if isinstance(text, str) else None


async def call_gemini(text: str, category_names: Sequence[str]) -> Optional[str]:
    """Gọi Gemini (timeout tổng 8 giây), trả text JSON thô của câu trả lời.
    Raise GeminiUnavailableError khi thiếu API key, lỗi mạng, quá thời gian chờ hoặc HTTP khác 200.
    Không log API key và không log nội dung câu người dùng nhập."""
    if not GEMINI_API_KEY:
        logger.warning("Gemini fallback: thiếu GEMINI_API_KEY")
        raise GeminiUnavailableError("missing_api_key")
    try:
        async with httpx.AsyncClient(timeout=GEMINI_TIMEOUT_SECONDS) as client:
            response = await asyncio.wait_for(
                client.post(
                    GEMINI_URL,
                    headers={"x-goog-api-key": GEMINI_API_KEY},
                    json=build_request_body(text, category_names),
                ),
                timeout=GEMINI_TIMEOUT_SECONDS,
            )
    except (httpx.HTTPError, asyncio.TimeoutError) as error:
        logger.warning("Gemini fallback: lỗi kết nối hoặc quá thời gian chờ (%s)", type(error).__name__)
        raise GeminiUnavailableError(type(error).__name__) from error
    if response.status_code != 200:
        logger.warning("Gemini fallback: Gemini trả HTTP %s", response.status_code)
        raise GeminiUnavailableError(f"http_{response.status_code}")
    try:
        payload = response.json()
    except ValueError as error:
        logger.warning("Gemini fallback: phản hồi không phải JSON")
        raise GeminiUnavailableError("invalid_json_body") from error
    return extract_response_text(payload)


async def parse_with_gemini(pool: asyncpg.Pool, user_id: str, text: str) -> List[ValidatedItem]:
    """Nhờ Gemini parse câu, rồi chỉ giữ item hợp lệ (đúng schema + category trong whitelist của user).
    Chỉ gửi câu người dùng nhập và tên category của họ lên Gemini, không gửi lịch sử giao dịch.
    Raise GeminiUnavailableError khi Gemini không dùng được."""
    category_id_by_name = await load_category_id_by_name(pool, user_id)
    raw_text = await call_gemini(text, list(category_id_by_name))
    return validate_gemini_items(raw_text, category_id_by_name)