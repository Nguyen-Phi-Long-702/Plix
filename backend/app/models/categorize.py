from typing import Optional
from pydantic import BaseModel


class CategorizeRequest(BaseModel):
    note: str


class CategorizeResponse(BaseModel):
    category_id: Optional[str] = None
    confidence: float