package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.longvuong.plix.data.local.entity.TransactionEntity;

public class TransactionSyncRecordDto {
    @SerializedName("id")
    public final String id;

    @SerializedName("amount")
    public final long amount;

    @SerializedName("type")
    public final String type;

    @Nullable
    @SerializedName("category_id")
    public final String categoryId;

    @Nullable
    @SerializedName("note")
    public final String note;

    @Nullable
    @SerializedName("payment_method")
    public final String paymentMethod;

    @SerializedName("occurred_at")
    public final long occurredAt;

    @SerializedName("is_recurring")
    public final boolean isRecurring;

    @Nullable
    @SerializedName("recurrence_rule")
    public final String recurrenceRule;

    @Nullable
    @SerializedName("recurrence_parent_id")
    public final String recurrenceParentId;

    @SerializedName("updated_at")
    public final long updatedAt;

    @SerializedName("is_deleted")
    public final boolean isDeleted;

    private TransactionSyncRecordDto(TransactionEntity entity) {
        this.id = entity.id;
        this.amount = entity.amount;
        this.type = entity.type;
        this.categoryId = entity.categoryId;
        this.note = entity.note;
        this.paymentMethod = entity.paymentMethod;
        this.occurredAt = entity.occurredAt;
        this.isRecurring = entity.isRecurring;
        this.recurrenceRule = entity.recurrenceRule;
        this.recurrenceParentId = entity.recurrenceParentId;
        this.updatedAt = entity.updatedAt;
        this.isDeleted = entity.isDeleted;
    }

    //Cố ý không có user_id và sync_status: server tự gán user_id từ JWT, sync_status chỉ tồn tại ở room
    public static TransactionSyncRecordDto fromEntity(TransactionEntity entity) {
        return new TransactionSyncRecordDto(entity);
    }
}