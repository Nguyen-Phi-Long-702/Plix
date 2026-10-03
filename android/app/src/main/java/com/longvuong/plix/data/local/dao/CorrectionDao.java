package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.longvuong.plix.data.local.entity.CorrectionEntity;

import java.util.List;

@Dao
public interface CorrectionDao {
    @Insert
    void insert(CorrectionEntity entity);

    @Update
    void update(CorrectionEntity entity);

    @Query("SELECT * FROM corrections WHERE id = :id")
    CorrectionEntity getById(String id);

    @Query("SELECT * FROM corrections WHERE sync_status = 'pending' AND user_id = :userId ORDER BY updated_at ASC")
    List<CorrectionEntity> getPendingSync(String userId);

    @Query("UPDATE corrections SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt")
    void markSynced(String id, long updatedAt);
}