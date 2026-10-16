from typing import Optional

from pydantic import BaseModel


class ForecastResponse(BaseModel):
    status: str
    forecast_net_amount: Optional[int] = None