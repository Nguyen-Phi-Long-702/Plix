from contextlib import asynccontextmanager

import asyncpg 
from fastapi import Depends, FastAPI

from app.core.config import DATABASE_URL
from app.core.security import verify_jwt
from app.routers import health


API_PREFIX = "/api/v1"


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db_pool = await asyncpg.create_pool(
        dsn=DATABASE_URL, ssl="require", min_size=1, max_size=3
    )
    yield
    await app.state.db_pool.close()


app = FastAPI(title="Plix API", lifespan=lifespan)
app.include_router(health.router, prefix=API_PREFIX)


@app.get(f"{API_PREFIX}/whoami")
def whoami(user_id: str = Depends(verify_jwt)):
    return {"user_id": user_id}