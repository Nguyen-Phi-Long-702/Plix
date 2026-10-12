from app.services.ai.nl_parser import split_sentences


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