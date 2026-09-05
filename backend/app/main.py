from fastapi import Depends, FastAPI
from app.core.security import verify_jwt
from app.routers import health
app = FastAPI(title="Plix API")
API_PREFIX = "/api/v1"
app.include_router(health.router, prefix=API_PREFIX)
@app.get(f"{API_PREFIX}/whoami")
def whoami(user_id: str = Depends(verify_jwt)):
    return {"user_id": user_id}
