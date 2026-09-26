import asyncio
import json
from types import SimpleNamespace

from app.routers import retrain
from app.services.ai import model_store

USER_A = "user-a"
USER_B = "user-b"

DATASETS = {
    USER_A: {
        "notes": ["cà phê sáng nay", "trà sữa trân châu", "xe buýt đi học"],
        "labels": ["Ăn uống", "Ăn uống", "Di chuyển"],
        "correction_count": 3,
    },
    USER_B: {
        "notes": ["xem phim rạp", "mua vé concert", "nạp tiền điện thoại"],
        "labels": ["Giải trí", "Giải trí", "Hoá đơn"],
        "correction_count": 5,
    },
}


class FakeUserIsolationPool:
    def __init__(self, datasets: dict):
        self._datasets = datasets
        self._rows = {}
        self.saved = {}

    def _row(self, user_id: str) -> dict:
        return self._rows.setdefault(user_id, {"is_training": False, "trained_at": None})

    async def fetchrow(self, query, *args):
        user_id = args[0]
        assert user_id in self._datasets, f"user_id lạ không có trong test data: {user_id}"
        row = self._row(user_id)

        if "EXTRACT(EPOCH" in query:
            return None

        if "INSERT INTO ai_model_params (user_id, is_training)" in query:
            if row["is_training"]:
                return None
            row["is_training"] = True
            return {"user_id": user_id}

        if "SELECT idf, class_priors, likelihoods, trained_at" in query:
            saved = self.saved.get(user_id)
            assert saved is not None, f"chưa có model nào được lưu cho {user_id}"
            return {
                "idf": json.dumps(saved["idf"]),
                "class_priors": json.dumps(saved["class_priors"]),
                "likelihoods": json.dumps(saved["likelihoods"]),
                "trained_at": row["trained_at"],
                "training_sample_count": saved["training_sample_count"],
                "is_training": row["is_training"],
            }

        raise AssertionError(f"fetchrow không mong đợi trong test: {query}")

    async def fetch(self, query, *args):
        user_id = args[0]
        assert user_id in self._datasets, f"user_id lạ không có trong test data: {user_id}"
        await asyncio.sleep(0)
        data = self._datasets[user_id]
        return [
            {"note": note, "category_name": label}
            for note, label in zip(data["notes"], data["labels"])
        ]

    async def fetchval(self, query, *args):
        user_id = args[0]
        assert user_id in self._datasets, f"user_id lạ không có trong test data: {user_id}"
        return self._datasets[user_id]["correction_count"]

    async def execute(self, query, *args):
        user_id = args[0]
        row = self._row(user_id)
        if "UPDATE ai_model_params SET is_training = false" in query:
            row["is_training"] = False
        elif "INSERT INTO ai_model_params" in query and "vocabulary" in query:
            row["is_training"] = False
            row["trained_at"] = "2026-09-29T10:00:00+00:00"
            self.saved[user_id] = {
                "idf": json.loads(args[2]),
                "class_priors": json.loads(args[3]),
                "likelihoods": json.loads(args[4]),
                "training_sample_count": args[5],
            }

def _call_retrain(pool: FakeUserIsolationPool, user_id: str):
    fake_request = SimpleNamespace(app=SimpleNamespace(state=SimpleNamespace(db_pool=pool)))
    return asyncio.run(retrain.retrain(fake_request, user_id=user_id))

def _load_model(pool: FakeUserIsolationPool, user_id: str):
    return asyncio.run(model_store.load_model_params(pool, user_id))

def test_two_users_retrain_produce_fully_independent_models():
    pool = FakeUserIsolationPool(DATASETS)

    response_a = _call_retrain(pool, USER_A)
    response_b = _call_retrain(pool, USER_B)

    assert response_a.training_sample_count == 3
    assert response_b.training_sample_count == 5

    loaded_a = _load_model(pool, USER_A)
    loaded_b = _load_model(pool, USER_B)

    categories_a = set(loaded_a.classifier._log_prior.keys())
    categories_b = set(loaded_b.classifier._log_prior.keys())
    assert categories_a == {"Ăn uống", "Di chuyển"}
    assert categories_b == {"Giải trí", "Hoá đơn"}
    assert categories_a.isdisjoint(categories_b), "model A không được lẫn category của B"

    vocabulary_a = loaded_a.vectorizer._vocabulary
    vocabulary_b = loaded_b.vectorizer._vocabulary
    assert vocabulary_a.isdisjoint(vocabulary_b), "từ vựng 2 model không được lẫn nhau"

    predicted_a, _ = loaded_a.classifier.predict("xem phim rạp mua vé concert")
    predicted_b, _ = loaded_b.classifier.predict("cà phê sáng nay trà sữa trân châu")
    assert predicted_a in categories_a, "model A chỉ được đoán ra category của chính A"
    assert predicted_b in categories_b, "model B chỉ được đoán ra category của chính B"

def test_retraining_second_user_does_not_change_first_users_saved_model():
    pool = FakeUserIsolationPool(DATASETS)

    _call_retrain(pool, USER_A)
    loaded_a_before = _load_model(pool, USER_A)
    sample_count_before = loaded_a_before.training_sample_count
    categories_before = dict(loaded_a_before.classifier._log_prior)

    _call_retrain(pool, USER_B)
    loaded_a_after = _load_model(pool, USER_A)

    assert loaded_a_after.training_sample_count == sample_count_before
    assert loaded_a_after.classifier._log_prior == categories_before
    assert set(loaded_a_after.classifier._log_prior.keys()) == {"Ăn uống", "Di chuyển"}