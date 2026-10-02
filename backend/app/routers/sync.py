from fastapi import APIRouter, Depends, Query, Request

from app.core.config import MAX_PULL_LIMIT
from app.core.security import verify_jwt
from app.models.sync import (
    SyncPullResponse,
    SyncPushRequest,
    SyncPushResponse,
    TransactionSyncRecord,
)
from app.services.sync_service import pull_transactions, push_transactions

router = APIRouter()


@router.post("/sync/transactions/push", response_model=SyncPushResponse)
async def sync_push_transactions(
    body: SyncPushRequest[TransactionSyncRecord],
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> SyncPushResponse:
    return await push_transactions(request.app.state.db_pool, user_id, body.records)


@router.get(
    "/sync/transactions/pull",
    response_model=SyncPullResponse[TransactionSyncRecord],
)
async def sync_pull_transactions(
    request: Request,
    since: int,
    limit: int = Query(500, ge=1),
    user_id: str = Depends(verify_jwt),
) -> SyncPullResponse[TransactionSyncRecord]:
    return await pull_transactions(
        request.app.state.db_pool, user_id, since, min(limit, MAX_PULL_LIMIT)
    )