import math

import pytest

from app.services.ai.tfidf import TfidfVectorizer
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