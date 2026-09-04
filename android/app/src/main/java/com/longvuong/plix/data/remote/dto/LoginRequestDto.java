package com.longvuong.plix.data.remote.dto;

public class LoginRequestDto {
    public final String email;
    public final String password;

    public LoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}