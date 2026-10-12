"""Bộ phân tích câu nhập nhanh tiếng Việt (rule-based)"""
import re
from typing import List, Optional

THOUSAND_MULTIPLIER = 1000

# Tách theo dấu phẩy và các liên từ phổ biến "và", "rồi" (chỉ khi đứng riêng thành 1 từ).
_SPLIT_PATTERN = re.compile(r",|\b(?:và|rồi)\b", re.IGNORECASE)

# Dạng 1: số + "k"/"nghìn"/"ngàn" (nhân 1.000), ví dụ "50k", "50 nghìn".
# Dạng 2: số có dấu chấm phân cách nghìn hoặc số trần, kèm "đ"/"vnd" hoặc không kèm đơn vị
#         (coi là VNĐ nguyên), ví dụ "500.000đ", "500.000 VND", "35000".
# (?!\w) ở cuối mỗi dạng: đơn vị phải đứng riêng, không dính vào chữ phía sau
# (ví dụ "2 kem" không được hiểu là "2k" + "em").
_MONEY_PATTERN = re.compile(
    r"(?P<thousand_count>\d+)\s*(?:k|nghìn|ngàn)(?!\w)"
    r"|"
    r"(?P<vnd_number>\d{1,3}(?:\.\d{3})+|\d+)(?:\s*(?:đ|vnd))?(?!\w)",
    re.IGNORECASE,
)


def split_sentences(text: str) -> List[str]:
    """Tách câu nhập nhanh thành các phần; mỗi phần là 1 giao dịch ứng viên.
    Tách theo dấu phẩy và liên từ "và", "rồi"; bỏ khoảng trắng thừa và phần rỗng."""
    parts = (part.strip() for part in _SPLIT_PATTERN.split(text))
    return [part for part in parts if part]


def extract_money(text: str) -> Optional[int]:
    """Tìm số tiền đầu tiên (từ trái sang phải) trong `text`, trả về VNĐ nguyên
    (ví dụ "50k" -> 50000). Không có số tiền nào: trả None."""
    match = _MONEY_PATTERN.search(text)
    if match is None:
        return None
    if match.group("thousand_count") is not None:
        return int(match.group("thousand_count")) * THOUSAND_MULTIPLIER
    return int(match.group("vnd_number").replace(".", ""))