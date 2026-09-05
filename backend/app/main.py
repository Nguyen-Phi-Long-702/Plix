from fastapi import FastAPI
from app.routers import health
app = FastAPI(title="Plix API")
API_PREFIX = "/api/v1"
app.include_router(health.router, prefix=API_PREFIX)
