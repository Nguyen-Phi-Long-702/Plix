from fastapi import APIRouter, Depends, HTTPException, Request, status

from app.core.security import verify_jwt
from app.models.categorize import CategorizeRequest, CategorizeResponse
from app.services.ai.categorize_service import NoModelAvailableError, classify_note

router = APIRouter()


@router.post("/categorize", response_model=CategorizeResponse)
async def categorize(
    body: CategorizeRequest,
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> CategorizeResponse:

    try:
        category_id, confidence = await classify_note(
            request.app.state.db_pool, user_id, body.note
        )
    except NoModelAvailableError:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Chưa có model AI nào sẵn sàng để phân loại",
        )
    return CategorizeResponse(category_id=category_id, confidence=confidence)