import asyncio

import pytest

from app.services.ai.nl_parser import (
    extract_money,
    match_category,
    parse_quick_add,
    parse_text,
    remove_money,
    split_sentences,
)


def test_split_by_comma():
    assert split_sentences("phở 50k, grab 35k") == ["phở 50k", "grab 35k"]


def test_split_by_va_conjunction():
    assert split_sentences("ăn phở 50k và uống cà phê 30k") == ["ăn phở 50k", "uống cà phê 30k"]


def test_split_by_roi_conjunction():
    assert split_sentences("đổ xăng 100k rồi mua nước 10k") == ["đổ xăng 100k", "mua nước 10k"]


def test_split_mixed_separators_into_many_parts():
    assert split_sentences("phở 50k, grab 35k và cà phê 20k rồi bánh 10k") == [
        "phở 50k",
        "grab 35k",
        "cà phê 20k",
        "bánh 10k",
    ]


def test_split_keeps_word_that_merely_starts_with_va():
    # "vàng" bắt đầu bằng "và" nhưng là 1 từ khác, không được tách
    assert split_sentences("mua vàng 500k") == ["mua vàng 500k"]


def test_split_conjunction_is_case_insensitive():
    assert split_sentences("Phở 50k VÀ grab 35k") == ["Phở 50k", "grab 35k"]


def test_split_without_separator_returns_single_part():
    assert split_sentences("phở 50k") == ["phở 50k"]


def test_split_drops_empty_parts():
    assert split_sentences("phở 50k,, grab 35k,") == ["phở 50k", "grab 35k"]


def test_split_empty_or_blank_text_returns_empty_list():
    assert split_sentences("") == []
    assert split_sentences("   ") == []


@pytest.mark.parametrize(
    "text, expected",
    [
        ("50k", 50000),
        ("50K", 50000),
        ("50 nghìn", 50000),
        ("50 ngàn", 50000),
        ("500.000đ", 500000),
        ("1.500.000đ", 1500000),
        ("500.000 VND", 500000),
        ("50000đ", 50000),
        ("50000", 50000),
        ("35", 35),
        ("500.000", 500000),
    ],
)
def test_extract_money_common_formats(text, expected):
    assert extract_money(text) == expected


def test_extract_money_finds_amount_inside_sentence():
    assert extract_money("phở 50k") == 50000


def test_extract_money_returns_none_when_no_amount():
    assert extract_money("ăn phở") is None
    assert extract_money("") is None


def test_extract_money_unit_must_stand_alone():
    # "kem" không phải đơn vị "k": "2" là số trần = 2 VNĐ, không phải 2.000
    assert extract_money("mua 2 kem") == 2


def test_extract_money_takes_first_amount_from_left():
    assert extract_money("phở 50k giảm 10k") == 50000


def test_split_then_extract_money_for_each_part():
    parts = split_sentences("phở 50k, grab 35k")

    assert [extract_money(part) for part in parts] == [50000, 35000]


def test_remove_money_keeps_the_rest_of_the_part():
    assert remove_money("phở 50k") == "phở"
    assert remove_money("mua 2 kem") == "mua kem"
    assert remove_money("ăn phở") == "ăn phở"


def test_match_category_unambiguous_keyword_has_full_confidence():
    category, confidence = match_category("phở")

    assert category == "Ăn uống"
    assert confidence == pytest.approx(1.0)


def test_match_category_ambiguous_keyword_lowers_confidence():
    # Trong seed, "grab" có ở 3 note Di chuyển và 1 note Lương -> confidence 3/4
    category, confidence = match_category("grab")

    assert category == "Di chuyển"
    assert confidence == pytest.approx(0.75)


def test_match_category_picks_category_with_highest_total_score():
    # uống: 1 điểm Ăn uống; cà, phê: mỗi từ 3/4 điểm Ăn uống -> (1 + 0.75 + 0.75) / 3 từ khớp
    category, confidence = match_category("uống cà phê")

    assert category == "Ăn uống"
    assert confidence == pytest.approx(2.5 / 3)


def test_match_category_unknown_words_returns_none():
    assert match_category("xyz abc") == (None, 0.0)
    assert match_category("") == (None, 0.0)


def test_parse_text_example_sentence_gives_two_items():
    parts = parse_text("phở 50k, grab 35k")

    assert len(parts) == 2
    assert (parts[0].amount, parts[0].category_name) == (50000, "Ăn uống")
    assert parts[0].confidence == pytest.approx(1.0)
    assert (parts[1].amount, parts[1].category_name) == (35000, "Di chuyển")
    assert parts[1].confidence == pytest.approx(0.75)


def test_parse_text_splits_three_transactions_in_one_sentence():
    parts = parse_text("ăn phở 50k và uống cà phê 30k rồi đổ xăng 100k")

    assert [(part.amount, part.category_name) for part in parts] == [
        (50000, "Ăn uống"),
        (30000, "Ăn uống"),
        (100000, "Di chuyển"),
    ]


def test_parse_text_part_without_money_needs_manual_entry():
    parts = parse_text("xin chào, phở 50k")

    assert parts[0] == (None, None, 0.0)
    assert (parts[1].amount, parts[1].category_name) == (50000, "Ăn uống")


def test_parse_text_unknown_words_keep_amount_but_no_category():
    assert parse_text("abc 50k") == [(50000, None, 0.0)]


def test_parse_text_empty_text_returns_empty_list():
    assert parse_text("") == []
    assert parse_text("   ") == []


class FakeCategoryPool:
    def __init__(self, rows):
        self._rows = rows
        self.fetch_args = None

    async def fetch(self, query, *args):
        assert "FROM categories" in query
        self.fetch_args = args
        return self._rows


def test_parse_quick_add_maps_category_name_to_whitelist_id():
    pool = FakeCategoryPool(
        [
            {"id": "sys_an_uong", "name": "Ăn uống"},
            {"id": "sys_di_chuyen", "name": "Di chuyển"},
        ]
    )

    items = asyncio.run(parse_quick_add(pool, "user-1", "phở 50k, grab 35k"))

    assert pool.fetch_args == ("user-1",)
    assert [(amount, category_id) for amount, category_id, _ in items] == [
        (50000, "sys_an_uong"),
        (35000, "sys_di_chuyen"),
    ]
    assert items[0][2] == pytest.approx(1.0)
    assert items[1][2] == pytest.approx(0.75)


def test_parse_quick_add_category_missing_from_whitelist_gives_none():
    # "Di chuyển" không còn trong whitelist của user (đã xoá) -> category_id None, amount vẫn giữ
    pool = FakeCategoryPool([{"id": "sys_an_uong", "name": "Ăn uống"}])

    items = asyncio.run(parse_quick_add(pool, "user-1", "phở 50k, grab 35k"))

    assert [(amount, category_id) for amount, category_id, _ in items] == [
        (50000, "sys_an_uong"),
        (35000, None),
    ]


def test_parse_quick_add_system_category_wins_over_same_name_user_category():
    # Query xếp category hệ thống (user_id NULL) lên trước; trùng tên thì lấy dòng đầu
    pool = FakeCategoryPool(
        [
            {"id": "sys_an_uong", "name": "Ăn uống"},
            {"id": "user-cat-1", "name": "Ăn uống"},
        ]
    )

    items = asyncio.run(parse_quick_add(pool, "user-1", "phở 50k"))

    assert items[0][1] == "sys_an_uong"