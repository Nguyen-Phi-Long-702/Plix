import pytest

from app.services.validation_service import ValidatedItem, validate_gemini_items

WHITELIST = {"Ăn uống": "sys_an_uong", "Di chuyển": "sys_di_chuyen"}


def test_valid_items_are_kept_and_category_name_is_mapped_to_id():
    raw = (
        '{"items": ['
        '{"amount": 50000, "category": "Ăn uống", "confidence": 0.9},'
        '{"amount": 35000, "category": "Di chuyển", "confidence": 0.7}'
        "]}"
    )

    assert validate_gemini_items(raw, WHITELIST) == [
        ValidatedItem(50000, "sys_an_uong", 0.9),
        ValidatedItem(35000, "sys_di_chuyen", 0.7),
    ]


def test_item_with_category_outside_whitelist_is_dropped():
    raw = (
        '{"items": ['
        '{"amount": 50000, "category": "Du lịch", "confidence": 0.9},'
        '{"amount": 35000, "category": "Di chuyển", "confidence": 0.7}'
        "]}"
    )

    assert validate_gemini_items(raw, WHITELIST) == [ValidatedItem(35000, "sys_di_chuyen", 0.7)]


@pytest.mark.parametrize("amount", ['"50k"', "0", "-5000", "true", "50000.5", "null"])
def test_item_with_invalid_amount_is_dropped(amount):
    raw = '{"items": [{"amount": %s, "category": "Ăn uống", "confidence": 0.9}]}' % amount

    assert validate_gemini_items(raw, WHITELIST) == []


@pytest.mark.parametrize("confidence", ['"cao"', "1.5", "-0.1", "true", "null"])
def test_item_with_invalid_confidence_is_dropped(confidence):
    raw = '{"items": [{"amount": 50000, "category": "Ăn uống", "confidence": %s}]}' % confidence

    assert validate_gemini_items(raw, WHITELIST) == []


def test_confidence_boundaries_zero_and_one_are_accepted():
    raw = (
        '{"items": ['
        '{"amount": 1000, "category": "Ăn uống", "confidence": 0},'
        '{"amount": 2000, "category": "Ăn uống", "confidence": 1}'
        "]}"
    )

    assert validate_gemini_items(raw, WHITELIST) == [
        ValidatedItem(1000, "sys_an_uong", 0.0),
        ValidatedItem(2000, "sys_an_uong", 1.0),
    ]


def test_item_missing_a_required_field_is_dropped():
    raw = (
        '{"items": ['
        '{"amount": 50000, "confidence": 0.9},'
        '{"category": "Ăn uống", "confidence": 0.9},'
        '{"amount": 50000, "category": "Ăn uống"}'
        "]}"
    )

    assert validate_gemini_items(raw, WHITELIST) == []


def test_item_that_is_not_an_object_is_dropped():
    raw = '{"items": ["phở 50k", 5, null, {"amount": 50000, "category": "Ăn uống", "confidence": 0.9}]}'

    assert validate_gemini_items(raw, WHITELIST) == [ValidatedItem(50000, "sys_an_uong", 0.9)]


def test_empty_items_list_returns_empty_list():
    assert validate_gemini_items('{"items": []}', WHITELIST) == []


@pytest.mark.parametrize(
    "raw",
    [None, "", "không phải JSON", '{"items": [', "[]", '"abc"', "{}", '{"items": "x"}', '{"items": null}'],
)
def test_malformed_or_wrong_shape_returns_empty_list(raw):
    assert validate_gemini_items(raw, WHITELIST) == []