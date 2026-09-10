"""
- TF(từ, note) = số lần từ xuất hiện trong note / tổng số từ trong note
- IDF(từ) = log(tổng số note trong tập training / số note chứa từ đó)
- TF-IDF(từ, note) = TF(từ, note) * IDF(từ)
"""
import csv
import math
import re
from pathlib import Path
from typing import Dict, List, Set

CSV_PATH = Path(__file__).resolve().parents[3] / "data" / "seed_categorization.csv"


def tokenize(text: str) -> List[str]:
    """Tiền xử lý: lowercase, bỏ dấu câu, tách từ đơn giản theo khoảng trắng."""
    text = text.lower()
    text = re.sub(r"[^\w\s]", " ", text, flags=re.UNICODE)
    return [token for token in text.split() if token]


class TfidfVectorizer:
    """Vector hoá note thành dict {từ: trọng số TF-IDF}, theo vocabulary
    xây dựng từ tập training truyền vào fit()."""

    def __init__(self):
        self._idf: Dict[str, float] = {}
        self._vocabulary: Set[str] = set()

    def fit(self, documents: List[str]) -> None:
        """Xây vocabulary + tính idf cho từng từ dựa trên tập documents
        (mỗi document là 1 note). Gọi lại fit() sẽ ghi đè state cũ."""
        total_docs = len(documents)
        if total_docs == 0:
            raise ValueError("documents rỗng — không thể fit TF-IDF")

        document_frequency: Dict[str, int] = {}
        for document in documents:
            tokens_in_document = set(tokenize(document))
            for token in tokens_in_document:
                document_frequency[token] = document_frequency.get(token, 0) + 1

        self._vocabulary = set(document_frequency.keys())
        self._idf = {
            token: math.log(total_docs / df)
            for token, df in document_frequency.items()
        }

    def transform(self, text: str) -> Dict[str, float]:
        """Vector hoá 1 note thành dict {từ: trọng số TF-IDF}. Từ không có
        trong vocabulary (chưa gặp lúc fit) bị bỏ qua."""
        tokens = tokenize(text)
        if not tokens:
            return {}

        total_tokens = len(tokens)
        term_frequency: Dict[str, int] = {}
        for token in tokens:
            term_frequency[token] = term_frequency.get(token, 0) + 1

        vector: Dict[str, float] = {}
        for token, count in term_frequency.items():
            if token not in self._vocabulary:
                continue
            tf = count / total_tokens
            vector[token] = tf * self._idf[token]
        return vector


if __name__ == "__main__":
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        training_notes = [row["note"] for row in csv.DictReader(csv_file)]

    vectorizer = TfidfVectorizer()
    vectorizer.fit(training_notes)

    example_notes = [
        "đi grab về nhà",
        "khám bệnh viện phí",
        "lương tháng này",
        "mua đồ gì đó lạ chưa từng thấy",
    ]
    for note in example_notes:
        print(note, "->", vectorizer.transform(note))