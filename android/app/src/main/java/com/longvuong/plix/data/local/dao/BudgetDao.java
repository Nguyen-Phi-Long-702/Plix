package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.longvuong.plix.data.local.entity.BudgetEntity;

@Dao
public interface BudgetDao {

    @Insert
    void insert(BudgetEntity entity);

    @Query("SELECT * FROM budgets WHERE id = :id")
    BudgetEntity getById(String id);
}