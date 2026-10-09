from fastapi import APIRouter, Depends, Query, Request

from app.core.config import MAX_PULL_LIMIT
from app.core.security import verify_jwt
from app.models.sync import SyncPullResponse, SyncPushRequest, SyncPushResponse
from app.services.sync_service import (
    SYNC_TABLES,
    SyncTableSpec,
    pull_records,
    push_records,
)

router = APIRouter()


def _register_sync_routes(spec: SyncTableSpec) -> None:
    """Đăng ký cặp route /sync/<bảng>/push và /sync/<bảng>/pull cho 1 bảng.
    Mỗi bảng có model record riêng nên FastAPI tự kiểm tra body (422 nếu sai)."""
    table = spec.table
    push_request_model = SyncPushRequest[spec.record_model]
    pull_response_model = SyncPullResponse[spec.record_model]

    @router.post(f"/sync/{table.value}/push", response_model=SyncPushResponse)
    async def sync_push(
        body: push_request_model,
        request: Request,
        user_id: str = Depends(verify_jwt),
    ) -> SyncPushResponse:
        return await push_records(request.app.state.db_pool, table, user_id, body.records)

    @router.get(f"/sync/{table.value}/pull", response_model=pull_response_model)
    async def sync_pull(
        request: Request,
        since: int,
        limit: int = Query(500, ge=1),
        user_id: str = Depends(verify_jwt),
    ):
        return await pull_records(
            request.app.state.db_pool, table, user_id, since, min(limit, MAX_PULL_LIMIT)
        )


# SYNC_TABLES có khoá là enum SyncTable: chỉ 5 bảng trong enum mới có route.
for _spec in SYNC_TABLES.values():
    _register_sync_routes(_spec)