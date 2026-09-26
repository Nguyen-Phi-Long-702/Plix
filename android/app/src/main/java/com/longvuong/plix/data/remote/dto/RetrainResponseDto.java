package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RetrainResponseDto {
    @SerializedName("trained_at")
    public String trainedAt;

    @SerializedName("training_sample_count")
    public int trainingSampleCount;
}