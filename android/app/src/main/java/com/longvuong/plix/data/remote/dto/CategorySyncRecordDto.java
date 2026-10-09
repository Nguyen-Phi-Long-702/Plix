package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.longvuong.plix.data.local.entity.CategoryEntity;

public class CategorySyncRecordDto {
    @SerializedName("id")
    public final String id;

    @SerializedName("name")
    public final String name;

    @SerializedName("type")
    public final String type;

    @SerializedName("updated_at")
    public final long updatedAt;

    @SerializedName("is_deleted")
    public final boolean isDeleted;

    private CategorySyncRecordDto(CategoryEntity entity) {
        this.id = entity.id;
        this.name = entity.name;
        this.type = entity.type;
        this.updatedAt = entity.updatedAt;
        this.isDeleted = entity.isDeleted;
    }

    //Máy chủ chỉ trả danh mục do chính user tạo (không trả danh mục hệ thống) nên mọi bản ghi kéo về đều thuộc user hiện tại
    public CategoryEntity toEntity(String userId) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.userId = userId;
        entity.name = name;
        entity.type = type;
        entity.updatedAt = updatedAt;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    //Cố ý không có user_id và sync_status: server tự gán user_id từ JWT, sync_status chỉ tồn tại ở room
    public static CategorySyncRecordDto fromEntity(CategoryEntity entity) {
        return new CategorySyncRecordDto(entity);
    }
}