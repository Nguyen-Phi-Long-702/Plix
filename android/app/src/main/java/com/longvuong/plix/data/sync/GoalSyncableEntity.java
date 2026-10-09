package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.dao.GoalDao;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.GoalSyncRecordDto;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class GoalSyncableEntity implements SyncableEntity<GoalEntity, GoalSyncRecordDto> {
    private static final String TABLE_NAME = "goals";

    private final GoalDao goalDao;
    private final SyncApiService syncApiService;

    @Inject
    public GoalSyncableEntity(GoalDao goalDao, SyncApiService syncApiService) {
        this.goalDao = goalDao;
        this.syncApiService = syncApiService;
    }

    @Override
    public String tableName() {
        return TABLE_NAME;
    }

    @Override
    public List<GoalEntity> getPendingSync(String userId) {
        return goalDao.getPendingSync(userId);
    }

    @Override
    public String entityId(GoalEntity entity) {
        return entity.id;
    }

    @Override
    public long entityUpdatedAt(GoalEntity entity) {
        return entity.updatedAt;
    }

    @Override
    public GoalSyncRecordDto toRecord(GoalEntity entity) {
        return GoalSyncRecordDto.fromEntity(entity);
    }

    @Override
    public void markSynced(String id, long updatedAt) {
        goalDao.markSynced(id, updatedAt);
    }

    @Override
    public Call<SyncPushResponseDto> push(List<GoalSyncRecordDto> records) {
        return syncApiService.pushGoals(new SyncPushRequestDto<>(records));
    }

    @Override
    public Call<SyncPullResponseDto<GoalSyncRecordDto>> pull(long since, int limit) {
        return syncApiService.pullGoals(since, limit);
    }

    @Override
    public String recordId(GoalSyncRecordDto record) {
        return record.id;
    }

    @Override
    public long recordUpdatedAt(GoalSyncRecordDto record) {
        return record.updatedAt;
    }

    @Override
    public GoalEntity findById(String id) {
        return goalDao.getById(id);
    }

    @Override
    public void insert(GoalEntity entity) {
        goalDao.insert(entity);
    }

    @Override
    public void update(GoalEntity entity) {
        goalDao.update(entity);
    }

    @Override
    public GoalEntity toEntity(GoalSyncRecordDto record, String userId) {
        return record.toEntity(userId);
    }
}