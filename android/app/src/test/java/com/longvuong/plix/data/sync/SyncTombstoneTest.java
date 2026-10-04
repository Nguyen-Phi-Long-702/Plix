package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.CorrectionEntity;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.dto.BudgetSyncRecordDto;
import com.longvuong.plix.data.remote.dto.CategorySyncRecordDto;
import com.longvuong.plix.data.remote.dto.CorrectionSyncRecordDto;
import com.longvuong.plix.data.remote.dto.GoalSyncRecordDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import org.junit.Test;

//Xoá mềm của cả 5 bảng: bản ghi đã xoá phải mang is_deleted = true khi đẩy lên (tombstone) và khi kéo về vẫn giữ is_deleted = true
public class SyncTombstoneTest {
    private static final String USER_ID = "user-1";

    @Test
    public void transaction_deletedEntity_keepsIsDeletedWhenPushedAndPulled() {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = USER_ID;
        entity.amount = 20000;
        entity.type = "expense";
        entity.occurredAt = 1L;
        entity.updatedAt = 2000L;
        entity.syncStatus = "pending";
        entity.isDeleted = true;

        TransactionSyncRecordDto record = TransactionSyncRecordDto.fromEntity(entity);
        assertTrue(record.isDeleted);
        TransactionEntity pulled = record.toEntity(USER_ID);
        assertTrue(pulled.isDeleted);
        assertEquals("synced", pulled.syncStatus);
    }

    @Test
    public void category_deletedEntity_keepsIsDeletedWhenPushedAndPulled() {
        CategoryEntity entity = new CategoryEntity();
        entity.id = "cat-1";
        entity.userId = USER_ID;
        entity.name = "Tiền điện";
        entity.type = "expense";
        entity.updatedAt = 2000L;
        entity.syncStatus = "pending";
        entity.isDeleted = true;

        CategorySyncRecordDto record = CategorySyncRecordDto.fromEntity(entity);
        assertTrue(record.isDeleted);
        CategoryEntity pulled = record.toEntity(USER_ID);
        assertTrue(pulled.isDeleted);
        assertEquals("synced", pulled.syncStatus);
    }

    @Test
    public void budget_deletedEntity_keepsIsDeletedWhenPushedAndPulled() {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = USER_ID;
        entity.period = "2026-10";
        entity.limitAmount = 3000000;
        entity.thresholdPercent = 80;
        entity.updatedAt = 2000L;
        entity.syncStatus = "pending";
        entity.isDeleted = true;

        BudgetSyncRecordDto record = BudgetSyncRecordDto.fromEntity(entity);
        assertTrue(record.isDeleted);
        BudgetEntity pulled = record.toEntity(USER_ID);
        assertTrue(pulled.isDeleted);
        assertEquals("synced", pulled.syncStatus);
    }

    @Test
    public void goal_deletedEntity_keepsIsDeletedWhenPushedAndPulled() {
        GoalEntity entity = new GoalEntity();
        entity.id = "goal-1";
        entity.userId = USER_ID;
        entity.name = "Mua xe";
        entity.targetAmount = 100000000;
        entity.deadline = 1767225600000L;
        entity.updatedAt = 2000L;
        entity.syncStatus = "pending";
        entity.isDeleted = true;

        GoalSyncRecordDto record = GoalSyncRecordDto.fromEntity(entity);
        assertTrue(record.isDeleted);
        GoalEntity pulled = record.toEntity(USER_ID);
        assertTrue(pulled.isDeleted);
        assertEquals("synced", pulled.syncStatus);
    }

    @Test
    public void correction_deletedEntity_keepsIsDeletedWhenPushedAndPulled() {
        CorrectionEntity entity = new CorrectionEntity();
        entity.id = "corr-1";
        entity.userId = USER_ID;
        entity.transactionId = "tx-1";
        entity.correctedCategoryId = "sys_an_uong";
        entity.createdAt = 1000L;
        entity.updatedAt = 2000L;
        entity.syncStatus = "pending";
        entity.isDeleted = true;

        CorrectionSyncRecordDto record = CorrectionSyncRecordDto.fromEntity(entity);
        assertTrue(record.isDeleted);
        CorrectionEntity pulled = record.toEntity(USER_ID);
        assertTrue(pulled.isDeleted);
        assertEquals("synced", pulled.syncStatus);
    }
}