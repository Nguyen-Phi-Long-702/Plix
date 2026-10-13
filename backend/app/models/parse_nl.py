from typing import List, Optional

from pydantic import BaseModel


class ParseNlRequest(BaseModel):
    text: str


class ParseNlItem(BaseModel):
    amount: Optional[int] = None
    category_id: Optional[str] = None
    confidence: float
    source: str


class ParseNlResponse(BaseModel):
    items: List[ParseNlItem]