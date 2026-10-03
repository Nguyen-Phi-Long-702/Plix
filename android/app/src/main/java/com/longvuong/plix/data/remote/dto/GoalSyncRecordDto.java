package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.longvuong.plix.data.local.entity.GoalEntity;

public class GoalSyncRecordDto {
    @SerializedName("id")
    public final String id;

    @SerializedName("name")
    public final String name;

    @SerializedName("target_amount")
    public final long targetAmount;

    @SerializedName("current_amount")
    public final long currentAmount;

    @SerializedName("deadline")
    public final long deadline;

    @SerializedName("updated_at")
    public final long updatedAt;

    @SerializedName("is_deleted")
    public final boolean isDeleted;

    private GoalSyncRecordDto(GoalEntity entity) {
        this.id = entity.id;
        this.name = entity.name;
        this.targetAmount = entity.targetAmount;
        this.currentAmount = entity.currentAmount;
        this.deadline = entity.deadline;
        this.updatedAt = entity.updatedAt;
        this.isDeleted = entity.isDeleted;
    }

    //Máy chủ không gửi user_id về, user_id lấy từ phiên đăng nhập hiện tại, bản ghi vừa kéo về coi như đã đồng bộ
    public GoalEntity toEntity(String userId) {
        GoalEntity entity = new GoalEntity();
        entity.id = id;
        entity.userId = userId;
        entity.name = name;
        entity.targetAmount = targetAmount;
        entity.currentAmount = currentAmount;
        entity.deadline = deadline;
        entity.updatedAt = updatedAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    //Cố ý không có user_id và sync_status: server tự gán user_id từ JWT, sync_status chỉ tồn tại ở room
    public static GoalSyncRecordDto fromEntity(GoalEntity entity) {
        return new GoalSyncRecordDto(entity);
    }
}