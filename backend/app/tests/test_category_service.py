from app.services.category_service import validate_category_write

class FakeCategoryRecord:
    """Giả lập record category tối thiểu - chỉ cần thuộc tính user_id vì
    đó là field duy nhất validate_category_write đọc tới."""

    def __init__(self, user_id):
        self.user_id = user_id


def test_own_category_is_valid():
    record = FakeCategoryRecord(user_id="user-1")

    assert validate_category_write(record, current_user_id="user-1") is True


def test_system_category_is_rejected():
    record = FakeCategoryRecord(user_id=None)

    assert validate_category_write(record, current_user_id="user-1") is False


def test_other_user_category_is_rejected():
    record = FakeCategoryRecord(user_id="user-2")

    assert validate_category_write(record, current_user_id="user-1") is False