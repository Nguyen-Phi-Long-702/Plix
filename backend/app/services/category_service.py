from typing import Any

def validate_category_write(record: Any, current_user_id: str) -> bool:
    if record.user_id is None:
        return False
    if record.user_id != current_user_id:
        return False
    return True