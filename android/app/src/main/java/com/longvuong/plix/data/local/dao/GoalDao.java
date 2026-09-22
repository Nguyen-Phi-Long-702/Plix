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
}