from math import ceil

from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.core.rate_limit import RetrainRateLimitedError, check_retrain_cooldown
from app.core.security import verify_jwt
from app.models.retrain import RetrainResponse
from app.services.ai.model_store import load_model_params
from app.services.ai.retrain_service import TrainingInProgressError, retrain_user_model

router = APIRouter()

@router.post("/retrain", response_model=RetrainResponse)
async def retrain(
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> RetrainResponse:
    pool = request.app.state.db_pool

    try:
        await check_retrain_cooldown(pool, user_id)
    except RetrainRateLimitedError as error:
        retry_after_minutes = ceil(error.retry_after_seconds / 60)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Vui lòng đợi thêm khoảng {retry_after_minutes} phút trước khi huấn luyện lại",
        )

    try:
        await retrain_user_model(pool, user_id)
    except TrainingInProgressError:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Đang huấn luyện mô hình, vui lòng thử lại sau",
        )

    model_params = await load_model_params(pool, user_id)
    return RetrainResponse(
        trained_at=model_params.trained_at,
        training_sample_count=model_params.training_sample_count,
    )