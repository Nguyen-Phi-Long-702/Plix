import csv
from typing import List, Tuple
import pytest

from app.services.ai.naive_bayes import CSV_PATH, NaiveBayesClassifier
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


def _load_real_dataset() -> Tuple[List[str], List[str]]:
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        rows = list(csv.DictReader(csv_file))
    return [row["note"] for row in rows], [row["category"] for row in rows]


def _fitted_classifier_on_real_dataset() -> NaiveBayesClassifier:
    notes, labels = _load_real_dataset()
    classifier = NaiveBayesClassifier()
    classifier.fit(notes, labels)
    return classifier


def test_predict_on_real_dataset_matches_main_block_example_grab():
    classifier = _fitted_classifier_on_real_dataset()

    category, confidence = classifier.predict("grab")

    assert category == "Di chuyển"
    assert 0.0 < confidence <= 1.0


def test_predict_on_real_dataset_matches_main_block_example_grab_full_note():
    classifier = _fitted_classifier_on_real_dataset()

    category, confidence = classifier.predict("đi grab về nhà")

    assert category == "Di chuyển"
    assert 0.0 < confidence <= 1.0


def test_predict_on_real_dataset_matches_main_block_example_health_note():
    classifier = _fitted_classifier_on_real_dataset()

    category, confidence = classifier.predict("khám bệnh viện phí")

    assert category == "Sức khoẻ"
    assert 0.0 < confidence <= 1.0


def test_predict_on_real_dataset_matches_main_block_example_salary_note():
    classifier = _fitted_classifier_on_real_dataset()

    category, confidence = classifier.predict("lương tháng này")

    assert category == "Lương"
    assert 0.0 < confidence <= 1.0


def test_predict_on_real_dataset_matches_main_block_example_ambiguous_note():
    classifier = _fitted_classifier_on_real_dataset()

    category, confidence = classifier.predict("mua đồ gì đó lạ chưa từng thấy")

    assert category == "Mua sắm"
    assert 0.0 < confidence <= 1.0


def test_predict_self_consistency_accuracy_on_full_dataset():
    notes, labels = _load_real_dataset()
    classifier = NaiveBayesClassifier()
    classifier.fit(notes, labels)

    correct = sum(1 for note, label in zip(notes, labels) if classifier.predict(note)[0] == label)
    accuracy = correct / len(notes)

    assert accuracy >= 0.90


def test_predict_generalizes_to_held_out_notes_per_category():
    notes, labels = _load_real_dataset()
    notes_by_category = {}
    for note, category in zip(notes, labels):
        notes_by_category.setdefault(category, []).append(note)

    train_notes, train_labels, test_notes, test_labels = [], [], [], []
    for category, category_notes in notes_by_category.items():
        train_size = len(category_notes) - 10
        train_notes += category_notes[:train_size]
        train_labels += [category] * train_size
        test_notes += category_notes[train_size:]
        test_labels += [category] * 10

    classifier = NaiveBayesClassifier()
    classifier.fit(train_notes, train_labels)

    correct = sum(
        1 for note, label in zip(test_notes, test_labels)
        if classifier.predict(note)[0] == label
    )
    accuracy = correct / len(test_notes)

    assert accuracy >= 0.60


def test_predict_on_fully_unknown_note_returns_uniform_confidence():
    notes, labels = _load_real_dataset()
    classifier = NaiveBayesClassifier()
    classifier.fit(notes, labels)

    category, confidence = classifier.predict("qwertyzxcvbnm123456")

    assert category in set(labels)
    assert confidence == pytest.approx(1 / len(set(labels)), abs=1e-9)