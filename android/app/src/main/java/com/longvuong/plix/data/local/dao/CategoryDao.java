package com.longvuong.plix.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(CategoryEntity entity);

    @Update
    void update(CategoryEntity entity);

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity getById(String id);

    @Query("SELECT COUNT(*) FROM categories")
    int countAll();

    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY type, name")
    LiveData<List<CategoryEntity>> getActiveCategories();

    @Query("SELECT * FROM categories WHERE user_id IS NULL AND is_deleted = 0 " +
            "AND name = :name AND type = :type LIMIT 1")
    CategoryEntity findSystemCategoryByNameAndType(String name, String type);

    @Query("SELECT * FROM categories WHERE sync_status = 'pending' AND user_id = :userId ORDER BY updated_at ASC")
    List<CategoryEntity> getPendingSync(String userId);

    @Query("UPDATE categories SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt")
    void markSynced(String id, long updatedAt);
}