package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.longvuong.plix.data.local.entity.TransactionEntity;

@Dao
public interface TransactionDao {

    @Insert
    void insert(TransactionEntity entity);

    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity getById(String id);
}