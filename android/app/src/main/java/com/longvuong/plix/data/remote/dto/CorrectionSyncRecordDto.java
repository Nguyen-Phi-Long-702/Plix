package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.longvuong.plix.data.local.entity.CorrectionEntity;

public class CorrectionSyncRecordDto {
    @SerializedName("id")
    public final String id;

    @SerializedName("transaction_id")
    public final String transactionId;

    @Nullable
    @SerializedName("predicted_category_id")
    public final String predictedCategoryId;

    @SerializedName("corrected_category_id")
    public final String correctedCategoryId;

    @SerializedName("created_at")
    public final long createdAt;

    @SerializedName("updated_at")
    public final long updatedAt;

    @SerializedName("is_deleted")
    public final boolean isDeleted;

    private CorrectionSyncRecordDto(CorrectionEntity entity) {
        this.id = entity.id;
        this.transactionId = entity.transactionId;
        this.predictedCategoryId = entity.predictedCategoryId;
        this.correctedCategoryId = entity.correctedCategoryId;
        this.createdAt = entity.createdAt;
        this.updatedAt = entity.updatedAt;
        this.isDeleted = entity.isDeleted;
    }

    //Máy chủ không gửi user_id về, user_id lấy từ phiên đăng nhập hiện tại, bản ghi vừa kéo về coi như đã đồng bộ
    public CorrectionEntity toEntity(String userId) {
        CorrectionEntity entity = new CorrectionEntity();
        entity.id = id;
        entity.userId = userId;
        entity.transactionId = transactionId;
        entity.predictedCategoryId = predictedCategoryId;
        entity.correctedCategoryId = correctedCategoryId;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    //Cố ý không có user_id và sync_status: server tự gán user_id từ JWT, sync_status chỉ tồn tại ở room
    public static CorrectionSyncRecordDto fromEntity(CorrectionEntity entity) {
        return new CorrectionSyncRecordDto(entity);
    }
}