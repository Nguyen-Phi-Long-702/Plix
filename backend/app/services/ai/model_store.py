"""
Helper load/save tham số model (TfidfVectorizer + NaiveBayesClassifier) vào
bảng ai_model_params (JSONB), theo user_id — dùng chung 1 code path cho
model thật của user và model seed dùng chung (sentinel user_id='__seed__').
"""
import asyncio
import csv
import json
from pathlib import Path
from typing import NamedTuple, Optional

import asyncpg

from app.core.config import DATABASE_URL
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.tfidf import TfidfVectorizer

CSV_PATH = Path(__file__).resolve().parents[3] / "data" / "seed_categorization.csv"


class ModelParams(NamedTuple):
    vectorizer: TfidfVectorizer
    classifier: NaiveBayesClassifier
    trained_at: object
    training_sample_count: int
    is_training: bool


async def save_model_params(
    pool: asyncpg.Pool,
    user_id: str,
    vectorizer: TfidfVectorizer,
    classifier: NaiveBayesClassifier,
    training_sample_count: int,
) -> None:
    """Ghi (insert lần đầu hoặc ghi đè nếu đã tồn tại) 1 hàng ai_model_params
    cho user_id. is_training luôn set false ở bước lưu này (advisory lock
    cho /retrain thêm ở Sprint 2 Ngày 13).

    GIẢ ĐỊNH BẮT BUỘC: vectorizer và classifier phải được fit() trên CÙNG
    một tập documents. Cột `vocabulary` được lưu suy ra từ `vectorizer._idf`
    và dùng lại cho cả classifier khi load — nếu 2 model được fit trên 2 tập
    dữ liệu khác nhau, vocabulary sẽ sai lệch cho classifier.
    """
    vocabulary = sorted(vectorizer._vocabulary)
    idf = vectorizer._idf
    class_priors = classifier._log_prior
    likelihoods = classifier._log_likelihood

    await pool.execute(
        """
        INSERT INTO ai_model_params
            (user_id, vocabulary, idf, class_priors, likelihoods,
             trained_at, training_sample_count, is_training)
        VALUES ($1, $2::jsonb, $3::jsonb, $4::jsonb, $5::jsonb, now(), $6, false)
        ON CONFLICT (user_id) DO UPDATE SET
            vocabulary = EXCLUDED.vocabulary,
            idf = EXCLUDED.idf,
            class_priors = EXCLUDED.class_priors,
            likelihoods = EXCLUDED.likelihoods,
            trained_at = EXCLUDED.trained_at,
            training_sample_count = EXCLUDED.training_sample_count,
            is_training = EXCLUDED.is_training
        """,
        user_id,
        json.dumps(vocabulary),
        json.dumps(idf),
        json.dumps(class_priors),
        json.dumps(likelihoods),
        training_sample_count,
    )


async def load_model_params(pool: asyncpg.Pool, user_id: str) -> Optional[ModelParams]:
    """Đọc 1 hàng ai_model_params theo user_id, dựng lại state để predict()
    hoạt động y hệt model vừa fit() trong tiến trình (Mục 7.1: model seed
    '__seed__' đi qua đúng code path load/predict/lưu). Trả None nếu
    user_id chưa có model (dùng để biết khi nào cần fallback về '__seed__')."""
    row = await pool.fetchrow(
        """
        SELECT idf, class_priors, likelihoods, trained_at,
               training_sample_count, is_training
        FROM ai_model_params
        WHERE user_id = $1
        """,
        user_id,
    )
    if row is None:
        return None

    idf = json.loads(row["idf"])
    class_priors = json.loads(row["class_priors"])
    likelihoods = json.loads(row["likelihoods"])
    vocabulary = set(idf.keys())

    vectorizer = TfidfVectorizer()
    vectorizer._idf = idf
    vectorizer._vocabulary = vocabulary

    classifier = NaiveBayesClassifier()
    classifier._log_prior = class_priors
    classifier._log_likelihood = likelihoods
    classifier._vocabulary = vocabulary

    return ModelParams(
        vectorizer=vectorizer,
        classifier=classifier,
        trained_at=row["trained_at"],
        training_sample_count=row["training_sample_count"],
        is_training=row["is_training"],
    )


async def _manual_test() -> None:
    pool = await asyncpg.create_pool(dsn=DATABASE_URL, ssl="require", min_size=1, max_size=1)
    try:
        # 1) Case chưa có model nào cho user này -> phải trả None
        #    (dùng 1 user_id giả không trùng '__seed__' để không đụng dữ liệu thật)
        never_saved = await load_model_params(pool, "__ngay9_test_chua_ton_tai__")
        print("load user chưa từng save ->", never_saved)
        assert never_saved is None, "BUG: phải trả None khi user_id chưa có hàng nào"

        # 2) Case save rồi load lại, so sánh predict() gốc vs. phục hồi
        with open(CSV_PATH, encoding="utf-8") as csv_file:
            rows = list(csv.DictReader(csv_file))
        notes = [row["note"] for row in rows]
        labels = [row["category"] for row in rows]

        vectorizer = TfidfVectorizer()
        vectorizer.fit(notes)
        classifier = NaiveBayesClassifier()
        classifier.fit(notes, labels)

        await save_model_params(pool, "__seed__", vectorizer, classifier, training_sample_count=0)
        loaded = await load_model_params(pool, "__seed__")
    finally:
        await pool.close()

    assert loaded is not None, "load trả về None ngay sau khi vừa save"

    example_notes = [
        "đi grab về nhà",
        "khám bệnh viện phí",
        "lương tháng này",
        "mua đồ gì đó lạ chưa từng thấy",
    ]
    all_match = True
    for note in example_notes:
        original = classifier.predict(note)
        restored = loaded.classifier.predict(note)
        match = original == restored
        all_match = all_match and match
        print(note, "-> original:", original, "restored:", restored, "match:", match)

    print("trained_at:", loaded.trained_at)
    print("training_sample_count:", loaded.training_sample_count)
    print("is_training:", loaded.is_training)
    print("\nALL MATCH:", all_match)


if __name__ == "__main__":
    asyncio.run(_manual_test())