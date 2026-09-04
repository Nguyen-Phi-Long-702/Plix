package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponseDto {

    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;
}