"""ValidationService: kiểm tra kết quả Gemini trả về (schema + whitelist category) trước khi dùng"""
import json
from typing import Any, Dict, List, NamedTuple, Optional


class ValidatedItem(NamedTuple):
    amount: int
    category_id: str
    confidence: float


def validate_gemini_items(
    raw_text: Optional[str], category_id_by_name: Dict[str, str]
) -> List[ValidatedItem]:
    """Parse text JSON Gemini trả về, chỉ giữ item đúng schema {amount, category, confidence}
    và có category nằm trong whitelist (đổi tên category sang category_id).
    Item sai schema hoặc category ngoài whitelist bị loại. JSON hỏng / sai cấu trúc ngoài cùng
    / không có nội dung: trả list rỗng, không raise."""
    if raw_text is None:
        return []
    try:
        data = json.loads(raw_text)
    except ValueError:
        return []
    if not isinstance(data, dict):
        return []
    raw_items = data.get("items")
    if not isinstance(raw_items, list):
        return []
    validated: List[ValidatedItem] = []
    for raw_item in raw_items:
        item = _validate_item(raw_item, category_id_by_name)
        if item is not None:
            validated.append(item)
    return validated


def _validate_item(raw_item: Any, category_id_by_name: Dict[str, str]) -> Optional[ValidatedItem]:
    if not isinstance(raw_item, dict):
        return None
    amount = raw_item.get("amount")
    category_name = raw_item.get("category")
    confidence = raw_item.get("confidence")
    # bool là subclass của int trong Python -> loại riêng để true/false không lọt qua thành 1/0
    if isinstance(amount, bool) or not isinstance(amount, int) or amount <= 0:
        return None
    if not isinstance(category_name, str) or category_name not in category_id_by_name:
        return None
    if isinstance(confidence, bool) or not isinstance(confidence, (int, float)):
        return None
    if not 0 <= confidence <= 1:
        return None
    return ValidatedItem(amount, category_id_by_name[category_name], float(confidence))