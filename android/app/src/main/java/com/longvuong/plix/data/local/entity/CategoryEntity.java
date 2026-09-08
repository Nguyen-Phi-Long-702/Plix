package com.longvuong.plix.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

//Unique(user_id, name, type): cho phép cùng tên khác loại (vd "Quà tặng" vừa expense vừa income)
@Entity(
        tableName = "categories",
        indices = {
                @Index("updated_at"),
                @Index("sync_status"),
                @Index("user_id"),
                @Index(value = {"user_id", "name", "type"}, unique = true)
        }
)
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @Nullable
    @ColumnInfo(name = "user_id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @NonNull
    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    //Category hệ thống không cho phép user xoá-validate ở CategoryUseCase
    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted;
}