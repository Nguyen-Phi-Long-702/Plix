package com.longvuong.plix.data.remote.dto;

public class SignupRequestDto {
    public final String email;
    public final String password;

    public SignupRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}