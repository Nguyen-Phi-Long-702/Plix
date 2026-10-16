from fastapi import APIRouter, Depends, Request

from app.core.security import verify_jwt
from app.models.anomaly import AnomalyResponse
from app.services.ai.anomaly_stats import check_anomaly

router = APIRouter()


@router.get("/anomaly", response_model=AnomalyResponse)
async def get_anomaly(
    request: Request,
    category_id: str,
    amount: int,
    user_id: str = Depends(verify_jwt),
) -> AnomalyResponse:
    anomaly_status, explanation = await check_anomaly(
        request.app.state.db_pool, user_id, category_id, amount
    )
    return AnomalyResponse(status=anomaly_status, explanation=explanation)