package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.longvuong.plix.data.local.entity.CorrectionEntity;

@Dao
public interface CorrectionDao {

    @Insert
    void insert(CorrectionEntity entity);

    @Query("SELECT * FROM corrections WHERE id = :id")
    CorrectionEntity getById(String id);
}