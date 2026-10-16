from pydantic import BaseModel


class AnomalyResponse(BaseModel):
    status: str
    explanation: str