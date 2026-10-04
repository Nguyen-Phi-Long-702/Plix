package com.longvuong.plix.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.longvuong.plix.data.local.entity.BudgetEntity;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(BudgetEntity entity);

    @Update
    void update(BudgetEntity entity);

    @Query("SELECT * FROM budgets WHERE id = :id")
    BudgetEntity getById(String id);

    @Query("SELECT * FROM budgets WHERE period = :period AND is_deleted = 0 ORDER BY category_id IS NULL DESC")
    LiveData<List<BudgetEntity>> getActiveByPeriod(String period);

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND period = :period AND category_id IS NULL AND is_deleted = 0 LIMIT 1")
    BudgetEntity findOverallBudgetByUserAndPeriod(String userId, String period);

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND period = :period AND category_id = :categoryId AND is_deleted = 0 LIMIT 1")
    BudgetEntity findCategoryBudgetByUserAndPeriod(String userId, String period, String categoryId);

    @Query("SELECT * FROM budgets WHERE user_id = :userId AND period = :period AND category_id = :categoryId AND is_deleted = 1 LIMIT 1")
    BudgetEntity findDeletedCategoryBudget(String userId, String period, String categoryId);

    @Query("SELECT * FROM budgets WHERE sync_status = 'pending' AND user_id = :userId ORDER BY updated_at ASC")
    List<BudgetEntity> getPendingSync(String userId);

    @Query("UPDATE budgets SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt")
    void markSynced(String id, long updatedAt);
}