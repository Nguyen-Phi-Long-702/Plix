package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateUserRequestDto {

    @SerializedName("password")
    public final String password;

    public UpdateUserRequestDto(String password) {
        this.password = password;
    }
}