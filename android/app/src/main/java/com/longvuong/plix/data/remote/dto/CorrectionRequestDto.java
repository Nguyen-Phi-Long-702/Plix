package com.longvuong.plix.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class CorrectionRequestDto {
    @SerializedName("transaction_id")
    public final String transactionId;

    @Nullable
    @SerializedName("predicted_category_id")
    public final String predictedCategoryId;

    @SerializedName("corrected_category_id")
    public final String correctedCategoryId;

    public CorrectionRequestDto(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId) {
        this.transactionId = transactionId;
        this.predictedCategoryId = predictedCategoryId;
        this.correctedCategoryId = correctedCategoryId;
    }
}