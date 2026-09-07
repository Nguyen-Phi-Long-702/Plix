from fastapi import APIRouter, Request
router = APIRouter()
@router.get("/health")
async def health_check(request: Request):
    try:
        await request.app.state.db_pool.fetchval("SELECT 1")
        database_status = "connected"
    except Exception:
        database_status = "disconnected"
    return {"status": "OK", "database": database_status}