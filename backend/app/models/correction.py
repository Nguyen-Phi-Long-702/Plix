from typing import Optional

from pydantic import BaseModel

class CorrectionRequest(BaseModel):
    transaction_id: str
    predicted_category_id: Optional[str] = None
    corrected_category_id: str

class CorrectionResponse(BaseModel):
    id: str