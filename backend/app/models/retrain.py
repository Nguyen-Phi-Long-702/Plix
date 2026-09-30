from datetime import datetime

from pydantic import BaseModel

class RetrainResponse(BaseModel):
    trained_at: datetime
    training_sample_count: int