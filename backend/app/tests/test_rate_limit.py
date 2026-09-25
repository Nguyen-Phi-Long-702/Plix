import asyncio

import app.core.rate_limit as rate_limit_module
from app.core.rate_limit import RetrainRateLimitedError, check_retrain_cooldown

class FakeCooldownPool:
    def __init__(self, elapsed_seconds):
        self._elapsed_seconds = elapsed_seconds

    async def fetchrow(self, query, user_id):
        if self._elapsed_seconds is None:
            return None
        return {"elapsed_seconds": self._elapsed_seconds}

def test_allows_first_retrain_when_user_has_no_previous_model():
    pool = FakeCooldownPool(elapsed_seconds=None)

    asyncio.run(check_retrain_cooldown(pool, "user-1")) 

def test_blocks_when_elapsed_seconds_less_than_cooldown(monkeypatch):
    monkeypatch.setattr(rate_limit_module, "RETRAIN_COOLDOWN_SECONDS", 3600)
    pool = FakeCooldownPool(elapsed_seconds=100)

    try:
        asyncio.run(check_retrain_cooldown(pool, "user-1"))
        assert False, "phải raise RetrainRateLimitedError"
    except RetrainRateLimitedError as error:
        assert error.retry_after_seconds == 3500

def test_allows_when_elapsed_seconds_equal_to_cooldown(monkeypatch):
    monkeypatch.setattr(rate_limit_module, "RETRAIN_COOLDOWN_SECONDS", 3600)
    pool = FakeCooldownPool(elapsed_seconds=3600)

    asyncio.run(check_retrain_cooldown(pool, "user-1"))

def test_allows_when_elapsed_seconds_greater_than_cooldown(monkeypatch):
    monkeypatch.setattr(rate_limit_module, "RETRAIN_COOLDOWN_SECONDS", 3600)
    pool = FakeCooldownPool(elapsed_seconds=7200)

    asyncio.run(check_retrain_cooldown(pool, "user-1")) 