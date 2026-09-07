from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from fastapi.responses import JSONResponse

from app.models.error_response import ErrorResponse

MAX_REQUEST_BODY_BYTES = 1024 * 1024  # 1MB


class MaxBodySizeMiddleware(BaseHTTPMiddleware):
    """Từ chối sớm các request có Content-Length vượt quá giới hạn cho phép,
    trả về đúng cấu trúc ErrorResponse dùng chung.
    Giới hạn phạm vi: chỉ kiểm tra header Content-Length (không đếm byte thật
    của body dạng chunked/không khai báo Content-Length) — đủ cho phạm vi hiện tại."""

    async def dispatch(self, request: Request, call_next):
        content_length = request.headers.get("content-length")
        if content_length is not None:
            try:
                length = int(content_length)
            except ValueError:
                length = None
            if length is not None and length > MAX_REQUEST_BODY_BYTES:
                body = ErrorResponse(
                    error_code="PAYLOAD_TOO_LARGE",
                    message=f"Kích thước yêu cầu vượt quá giới hạn cho phép ({MAX_REQUEST_BODY_BYTES} bytes)",
                )
                return JSONResponse(status_code=413, content=body.model_dump())
        return await call_next(request)