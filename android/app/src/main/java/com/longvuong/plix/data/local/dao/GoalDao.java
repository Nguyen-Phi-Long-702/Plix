package com.longvuong.plix.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.longvuong.plix.data.local.entity.GoalEntity;

import java.util.List;

@Dao
public interface GoalDao {
    @Insert
    void insert(GoalEntity entity);

    @Update
    void update(GoalEntity entity);

    @Query("SELECT * FROM goals WHERE id = :id")
    GoalEntity getById(String id);

    @Query("SELECT * FROM goals WHERE is_deleted = 0 ORDER BY deadline ASC")
    LiveData<List<GoalEntity>> getActiveGoals();

    @Query("SELECT * FROM goals WHERE sync_status = 'pending' AND user_id = :userId ORDER BY updated_at ASC")
    List<GoalEntity> getPendingSync(String userId);

    @Query("UPDATE goals SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt")
    void markSynced(String id, long updatedAt);
}