import math
import csv
import pytest

from app.services.ai.tfidf import CSV_PATH, TfidfVectorizer
FIXED_DOCS = ["mua cà phê", "mua trà sữa", "uống cà phê sáng"]


def _fitted_vectorizer() -> TfidfVectorizer:
    vectorizer = TfidfVectorizer()
    vectorizer.fit(FIXED_DOCS)
    return vectorizer


def test_transform_matches_hand_computed_tfidf():
    vectorizer = _fitted_vectorizer()

    result = vectorizer.transform("mua cà phê")

    idf_shared = math.log(3 / 2)
    expected = {
        "mua": (1 / 3) * idf_shared,
        "cà": (1 / 3) * idf_shared,
        "phê": (1 / 3) * idf_shared,
    }
    assert result == pytest.approx(expected, abs=1e-9)


def test_transform_weighs_repeated_words_higher():
    vectorizer = _fitted_vectorizer()

    result = vectorizer.transform("cà cà phê")

    idf_shared = math.log(3 / 2)
    expected = {
        "cà": (2 / 3) * idf_shared,
        "phê": (1 / 3) * idf_shared,
    }
    assert result == pytest.approx(expected, abs=1e-9)


def test_transform_ignores_out_of_vocabulary_words():
    vectorizer = _fitted_vectorizer()

    result = vectorizer.transform("uống nước suối")

    expected = {"uống": (1 / 3) * math.log(3)}
    assert result == pytest.approx(expected, abs=1e-9)


def test_transform_empty_text_returns_empty_vector():
    vectorizer = _fitted_vectorizer()

    assert vectorizer.transform("") == {}


def test_fit_raises_on_empty_documents():
    vectorizer = TfidfVectorizer()

    with pytest.raises(ValueError):
        vectorizer.fit([])

def _fitted_vectorizer_on_real_dataset() -> TfidfVectorizer:
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        notes = [row["note"] for row in csv.DictReader(csv_file)]
    vectorizer = TfidfVectorizer()
    vectorizer.fit(notes)
    return vectorizer


def test_transform_on_real_dataset_matches_main_block_example_grab_note():
    vectorizer = _fitted_vectorizer_on_real_dataset()

    result = vectorizer.transform("đi grab về nhà")

    assert set(result.keys()) == {"đi", "grab", "nhà"}
    assert all(weight > 0 for weight in result.values())


def test_transform_on_real_dataset_matches_main_block_example_health_note():
    vectorizer = _fitted_vectorizer_on_real_dataset()

    result = vectorizer.transform("khám bệnh viện phí")

    assert set(result.keys()) == {"khám", "bệnh", "viện", "phí"}
    assert all(weight > 0 for weight in result.values())


def test_transform_on_real_dataset_matches_main_block_example_salary_note():
    vectorizer = _fitted_vectorizer_on_real_dataset()

    result = vectorizer.transform("lương tháng này")

    assert set(result.keys()) == {"lương", "tháng", "này"}
    assert all(weight > 0 for weight in result.values())


def test_transform_on_real_dataset_skips_unknown_words_in_note():
    vectorizer = _fitted_vectorizer_on_real_dataset()

    result = vectorizer.transform("mua đồ gì đó lạ chưa từng thấy")

    assert set(result.keys()) == {"mua", "đồ"}
    assert all(weight > 0 for weight in result.values())


def test_transform_on_real_dataset_returns_empty_vector_for_unknown_note():
    vectorizer = _fitted_vectorizer_on_real_dataset()

    assert vectorizer.transform("qwertyzxcvbnm123456") == {}


def test_fit_on_real_dataset_is_deterministic():
    vectorizer_a = _fitted_vectorizer_on_real_dataset()
    vectorizer_b = _fitted_vectorizer_on_real_dataset()

    assert vectorizer_a.transform("đi grab về nhà") == vectorizer_b.transform("đi grab về nhà")