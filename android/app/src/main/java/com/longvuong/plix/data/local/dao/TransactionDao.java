package com.longvuong.plix.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(TransactionEntity entity);

    @Update
    void update(TransactionEntity entity);

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY occurred_at DESC")
    LiveData<List<TransactionEntity>> getAll();

    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity getById(String id);
}