package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.dao.BudgetDao;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.BudgetSyncRecordDto;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class BudgetSyncableEntity implements SyncableEntity<BudgetEntity, BudgetSyncRecordDto> {
    private static final String TABLE_NAME = "budgets";

    private final BudgetDao budgetDao;
    private final SyncApiService syncApiService;

    @Inject
    public BudgetSyncableEntity(BudgetDao budgetDao, SyncApiService syncApiService) {
        this.budgetDao = budgetDao;
        this.syncApiService = syncApiService;
    }

    @Override
    public String tableName() {
        return TABLE_NAME;
    }

    @Override
    public List<BudgetEntity> getPendingSync(String userId) {
        return budgetDao.getPendingSync(userId);
    }

    @Override
    public String entityId(BudgetEntity entity) {
        return entity.id;
    }

    @Override
    public long entityUpdatedAt(BudgetEntity entity) {
        return entity.updatedAt;
    }

    @Override
    public BudgetSyncRecordDto toRecord(BudgetEntity entity) {
        return BudgetSyncRecordDto.fromEntity(entity);
    }

    @Override
    public void markSynced(String id, long updatedAt) {
        budgetDao.markSynced(id, updatedAt);
    }

    @Override
    public Call<SyncPushResponseDto> push(List<BudgetSyncRecordDto> records) {
        return syncApiService.pushBudgets(new SyncPushRequestDto<>(records));
    }

    @Override
    public Call<SyncPullResponseDto<BudgetSyncRecordDto>> pull(long since, int limit) {
        return syncApiService.pullBudgets(since, limit);
    }

    @Override
    public String recordId(BudgetSyncRecordDto record) {
        return record.id;
    }

    @Override
    public long recordUpdatedAt(BudgetSyncRecordDto record) {
        return record.updatedAt;
    }

    @Override
    public BudgetEntity findById(String id) {
        return budgetDao.getById(id);
    }

    @Override
    public void insert(BudgetEntity entity) {
        budgetDao.insert(entity);
    }

    @Override
    public void update(BudgetEntity entity) {
        budgetDao.update(entity);
    }

    @Override
    public BudgetEntity toEntity(BudgetSyncRecordDto record, String userId) {
        return record.toEntity(userId);
    }
}