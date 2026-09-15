import pytest

from app.services.ai.naive_bayes import NaiveBayesClassifier
FIXED_DOCS = ["phở bò", "cơm gà", "xe buýt", "tàu hoả"]
FIXED_LABELS = ["Ăn uống", "Ăn uống", "Di chuyển", "Di chuyển"]


def _fitted_classifier() -> NaiveBayesClassifier:
    classifier = NaiveBayesClassifier()
    classifier.fit(FIXED_DOCS, FIXED_LABELS)
    return classifier


def test_predict_matches_hand_computed_category_and_confidence():
    classifier = _fitted_classifier()

    category, confidence = classifier.predict("phở bò")

    assert category == "Ăn uống"
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_predict_other_category_by_symmetry():
    classifier = _fitted_classifier()

    category, confidence = classifier.predict("xe buýt")

    assert category == "Di chuyển"
    assert confidence == pytest.approx(0.8, abs=1e-9)


def test_predict_ignores_out_of_vocabulary_words():
    classifier = _fitted_classifier()

    category, confidence = classifier.predict("phở gì đó")

    assert category == "Ăn uống"
    assert confidence == pytest.approx(2 / 3, abs=1e-9)


def test_fit_raises_on_empty_documents():
    classifier = NaiveBayesClassifier()

    with pytest.raises(ValueError):
        classifier.fit([], [])


def test_fit_raises_on_mismatched_lengths():
    classifier = NaiveBayesClassifier()

    with pytest.raises(ValueError):
        classifier.fit(["a", "b"], ["x"])