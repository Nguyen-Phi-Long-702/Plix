package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ParseNlResponseDto {
    @SerializedName("items")
    public List<ParseNlItemDto> items;

    public static class ParseNlItemDto {
        @Nullable
        @SerializedName("amount")
        public Long amount; //null = câu không có số tiền hợp lệ, cần nhập tay

        @Nullable
        @SerializedName("category_id")
        public String categoryId; //null = không khớp danh mục nào

        @SerializedName("confidence")
        public float confidence;
    }
}