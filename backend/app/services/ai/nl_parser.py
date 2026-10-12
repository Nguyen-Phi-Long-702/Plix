"""Bộ phân tích câu nhập nhanh tiếng Việt (rule-based)"""
import re
from typing import List

# Tách theo dấu phẩy và các liên từ phổ biến "và", "rồi" (chỉ khi đứng riêng thành 1 từ).
_SPLIT_PATTERN = re.compile(r",|\b(?:và|rồi)\b", re.IGNORECASE)


def split_sentences(text: str) -> List[str]:
    """Tách câu nhập nhanh thành các phần; mỗi phần là 1 giao dịch ứng viên.
    Tách theo dấu phẩy và liên từ "và", "rồi"; bỏ khoảng trắng thừa và phần rỗng."""
    parts = (part.strip() for part in _SPLIT_PATTERN.split(text))
    return [part for part in parts if part]