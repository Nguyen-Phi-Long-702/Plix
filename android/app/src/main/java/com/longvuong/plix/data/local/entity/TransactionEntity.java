package com.longvuong.plix.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        indices = {
                @Index("occurred_at"),
                @Index("updated_at"),
                @Index("sync_status"),
                @Index({"user_id", "occurred_at"}),
                @Index({"user_id", "updated_at"})
        }
)
public class TransactionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "amount")
    public long amount;

    @NonNull
    @ColumnInfo(name = "type")
    public String type;

    @Nullable
    @ColumnInfo(name = "category_id")
    public String categoryId;

    @Nullable
    @ColumnInfo(name = "note")
    public String note;

    @Nullable
    @ColumnInfo(name = "payment_method")
    public String paymentMethod;

    @ColumnInfo(name = "occurred_at")
    public long occurredAt;

    @ColumnInfo(name = "is_recurring")
    public boolean isRecurring;

    @Nullable
    @ColumnInfo(name = "recurrence_rule")
    public String recurrenceRule;

    @Nullable
    @ColumnInfo(name = "recurrence_parent_id")
    public String recurrenceParentId;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted;
}