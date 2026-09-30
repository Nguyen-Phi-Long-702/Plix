package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CategorizeResponseDto {
    @SerializedName("category_id")
    public String categoryId;

    @SerializedName("confidence")
    public float confidence;
}
