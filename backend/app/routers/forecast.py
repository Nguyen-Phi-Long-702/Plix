import time
from typing import Optional

from fastapi import APIRouter, Depends, Query, Request

from app.core.security import verify_jwt
from app.models.forecast import ForecastResponse
from app.services.ai.forecast import check_forecast

router = APIRouter()


def _now_ms() -> int:
    return int(time.time() * 1000)


@router.get("/forecast", response_model=ForecastResponse)
async def get_forecast(
    request: Request,
    month: Optional[str] = Query(None, pattern=r"^20\d{2}-(0[1-9]|1[0-2])$"),
    user_id: str = Depends(verify_jwt),
) -> ForecastResponse:
    forecast_status, forecast_net_amount = await check_forecast(
        request.app.state.db_pool, user_id, month, _now_ms()
    )
    return ForecastResponse(status=forecast_status, forecast_net_amount=forecast_net_amount)