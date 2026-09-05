package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RefreshRequestDto {

    @SerializedName("refresh_token")
    public final String refreshToken;

    public RefreshRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}