package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.longvuong.plix.data.local.entity.GoalEntity;

@Dao
public interface GoalDao {

    @Insert
    void insert(GoalEntity entity);

    @Query("SELECT * FROM goals WHERE id = :id")
    GoalEntity getById(String id);
}