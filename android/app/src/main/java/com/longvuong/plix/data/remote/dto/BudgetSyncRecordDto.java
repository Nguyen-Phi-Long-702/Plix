package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.longvuong.plix.data.local.entity.BudgetEntity;

public class BudgetSyncRecordDto {
    @SerializedName("id")
    public final String id;

    @SerializedName("period")
    public final String period;

    @Nullable
    @SerializedName("category_id")
    public final String categoryId;

    @SerializedName("limit_amount")
    public final long limitAmount;

    @SerializedName("threshold_percent")
    public final int thresholdPercent;

    @SerializedName("updated_at")
    public final long updatedAt;

    @SerializedName("is_deleted")
    public final boolean isDeleted;

    private BudgetSyncRecordDto(BudgetEntity entity) {
        this.id = entity.id;
        this.period = entity.period;
        this.categoryId = entity.categoryId;
        this.limitAmount = entity.limitAmount;
        this.thresholdPercent = entity.thresholdPercent;
        this.updatedAt = entity.updatedAt;
        this.isDeleted = entity.isDeleted;
    }

    //Máy chủ không gửi user_id về, user_id lấy từ phiên đăng nhập hiện tại, bản ghi vừa kéo về coi như đã đồng bộ
    public BudgetEntity toEntity(String userId) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = id;
        entity.userId = userId;
        entity.period = period;
        entity.categoryId = categoryId;
        entity.limitAmount = limitAmount;
        entity.thresholdPercent = thresholdPercent;
        entity.updatedAt = updatedAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    //Cố ý không có user_id và sync_status: server tự gán user_id từ JWT, sync_status chỉ tồn tại ở room
    public static BudgetSyncRecordDto fromEntity(BudgetEntity entity) {
        return new BudgetSyncRecordDto(entity);
    }
}