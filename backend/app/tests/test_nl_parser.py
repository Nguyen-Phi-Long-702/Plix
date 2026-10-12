import pytest

from app.services.ai.nl_parser import extract_money, split_sentences


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