from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.testclient import TestClient
from pydantic import BaseModel

from app.models.error_response import ErrorResponse


class _EchoBody(BaseModel):
    note: str


def _build_app() -> FastAPI:
    app = FastAPI()

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request, exc):
        body = ErrorResponse(error_code="VALIDATION_ERROR", message=str(exc.errors()))
        return JSONResponse(status_code=422, content=body.model_dump())

    @app.post("/echo")
    async def echo(body: _EchoBody):
        return body

    return app


def test_missing_field_returns_shared_error_response_format():
    client = TestClient(_build_app())

    response = client.post("/echo", json={})

    assert response.status_code == 422
    data = response.json()
    assert set(data.keys()) == {"error_code", "message"}
    assert data["error_code"] == "VALIDATION_ERROR"