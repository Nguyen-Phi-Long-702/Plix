"""
- log-prior(category) = log(số note thuộc category / tổng số note)
- log-likelihood(từ, category) = log((số lần từ xuất hiện trong các note thuộc
  category + 1) / (tổng số từ trong các note thuộc category + kích thước vocabulary))
  — Laplace smoothing +1 để tránh xác suất 0 với từ chưa gặp trong category đó.
- predict(note) = category có log-prior + Σlog-likelihood(từ) lớn nhất.
- confidence = softmax của các điểm log-prior+log-likelihood theo từng category
  — là normalized score/heuristic, KHÔNG phải xác suất đã hiệu chỉnh.
"""
import csv
import math
from pathlib import Path
from typing import Dict, List, Set, Tuple

from app.services.ai.tfidf import tokenize

CSV_PATH = Path(__file__).resolve().parents[3] / "data" / "seed_categorization.csv"


class NaiveBayesClassifier:
    """Phân loại note thành category, theo vocabulary xây từ tập training
    truyền vào fit(). Từ không có trong vocabulary (chưa gặp lúc fit) bị bỏ
    qua khi predict — cùng quy ước với TfidfVectorizer."""

    def __init__(self):
        self._log_prior: Dict[str, float] = {}
        self._log_likelihood: Dict[str, Dict[str, float]] = {}
        self._vocabulary: Set[str] = set()

    def fit(self, documents: List[str], labels: List[str]) -> None:
        """Tính log-prior + log-likelihood (có Laplace smoothing) cho từng
        category, dựa trên tập documents (mỗi document là 1 note) và labels
        (category tương ứng, cùng độ dài với documents). Gọi lại fit() sẽ
        ghi đè state cũ."""
        total_docs = len(documents)
        if total_docs == 0:
            raise ValueError("documents rỗng — không thể fit Naive Bayes")
        if len(labels) != total_docs:
            raise ValueError("documents và labels phải cùng độ dài")

        tokens_by_doc = [tokenize(document) for document in documents]
        self._vocabulary = {token for tokens in tokens_by_doc for token in tokens}
        vocabulary_size = len(self._vocabulary)

        doc_count_by_category: Dict[str, int] = {}
        word_count_by_category: Dict[str, Dict[str, int]] = {}
        for tokens, category in zip(tokens_by_doc, labels):
            doc_count_by_category[category] = doc_count_by_category.get(category, 0) + 1
            word_count = word_count_by_category.setdefault(category, {})
            for token in tokens:
                word_count[token] = word_count.get(token, 0) + 1

        self._log_prior = {
            category: math.log(count / total_docs)
            for category, count in doc_count_by_category.items()
        }

        self._log_likelihood = {}
        for category, word_count in word_count_by_category.items():
            total_words_in_category = sum(word_count.values())
            denominator = total_words_in_category + vocabulary_size
            self._log_likelihood[category] = {
                token: math.log((word_count.get(token, 0) + 1) / denominator)
                for token in self._vocabulary
            }

    def predict(self, text: str) -> Tuple[str, float]:
        """Trả về (category dự đoán, confidence). confidence = softmax của
        các điểm log-prior+log-likelihood — normalized score, không phải
        xác suất đã hiệu chỉnh."""
        tokens = [token for token in tokenize(text) if token in self._vocabulary]

        scores: Dict[str, float] = {}
        for category, log_prior in self._log_prior.items():
            score = log_prior
            log_likelihood = self._log_likelihood[category]
            for token in tokens:
                score += log_likelihood[token]
            scores[category] = score

        max_score = max(scores.values())
        exp_scores = {c: math.exp(s - max_score) for c, s in scores.items()}
        total_exp = sum(exp_scores.values())
        confidences = {c: exp_score / total_exp for c, exp_score in exp_scores.items()}

        predicted_category = max(scores, key=scores.get)
        return predicted_category, confidences[predicted_category]


if __name__ == "__main__":
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        rows = list(csv.DictReader(csv_file))
    training_notes = [row["note"] for row in rows]
    training_labels = [row["category"] for row in rows]

    classifier = NaiveBayesClassifier()
    classifier.fit(training_notes, training_labels)

    example_notes = [
        "grab",
        "đi grab về nhà",
        "khám bệnh viện phí",
        "lương tháng này",
        "mua đồ gì đó lạ chưa từng thấy",
    ]
    for note in example_notes:
        category, confidence = classifier.predict(note)
        print(note, "->", category, f"(confidence={confidence:.4f})")