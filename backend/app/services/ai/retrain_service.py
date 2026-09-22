import asyncio
import time
import asyncpg

from app.core.config import DATABASE_URL
from app.services.ai.model_store import load_model_params, save_model_params
from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.tfidf import TfidfVectorizer


class TrainingInProgressError(Exception):
    """Raise khi user_id này đang có 1 lượt retrain khác chạy dở
    (is_training=true) — endpoint /retrain (Ngày 22) sẽ bắt exception này
    và trả 409 Conflict, KHÔNG tự ý retry hay chờ."""


async def _try_acquire_training_lock(pool: asyncpg.Pool, user_id: str) -> bool:
    row = await pool.fetchrow(
        """
        INSERT INTO ai_model_params (user_id, is_training)
        VALUES ($1, true)
        ON CONFLICT (user_id) DO UPDATE
            SET is_training = true
            WHERE ai_model_params.is_training = false
        RETURNING user_id
        """,
        user_id,
    )
    return row is not None


async def _release_training_lock(pool: asyncpg.Pool, user_id: str) -> None:
    await pool.execute(
        "UPDATE ai_model_params SET is_training = false WHERE user_id = $1",
        user_id,
    )


async def retrain_user_model(pool: asyncpg.Pool, user_id: str) -> None:
    if not await _try_acquire_training_lock(pool, user_id):
        raise TrainingInProgressError(user_id)

    try:
        rows = await pool.fetch(
            """
            SELECT t.note AS note, c.name AS category_name
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = $1
              AND t.is_deleted = false
              AND c.is_deleted = false
            """,
            user_id,
        )
        notes = [row["note"] or "" for row in rows]
        labels = [row["category_name"] for row in rows]

        vectorizer = TfidfVectorizer()
        vectorizer.fit(notes)
        classifier = NaiveBayesClassifier()
        classifier.fit(notes, labels)

        training_sample_count = await pool.fetchval(
            """
            SELECT COUNT(*) FROM corrections
            WHERE user_id = $1 AND is_deleted = false
            """,
            user_id,
        )

        await save_model_params(pool, user_id, vectorizer, classifier, training_sample_count)
    except Exception:
        await _release_training_lock(pool, user_id)
        raise


async def _manual_test() -> None:
    pool = await asyncpg.create_pool(dsn=DATABASE_URL, ssl="require", min_size=1, max_size=1)
    test_user_id = "__ngay13_test_retrain__"
    category_a_id, category_b_id = "__ngay13_cat_a__", "__ngay13_cat_b__"
    tx_ids = ["__ngay13_tx_1__", "__ngay13_tx_2__", "__ngay13_tx_3__", "__ngay13_tx_4__"]
    correction_ids = ["__ngay13_corr_1__", "__ngay13_corr_2__"]
    now_ms = int(time.time() * 1000)

    try:
        for category_id, name in [
            (category_a_id, "Test Ngày13 A"),
            (category_b_id, "Test Ngày13 B"),
        ]:
            await pool.execute(
                "INSERT INTO categories (id, user_id, name, updated_at) VALUES ($1, $2, $3, $4)",
                category_id, test_user_id, name, now_ms,
            )

        for tx_id, category_id, note in [
            (tx_ids[0], category_a_id, "note mẫu A1"),
            (tx_ids[1], category_a_id, "note mẫu A2"),
            (tx_ids[2], category_b_id, "note mẫu B1"),
            (tx_ids[3], category_b_id, "note mẫu B2"),
        ]:
            await pool.execute(
                """
                INSERT INTO transactions (id, user_id, amount, category_id, note, occurred_at, updated_at)
                VALUES ($1, $2, 10000, $3, $4, $5, $5)
                """,
                tx_id, test_user_id, category_id, note, now_ms,
            )

        for correction_id, tx_id in zip(correction_ids, tx_ids[:2]):
            await pool.execute(
                """
                INSERT INTO corrections
                    (id, user_id, transaction_id, predicted_category_id,
                     corrected_category_id, created_at, updated_at)
                VALUES ($1, $2, $3, $4, $4, $5, $5)
                """,
                correction_id, test_user_id, tx_id, category_b_id, now_ms,
            )

        # Case 1: retrain bình thường (chưa bị khoá)
        await retrain_user_model(pool, test_user_id)
        loaded = await load_model_params(pool, test_user_id)
        assert loaded is not None
        assert loaded.training_sample_count == 2, (
            f"Kỳ vọng 2 correction, thực tế {loaded.training_sample_count}"
        )
        assert loaded.is_training is False
        predicted_category, confidence = loaded.classifier.predict("note mẫu A1")
        print(
            "Case 1 (retrain bình thường) OK — training_sample_count=2 (không phải 4);",
            "predict('note mẫu A1') ->", predicted_category, f"(confidence={confidence:.4f})",
        )

        # Case 2: giả lập đang có 1 lượt retrain khác chạy dở
        await pool.execute(
            "UPDATE ai_model_params SET is_training = true WHERE user_id = $1",
            test_user_id,
        )
        try:
            await retrain_user_model(pool, test_user_id)
            print("Case 2 FAIL: đáng lẽ phải raise TrainingInProgressError")
        except TrainingInProgressError:
            print("Case 2 OK — raise TrainingInProgressError đúng như kỳ vọng khi đang bị khoá")

    finally:
        # Dọn sạch dữ liệu giả, không để lại rác trong Postgres thật dùng chung
        await pool.execute("DELETE FROM corrections WHERE user_id = $1", test_user_id)
        await pool.execute("DELETE FROM transactions WHERE user_id = $1", test_user_id)
        await pool.execute("DELETE FROM categories WHERE user_id = $1", test_user_id)
        await pool.execute("DELETE FROM ai_model_params WHERE user_id = $1", test_user_id)
        await pool.close()


if __name__ == "__main__":
    asyncio.run(_manual_test())