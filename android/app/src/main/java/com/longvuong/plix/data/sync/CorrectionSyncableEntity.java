package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.dao.CorrectionDao;
import com.longvuong.plix.data.local.entity.CorrectionEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.CorrectionSyncRecordDto;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class CorrectionSyncableEntity implements SyncableEntity<CorrectionEntity, CorrectionSyncRecordDto> {
    private static final String TABLE_NAME = "corrections";

    private final CorrectionDao correctionDao;
    private final SyncApiService syncApiService;

    @Inject
    public CorrectionSyncableEntity(CorrectionDao correctionDao, SyncApiService syncApiService) {
        this.correctionDao = correctionDao;
        this.syncApiService = syncApiService;
    }

    @Override
    public String tableName() {
        return TABLE_NAME;
    }

    @Override
    public List<CorrectionEntity> getPendingSync(String userId) {
        return correctionDao.getPendingSync(userId);
    }

    @Override
    public String entityId(CorrectionEntity entity) {
        return entity.id;
    }

    @Override
    public long entityUpdatedAt(CorrectionEntity entity) {
        return entity.updatedAt;
    }

    @Override
    public CorrectionSyncRecordDto toRecord(CorrectionEntity entity) {
        return CorrectionSyncRecordDto.fromEntity(entity);
    }

    @Override
    public void markSynced(String id, long updatedAt) {
        correctionDao.markSynced(id, updatedAt);
    }

    @Override
    public Call<SyncPushResponseDto> push(List<CorrectionSyncRecordDto> records) {
        return syncApiService.pushCorrections(new SyncPushRequestDto<>(records));
    }

    @Override
    public Call<SyncPullResponseDto<CorrectionSyncRecordDto>> pull(long since, int limit) {
        return syncApiService.pullCorrections(since, limit);
    }

    @Override
    public String recordId(CorrectionSyncRecordDto record) {
        return record.id;
    }

    @Override
    public long recordUpdatedAt(CorrectionSyncRecordDto record) {
        return record.updatedAt;
    }

    @Override
    public CorrectionEntity findById(String id) {
        return correctionDao.getById(id);
    }

    @Override
    public void insert(CorrectionEntity entity) {
        correctionDao.insert(entity);
    }

    @Override
    public void update(CorrectionEntity entity) {
        correctionDao.update(entity);
    }

    @Override
    public CorrectionEntity toEntity(CorrectionSyncRecordDto record, String userId) {
        return record.toEntity(userId);
    }
}