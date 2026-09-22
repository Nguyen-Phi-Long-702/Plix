from typing import Optional
from app.models.sync import CategorySyncRecord

class CategoryOut(CategorySyncRecord):
    user_id: Optional[str] = None