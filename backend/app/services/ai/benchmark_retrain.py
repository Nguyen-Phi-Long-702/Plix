import asyncio
import csv
import random
import statistics
import time

import asyncpg

from app.core.config import DATABASE_URL
from app.services.ai.model_store import CSV_PATH, load_model_params
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.retrain_service import retrain_user_model
from app.services.ai.tfidf import TfidfVectorizer

TEST_USER_ID = "__ngay26_benchmark_retrain__"
NUM_TRANSACTIONS = 5000
TARGET_SECONDS = 3.0


async def _cleanup(pool: asyncpg.Pool) -> None:
    await pool.execute("DELETE FROM transactions WHERE user_id = $1", TEST_USER_ID)
    await pool.execute("DELETE FROM categories WHERE user_id = $1", TEST_USER_ID)
    await pool.execute("DELETE FROM ai_model_params WHERE user_id = $1", TEST_USER_ID)


async def main() -> None:
    with open(CSV_PATH, encoding="utf-8") as csv_file:
        seed_rows = list(csv.DictReader(csv_file))
    sampled_rows = random.Random(26).choices(seed_rows, k=NUM_TRANSACTIONS)
    notes = [row["note"] for row in sampled_rows]
    labels = [row["category"] for row in sampled_rows]

    pool = await asyncpg.create_pool(dsn=DATABASE_URL, ssl="require", min_size=1, max_size=1)
    try:
        rtt_samples = []
        for _ in range(5):
            started = time.perf_counter()
            await pool.fetchval("SELECT 1")
            rtt_samples.append(time.perf_counter() - started)
        rtt = statistics.median(rtt_samples)

        started = time.perf_counter()
        TfidfVectorizer().fit(notes)
        NaiveBayesClassifier().fit(notes, labels)
        cpu_fit_seconds = time.perf_counter() - started

        await _cleanup(pool)
        now_ms = int(time.time() * 1000)
        category_id_by_name = {}
        for index, name in enumerate(sorted(set(labels))):
            category_id = f"__ngay26_cat_{index}__"
            category_id_by_name[name] = category_id
            await pool.execute(
                "INSERT INTO categories (id, user_id, name, updated_at) VALUES ($1, $2, $3, $4)",
                category_id, TEST_USER_ID, name, now_ms,
            )
        await pool.executemany(
            """
            INSERT INTO transactions (id, user_id, amount, category_id, note, occurred_at, updated_at)
            VALUES ($1, $2, 10000, $3, $4, $5, $5)
            """,
            [
                (f"__ngay26_tx_{i}__", TEST_USER_ID, category_id_by_name[label], note, now_ms)
                for i, (note, label) in enumerate(zip(notes, labels))
            ],
        )
        print(f"Da tao {NUM_TRANSACTIONS} giao dich gia cho user {TEST_USER_ID}")

        started = time.perf_counter()
        await retrain_user_model(pool, TEST_USER_ID)
        retrain_seconds = time.perf_counter() - started

        started = time.perf_counter()
        loaded = await load_model_params(pool, TEST_USER_ID)
        reload_seconds = time.perf_counter() - started
        assert loaded is not None, "BUG: retrain xong nhung load model tra ve None"
    finally:
        await _cleanup(pool)
        await pool.close()

    endpoint_seconds = rtt + retrain_seconds + reload_seconds
    print("\n--- KET QUA DO (giay) ---")
    print(f"1 lan truy van toi Postgres (SELECT 1, trung vi 5 lan): {rtt:.3f}")
    print(f"Chi tinh toan fit TF-IDF + Naive Bayes (trong bo nho):  {cpu_fit_seconds:.3f}")
    print(f"retrain_user_model (khoa + doc + fit + luu):            {retrain_seconds:.3f}")
    print(f"load_model_params sau retrain (router /retrain goi):    {reload_seconds:.3f}")
    print(f"Uoc tinh 1 lan goi /retrain (1 truy van cooldown + 2 muc ben tren): {endpoint_seconds:.3f}")
    print(f"Muc tieu tham chieu: < {TARGET_SECONDS:.1f} giay ->", "DAT" if endpoint_seconds < TARGET_SECONDS else "CHAM")


if __name__ == "__main__":
    asyncio.run(main())