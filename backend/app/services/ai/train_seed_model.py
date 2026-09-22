import asyncio
import csv

import asyncpg

from app.core.config import DATABASE_URL
from app.services.ai.model_store import CSV_PATH, save_model_params, load_model_params
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.tfidf import TfidfVectorizer

SEED_USER_ID = "__seed__"

REGRESSION_SAMPLES = [
    ("cà phê", "Ăn uống"),
    ("đổ xăng", "Di chuyển"),
    ("tiền điện", "Hoá đơn (điện/nước/internet)"),
    ("lương tháng", "Lương"),
]


async def train_and_save_seed_model() -> None:
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        rows = list(csv.DictReader(csv_file))
    notes = [row["note"] for row in rows]
    labels = [row["category"] for row in rows]
    print(f"Doc {len(notes)} dong tu {CSV_PATH}")

    vectorizer = TfidfVectorizer()
    vectorizer.fit(notes)
    classifier = NaiveBayesClassifier()
    classifier.fit(notes, labels)

    pool = await asyncpg.create_pool(dsn=DATABASE_URL, ssl="require", min_size=1, max_size=1)
    try:
        await save_model_params(pool, SEED_USER_ID, vectorizer, classifier, training_sample_count=0)
        loaded = await load_model_params(pool, SEED_USER_ID)
    finally:
        await pool.close()

    assert loaded is not None, "BUG: vua save xong nhung load lai tra ve None"

    print("\nDa luu model seed vao ai_model_params (user_id='__seed__')")
    print("trained_at:", loaded.trained_at)
    print("training_sample_count:", loaded.training_sample_count)

    print("\n--- Regression nhanh ---")
    all_ok = True
    for note, expected_category in REGRESSION_SAMPLES:
        predicted_category, confidence = loaded.classifier.predict(note)
        ok = predicted_category == expected_category
        all_ok = all_ok and ok
        print(f"{note!r} -> predicted={predicted_category!r} (confidence={confidence:.4f}), "
              f"expected={expected_category!r}, {'OK' if ok else 'SAI'}")

    print("\nKET QUA REGRESSION:", "PASS" if all_ok else "FAIL - kiem tra lai truoc khi Exit Sprint 1")


if __name__ == "__main__":
    asyncio.run(train_and_save_seed_model())