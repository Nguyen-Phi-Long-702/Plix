package com.longvuong.plix.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.longvuong.plix.data.local.entity.CategoryEntity;

@Dao
public interface CategoryDao {

    @Insert
    void insert(CategoryEntity entity);

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity getById(String id);

    @Query("SELECT COUNT(*) FROM categories")
    int countAll();
}