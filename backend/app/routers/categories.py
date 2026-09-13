from typing import List
from fastapi import APIRouter, Depends, Request
from app.core.security import verify_jwt
from app.models.category import CategoryOut

router = APIRouter()


@router.get("/categories", response_model=List[CategoryOut])
async def list_categories(request: Request, user_id: str = Depends(verify_jwt)):
    """Trả về category HỆ THỐNG (user_id NULL) + category CỦA CHÍNH user
    đang đăng nhập, is_deleted=false."""
    rows = await request.app.state.db_pool.fetch(
        """
        SELECT id, user_id, name, type, updated_at, is_deleted
        FROM categories
        WHERE is_deleted = false AND (user_id IS NULL OR user_id = $1)
        ORDER BY user_id NULLS FIRST, name
        """,
        user_id,
    )
    return [CategoryOut(**dict(row)) for row in rows]