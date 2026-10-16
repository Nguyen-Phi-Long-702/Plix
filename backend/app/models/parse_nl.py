from typing import List, Optional

from pydantic import BaseModel


class ParseNlRequest(BaseModel):
    text: str
    # true = người dùng đang bật "Cho phép dùng Gemini dự phòng" trong Cài đặt Android.
    # Thiếu trường này (app cũ) = false: không được gọi Gemini.
    allow_gemini: bool = False


class ParseNlItem(BaseModel):
    amount: Optional[int] = None
    category_id: Optional[str] = None
    confidence: float
    source: str


class ParseNlResponse(BaseModel):
    items: List[ParseNlItem]