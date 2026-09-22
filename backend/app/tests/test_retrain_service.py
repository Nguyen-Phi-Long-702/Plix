import asyncio
import json

from app.services.ai.naive_bayes import NaiveBayesClassifier
from app.services.ai.retrain_service import TrainingInProgressError, retrain_user_model

FIXED_NOTES = ["phở bò", "cơm gà", "xe buýt", "tàu hoả"]
FIXED_LABELS = ["Ăn uống", "Ăn uống", "Di chuyển", "Di chuyển"]


class FakeRetrainPool:
    def __init__(self, notes, labels, correction_count):
        self._notes = notes
        self._labels = labels
        self._correction_count = correction_count
        self.is_training = {}
        self.saved = {}

    async def fetchrow(self, query, user_id):
        if self.is_training.get(user_id, False):
            return None
        self.is_training[user_id] = True
        return {"user_id": user_id}

    async def fetch(self, query, user_id):
        await asyncio.sleep(0)
        return [
            {"note": note, "category_name": label}
            for note, label in zip(self._notes, self._labels)
        ]

    async def fetchval(self, query, user_id):
        return self._correction_count

    async def execute(self, query, *args):
        user_id = args[0]
        self.is_training[user_id] = False
        if "INSERT INTO ai_model_params" in query:
            self.saved[user_id] = {
                "idf": json.loads(args[2]),
                "class_priors": json.loads(args[3]),
                "likelihoods": json.loads(args[4]),
                "training_sample_count": args[5],
            }


def test_retrain_success_saves_correct_model_and_sample_count():
    pool = FakeRetrainPool(FIXED_NOTES, FIXED_LABELS, correction_count=2)

    asyncio.run(retrain_user_model(pool, "user-1"))

    saved = pool.saved["user-1"]
    assert saved["training_sample_count"] == 2  
    assert pool.is_training["user-1"] is False  

    restored_classifier = NaiveBayesClassifier()
    restored_classifier._log_prior = saved["class_priors"]
    restored_classifier._log_likelihood = saved["likelihoods"]
    restored_classifier._vocabulary = set(saved["idf"].keys())

    expected_classifier = NaiveBayesClassifier()
    expected_classifier.fit(FIXED_NOTES, FIXED_LABELS)

    assert restored_classifier.predict("phở bò") == expected_classifier.predict("phở bò")


def test_second_concurrent_retrain_is_blocked_by_lock():
    pool = FakeRetrainPool(FIXED_NOTES, FIXED_LABELS, correction_count=2)

    async def _call_twice():
        return await asyncio.gather(
            retrain_user_model(pool, "user-2"),
            retrain_user_model(pool, "user-2"),
            return_exceptions=True,
        )

    results = asyncio.run(_call_twice())

    succeeded = [r for r in results if r is None]
    blocked = [r for r in results if isinstance(r, TrainingInProgressError)]
    assert len(succeeded) == 1, "phải có đúng 1 lượt retrain chạy thành công"
    assert len(blocked) == 1, "lượt còn lại phải bị chặn bởi khoá is_training"