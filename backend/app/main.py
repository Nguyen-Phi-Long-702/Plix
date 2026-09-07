from contextlib import asynccontextmanager

import asyncpg
from fastapi import Depends, FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.config import DATABASE_URL
from app.core.security import verify_jwt
from app.models.error_response import ErrorResponse
from app.routers import health


API_PREFIX = "/api/v1"

_ERROR_CODE_BY_STATUS = {
    400: "BAD_REQUEST",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    405: "METHOD_NOT_ALLOWED",
    413: "PAYLOAD_TOO_LARGE",
    422: "VALIDATION_ERROR",
    500: "INTERNAL_ERROR",
}


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db_pool = await asyncpg.create_pool(
        dsn=DATABASE_URL, ssl="require", min_size=1, max_size=3
    )
    yield
    await app.state.db_pool.close()


app = FastAPI(title="Plix API", lifespan=lifespan)


@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    error_code = _ERROR_CODE_BY_STATUS.get(exc.status_code, "HTTP_ERROR")
    body = ErrorResponse(error_code=error_code, message=str(exc.detail))
    return JSONResponse(status_code=exc.status_code, content=body.model_dump())


app.include_router(health.router, prefix=API_PREFIX)


@app.get(f"{API_PREFIX}/whoami")
def whoami(user_id: str = Depends(verify_jwt)):
    return {"user_id": user_id}