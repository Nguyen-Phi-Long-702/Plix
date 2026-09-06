package com.longvuong.plix.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "goals",
        indices = {
                @Index("sync_status")
        }
)
public class GoalEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "target_amount")
    public long targetAmount;

    @ColumnInfo(name = "current_amount")
    public long currentAmount;

    @ColumnInfo(name = "deadline")
    public long deadline;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted;
}