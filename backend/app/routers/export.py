from fastapi import APIRouter, Depends, HTTPException, Request, Response, status

from app.core.rate_limit import ExportRateLimitedError, check_export_cooldown
from app.core.security import verify_jwt
from app.services.export_service import build_transactions_csv, fetch_transactions_for_export

router = APIRouter()


@router.get("/export/csv")
async def export_csv(
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> Response:
    try:
        check_export_cooldown(user_id)
    except ExportRateLimitedError as error:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Vui lòng đợi thêm {error.retry_after_seconds} giây trước khi xuất dữ liệu lại",
        )

    rows = await fetch_transactions_for_export(request.app.state.db_pool, user_id)
    return Response(
        content=build_transactions_csv(rows),
        media_type="text/csv",
        headers={"Content-Disposition": 'attachment; filename="plix-giao-dich.csv"'},
    )