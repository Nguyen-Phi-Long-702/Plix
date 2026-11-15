package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequestDto {

    @SerializedName("type")
    public final String type;

    @SerializedName("email")
    public final String email;

    @SerializedName("token")
    public final String token;

    public VerifyOtpRequestDto(String email, String token) {
        this.type = "recovery";
        this.email = email;
        this.token = token;
    }
}