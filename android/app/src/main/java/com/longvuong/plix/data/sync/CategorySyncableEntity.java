package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.CategorySyncRecordDto;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class CategorySyncableEntity implements SyncableEntity<CategoryEntity, CategorySyncRecordDto> {
    private static final String TABLE_NAME = "categories";

    private final CategoryDao categoryDao;
    private final SyncApiService syncApiService;

    @Inject
    public CategorySyncableEntity(CategoryDao categoryDao, SyncApiService syncApiService) {
        this.categoryDao = categoryDao;
        this.syncApiService = syncApiService;
    }

    @Override
    public String tableName() {
        return TABLE_NAME;
    }

    @Override
    public List<CategoryEntity> getPendingSync(String userId) {
        return categoryDao.getPendingSync(userId);
    }

    @Override
    public String entityId(CategoryEntity entity) {
        return entity.id;
    }

    @Override
    public long entityUpdatedAt(CategoryEntity entity) {
        return entity.updatedAt;
    }

    @Override
    public CategorySyncRecordDto toRecord(CategoryEntity entity) {
        return CategorySyncRecordDto.fromEntity(entity);
    }

    @Override
    public void markSynced(String id, long updatedAt) {
        categoryDao.markSynced(id, updatedAt);
    }

    @Override
    public Call<SyncPushResponseDto> push(List<CategorySyncRecordDto> records) {
        return syncApiService.pushCategories(new SyncPushRequestDto<>(records));
    }

    @Override
    public Call<SyncPullResponseDto<CategorySyncRecordDto>> pull(long since, int limit) {
        return syncApiService.pullCategories(since, limit);
    }

    @Override
    public String recordId(CategorySyncRecordDto record) {
        return record.id;
    }

    @Override
    public long recordUpdatedAt(CategorySyncRecordDto record) {
        return record.updatedAt;
    }

    @Override
    public CategoryEntity findById(String id) {
        return categoryDao.getById(id);
    }

    @Override
    public void insert(CategoryEntity entity) {
        categoryDao.insert(entity);
    }

    @Override
    public void update(CategoryEntity entity) {
        categoryDao.update(entity);
    }

    @Override
    public CategoryEntity toEntity(CategorySyncRecordDto record, String userId) {
        return record.toEntity(userId);
    }
}