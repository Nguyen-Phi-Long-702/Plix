"""Bộ phân tích câu nhập nhanh tiếng Việt (rule-based)"""
import csv
import re
from functools import lru_cache
from typing import Dict, List, NamedTuple, Optional, Tuple

import asyncpg

from app.services.ai.tfidf import CSV_PATH, tokenize

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


def remove_money(text: str) -> str:
    """Bỏ số tiền đầu tiên (đúng cái extract_money lấy) khỏi `text`, giữ phần còn lại."""
    return " ".join(_MONEY_PATTERN.sub(" ", text, count=1).split())


@lru_cache(maxsize=1)
def _load_keyword_dictionary() -> Dict[str, Dict[str, int]]:
    """Từ điển từ khoá xây từ seed_categorization.csv (dùng chung với seed của TF-IDF/Naive Bayes):
    {từ: {tên category: số note của category đó có chứa từ này}}."""
    dictionary: Dict[str, Dict[str, int]] = {}
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        for row in csv.DictReader(csv_file):
            for token in set(tokenize(row["note"])):
                counts = dictionary.setdefault(token, {})
                counts[row["category"]] = counts.get(row["category"], 0) + 1
    return dictionary


def match_category(text: str) -> Tuple[Optional[str], float]:
    """Khớp từ khoá của `text` (đã bỏ số tiền) với từ điển; trả (tên category, confidence).
    Mỗi từ khớp chia 1 điểm cho các category theo tỷ lệ số note của category đó chứa từ này.
    Category có tổng điểm cao nhất thắng (bằng điểm: category xuất hiện trước trong từ điển).
    confidence = điểm của category thắng / số từ khớp, nằm trong [0, 1]: từ khoá chỉ thuộc 1
    category -> 1.0; từ khoá lẫn nhiều category -> thấp hơn. Không từ nào khớp: (None, 0.0)."""
    dictionary = _load_keyword_dictionary()
    scores: Dict[str, float] = {}
    matched_tokens = 0
    for token in dict.fromkeys(tokenize(text)):
        counts = dictionary.get(token)
        if counts is None:
            continue
        matched_tokens += 1
        total = sum(counts.values())
        for category_name, count in counts.items():
            scores[category_name] = scores.get(category_name, 0.0) + count / total
    if matched_tokens == 0:
        return None, 0.0
    best = max(scores, key=scores.get)
    return best, scores[best] / matched_tokens


class ParsedPart(NamedTuple):
    amount: Optional[int]
    category_name: Optional[str]
    confidence: float


def parse_text(text: str) -> List[ParsedPart]:
    """Tách câu thành các phần; mỗi phần ra 1 ParsedPart. Phần không có số tiền hợp lệ:
    (None, None, 0.0) = cần nhập tay, không đoán."""
    parts = []
    for part in split_sentences(text):
        amount = extract_money(part)
        if amount is None:
            parts.append(ParsedPart(None, None, 0.0))
            continue
        category_name, confidence = match_category(remove_money(part))
        parts.append(ParsedPart(amount, category_name, confidence))
    return parts


# Nguồn của item trong response /parse-nl: parser tự viết (rule-based).
SOURCE_RULE = "rule"


async def load_category_id_by_name(pool: asyncpg.Pool, user_id: str) -> Dict[str, str]:
    """Whitelist category của user (category hệ thống + category của chính user, chưa xoá):
    {tên category: category_id}. Trùng tên: lấy category hệ thống (query xếp user_id NULL lên trước)."""
    rows = await pool.fetch(
        """
        SELECT id, name FROM categories
        WHERE is_deleted = false
          AND (user_id IS NULL OR user_id = $1)
        ORDER BY user_id NULLS FIRST
        """,
        user_id,
    )
    category_id_by_name: Dict[str, str] = {}
    for row in rows:
        category_id_by_name.setdefault(row["name"], row["id"])
    return category_id_by_name


async def parse_quick_add(
    pool: asyncpg.Pool, user_id: str, text: str
) -> List[Tuple[Optional[int], Optional[str], float]]:
    """Parse câu nhập nhanh, đổi tên category sang category_id theo whitelist của user
    (category hệ thống + category của chính user, chưa xoá). Tên không có trong whitelist: None."""
    parts = parse_text(text)
    category_id_by_name = await load_category_id_by_name(pool, user_id)
    return [
        (part.amount, category_id_by_name.get(part.category_name), part.confidence)
        for part in parts
    ]