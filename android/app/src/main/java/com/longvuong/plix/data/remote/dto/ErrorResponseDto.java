package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponseDto {
    @SerializedName("error_code")
    public String errorCode;

    @SerializedName("message")
    public String message;
}