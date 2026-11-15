package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RecoverRequestDto {

    @SerializedName("email")
    public final String email;

    public RecoverRequestDto(String email) {
        this.email = email;
    }
}