from typing import Generic, List, Literal, Optional, TypeVar
from pydantic import BaseModel, Field

class SyncRecordBase(BaseModel):
    id: str
    updated_at: int  # epoch milliseconds
    is_deleted: bool


class TransactionSyncRecord(SyncRecordBase):
    amount: int
    type: Literal["income", "expense"] = "expense"
    category_id: Optional[str] = None
    note: Optional[str] = ""
    payment_method: Optional[
        Literal["cash", "bank_transfer", "e_wallet", "credit_card", "other"]
    ] = None
    occurred_at: int
    is_recurring: bool = False
    recurrence_rule: Optional[str] = None
    recurrence_parent_id: Optional[str] = None


class CategorySyncRecord(SyncRecordBase):
    name: str
    type: Literal["income", "expense"] = "expense"


class BudgetSyncRecord(SyncRecordBase):
    period: str  # "YYYY-MM"
    category_id: Optional[str] = None
    limit_amount: int
    threshold_percent: int = Field(default=80, ge=50, le=100)


class GoalSyncRecord(SyncRecordBase):
    name: str
    target_amount: int
    current_amount: int = 0
    deadline: int  # epoch milliseconds


class CorrectionSyncRecord(SyncRecordBase):
    transaction_id: str
    predicted_category_id: Optional[str] = None
    corrected_category_id: str
    created_at: int  # epoch milliseconds, không đổi sau khi tạo


T = TypeVar("T", bound=SyncRecordBase)


class SyncPushRequest(BaseModel, Generic[T]):
    records: List[T]


class RejectedRecord(BaseModel):
    id: str
    reason: Literal["forbidden", "invalid_data"]


class SyncPushResponse(BaseModel):
    upserted_ids: List[str]
    rejected: List[RejectedRecord]


class SyncPullResponse(BaseModel, Generic[T]):
    records: List[T]
    has_more: bool