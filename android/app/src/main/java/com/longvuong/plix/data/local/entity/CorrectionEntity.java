package com.longvuong.plix.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "corrections",
        indices = {
                @Index("updated_at"),
                @Index("sync_status"),
                @Index({"user_id", "transaction_id"}),
                @Index({"user_id", "updated_at"})
        }
)
public class CorrectionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "transaction_id")
    public String transactionId;

    @Nullable
    @ColumnInfo(name = "predicted_category_id")
    public String predictedCategoryId;

    @NonNull
    @ColumnInfo(name = "corrected_category_id")
    public String correctedCategoryId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted;
}