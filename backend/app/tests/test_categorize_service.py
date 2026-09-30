import asyncio
import json

import pytest

from app.services.ai.categorize_service import (
    MIN_TRAINING_SAMPLES_FOR_OWN_MODEL,
    NoModelAvailableError,
    classify_note,
)
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.tfidf import TfidfVectorizer
from app.services.ai.train_seed_model import SEED_USER_ID

FIXED_NOTES = ["phở bò", "cơm gà", "xe buýt", "tàu hoả"]
FIXED_LABELS = ["Ăn uống", "Ăn uống", "Di chuyển", "Di chuyển"]

SEED_NOTES = ["đổ xăng xe máy", "sửa xe máy", "vé xe buýt", "cà phê sáng"]
SEED_LABELS = ["Di chuyển", "Di chuyển", "Di chuyển", "Ăn uống"]


def _fitted_model_row(training_sample_count, notes=None, labels=None):
    notes = FIXED_NOTES if notes is None else notes
    labels = FIXED_LABELS if labels is None else labels
    vectorizer = TfidfVectorizer()
    vectorizer.fit(notes)
    classifier = NaiveBayesClassifier()
    classifier.fit(notes, labels)
    return {
        "idf": json.dumps(vectorizer._idf),
        "class_priors": json.dumps(classifier._log_prior),
        "likelihoods": json.dumps(classifier._log_likelihood),
        "trained_at": None,
        "training_sample_count": training_sample_count,
        "is_training": False,
    }


class FakeCategorizePool:

    def __init__(self, model_rows, category_rows):
        self._model_rows = model_rows
        self._category_rows = category_rows

    async def fetchrow(self, query, *args):
        if "ai_model_params" in query:
            (user_id,) = args
            return self._model_rows.get(user_id)
        if "FROM categories" in query:
            user_id, name = args
            return self._category_rows.get((user_id, name))
        raise AssertionError(f"Query không mong đợi trong test: {query}")


def test_uses_own_model_when_training_sample_count_at_least_10():
    pool = FakeCategorizePool(
        model_rows={
            "user-1": _fitted_model_row(
                training_sample_count=MIN_TRAINING_SAMPLES_FOR_OWN_MODEL
            )
        },
        category_rows={("user-1", "Ăn uống"): {"id": "cat-an-uong"}},
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-1", "phở bò"))

    assert category_id == "cat-an-uong"
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_falls_back_to_seed_model_when_user_has_no_model_yet():
    pool = FakeCategorizePool(
        model_rows={SEED_USER_ID: _fitted_model_row(training_sample_count=0)},
        category_rows={("user-2", "Ăn uống"): {"id": "cat-an-uong-2"}},
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-2", "cơm gà"))

    assert category_id == "cat-an-uong-2"
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_falls_back_to_seed_model_when_own_model_has_fewer_than_10_samples():
    pool = FakeCategorizePool(
        model_rows={
            "user-5": _fitted_model_row(
                training_sample_count=MIN_TRAINING_SAMPLES_FOR_OWN_MODEL - 1
            ),
            SEED_USER_ID: _fitted_model_row(
                training_sample_count=0, notes=SEED_NOTES, labels=SEED_LABELS
            ),
        },
        category_rows={("user-5", "Di chuyển"): {"id": "cat-di-chuyen-seed"}},
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-5", "phở bò"))

    assert category_id == "cat-di-chuyen-seed"
    assert confidence == pytest.approx(0.75, abs=1e-9)


def test_uses_own_model_exactly_at_threshold_10_samples():
    pool = FakeCategorizePool(
        model_rows={
            "user-6": _fitted_model_row(
                training_sample_count=MIN_TRAINING_SAMPLES_FOR_OWN_MODEL
            ),
            SEED_USER_ID: _fitted_model_row(
                training_sample_count=0, notes=SEED_NOTES, labels=SEED_LABELS
            ),
        },
        category_rows={("user-6", "Ăn uống"): {"id": "cat-an-uong-own"}},
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-6", "phở bò"))

    assert category_id == "cat-an-uong-own"
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_returns_none_category_id_when_predicted_name_not_in_whitelist():
    pool = FakeCategorizePool(
        model_rows={
            "user-3": _fitted_model_row(
                training_sample_count=MIN_TRAINING_SAMPLES_FOR_OWN_MODEL
            )
        },
        category_rows={},  #không có category hợp lệ nào khớp tên dự đoán
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-3", "phở bò"))

    assert category_id is None
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_raises_when_neither_user_model_nor_seed_model_exists():
    pool = FakeCategorizePool(model_rows={}, category_rows={})

    with pytest.raises(NoModelAvailableError):
        asyncio.run(classify_note(pool, "user-4", "phở bò"))


def test_threshold_is_10_per_project_plan():
    assert MIN_TRAINING_SAMPLES_FOR_OWN_MODEL == 10


def test_falls_back_to_seed_model_when_own_model_row_is_mid_retrain():
    pool = FakeCategorizePool(
        model_rows={
            "user-7": {
                "idf": "{}",
                "class_priors": "{}",
                "likelihoods": "{}",
                "trained_at": None,
                "training_sample_count": 0,
                "is_training": True,
            },
            SEED_USER_ID: _fitted_model_row(training_sample_count=0),
        },
        category_rows={("user-7", "Ăn uống"): {"id": "cat-an-uong-7"}},
    )

    category_id, confidence = asyncio.run(classify_note(pool, "user-7", "phở bò"))

    assert category_id == "cat-an-uong-7"
    assert confidence == pytest.approx(0.8, abs=1e-9)