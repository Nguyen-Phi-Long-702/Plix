from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.core.security import verify_jwt
from app.models.correction import CorrectionRequest, CorrectionResponse
from app.services.ai.correction_service import TransactionNotOwnedError, save_correction

router = APIRouter()


@router.post(
    "/correction",
    response_model=CorrectionResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_correction(
    body: CorrectionRequest,
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> CorrectionResponse:
    try:
        correction_id = await save_correction(
            request.app.state.db_pool,
            user_id,
            body.transaction_id,
            body.predicted_category_id,
            body.corrected_category_id,
        )
    except TransactionNotOwnedError:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Giao dịch không tồn tại hoặc không thuộc về người dùng này",
        )
    return CorrectionResponse(id=correction_id)