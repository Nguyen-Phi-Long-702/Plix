from fastapi import APIRouter, Depends, Request

from app.core.security import verify_jwt
from app.models.sync import SyncPushRequest, SyncPushResponse, TransactionSyncRecord
from app.services.sync_service import push_transactions

router = APIRouter()


@router.post("/sync/transactions/push", response_model=SyncPushResponse)
async def sync_push_transactions(
    body: SyncPushRequest[TransactionSyncRecord],
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> SyncPushResponse:
    return await push_transactions(request.app.state.db_pool, user_id, body.records)