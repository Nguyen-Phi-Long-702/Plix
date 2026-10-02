package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class TransactionSyncableEntity implements SyncableEntity<TransactionEntity, TransactionSyncRecordDto> {
    private static final String TABLE_NAME = "transactions";

    private final TransactionDao transactionDao;
    private final SyncApiService syncApiService;

    @Inject
    public TransactionSyncableEntity(TransactionDao transactionDao, SyncApiService syncApiService) {
        this.transactionDao = transactionDao;
        this.syncApiService = syncApiService;
    }

    @Override
    public String tableName() {
        return TABLE_NAME;
    }

    @Override
    public List<TransactionEntity> getPendingSync(String userId) {
        return transactionDao.getPendingSync(userId);
    }

    @Override
    public String entityId(TransactionEntity entity) {
        return entity.id;
    }

    @Override
    public long entityUpdatedAt(TransactionEntity entity) {
        return entity.updatedAt;
    }

    @Override
    public TransactionSyncRecordDto toRecord(TransactionEntity entity) {
        return TransactionSyncRecordDto.fromEntity(entity);
    }

    @Override
    public void markSynced(String id, long updatedAt) {
        transactionDao.markSynced(id, updatedAt);
    }

    @Override
    public Call<SyncPushResponseDto> push(List<TransactionSyncRecordDto> records) {
        return syncApiService.pushTransactions(new SyncPushRequestDto<>(records));
    }

    @Override
    public Call<SyncPullResponseDto<TransactionSyncRecordDto>> pull(long since, int limit) {
        return syncApiService.pullTransactions(since, limit);
    }

    @Override
    public String recordId(TransactionSyncRecordDto record) {
        return record.id;
    }

    @Override
    public long recordUpdatedAt(TransactionSyncRecordDto record) {
        return record.updatedAt;
    }

    @Override
    public TransactionEntity findById(String id) {
        return transactionDao.getById(id);
    }

    @Override
    public void insert(TransactionEntity entity) {
        transactionDao.insert(entity);
    }

    @Override
    public void update(TransactionEntity entity) {
        transactionDao.update(entity);
    }

    @Override
    public TransactionEntity toEntity(TransactionSyncRecordDto record, String userId) {
        return record.toEntity(userId);
    }
}