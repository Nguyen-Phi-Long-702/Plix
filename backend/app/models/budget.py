from app.models.sync import BudgetSyncRecord, SyncPushRequest, SyncPullResponse

BudgetSyncPushRequest = SyncPushRequest[BudgetSyncRecord]
BudgetSyncPullResponse = SyncPullResponse[BudgetSyncRecord]