import csv
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parents[2] / "data"

def _read_rows(file_name: str, encoding: str) -> list:
    with open(DATA_DIR / file_name, encoding=encoding, newline="") as csv_file:
        return list(csv.DictReader(csv_file))


def _seed_rows() -> list:
    return _read_rows("seed_categorization.csv", "utf-8")


def _holdout_rows() -> list:
    return _read_rows("test_categorization_holdout.csv", "utf-8-sig")


def _normalize(note: str) -> str:
    return note.strip().lower()


def test_seed_and_holdout_use_the_same_categories():
    seed_categories = {row["category"] for row in _seed_rows()}
    holdout_categories = {row["category"] for row in _holdout_rows()}
    assert seed_categories == holdout_categories


def test_seed_has_no_duplicate_notes():
    notes = [_normalize(row["note"]) for row in _seed_rows()]
    duplicates = sorted({note for note in notes if notes.count(note) > 1})
    assert not duplicates, f"note trùng trong seed: {duplicates}"


def test_seed_does_not_overlap_with_holdout():
    seed_notes = {_normalize(row["note"]) for row in _seed_rows()}
    holdout_notes = {_normalize(row["note"]) for row in _holdout_rows()}
    overlap = sorted(seed_notes & holdout_notes)
    assert not overlap, f"seed trùng tập kiểm tra độc lập: {overlap}"