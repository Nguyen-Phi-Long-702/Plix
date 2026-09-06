package com.longvuong.plix.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "budgets",
        indices = {
                @Index("sync_status"),
                @Index(value = {"user_id", "period", "category_id"}, unique = true)
        }
)
public class BudgetEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "period")
    public String period;

    @Nullable
    @ColumnInfo(name = "category_id")
    public String categoryId;

    @ColumnInfo(name = "limit_amount")
    public long limitAmount;

    @ColumnInfo(name = "threshold_percent")
    public int thresholdPercent;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted;
}