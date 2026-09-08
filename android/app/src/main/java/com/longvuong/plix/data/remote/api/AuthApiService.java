package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.AuthResponseDto;
import com.longvuong.plix.data.remote.dto.LoginRequestDto;
import com.longvuong.plix.data.remote.dto.RefreshRequestDto;
import com.longvuong.plix.data.remote.dto.SignupRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApiService {

    @POST("auth/v1/signup")
    Call<AuthResponseDto> signup(@Body SignupRequestDto body);

    @POST("auth/v1/token")
    Call<AuthResponseDto> login(
            @Query("grant_type") String grantType,
            @Body LoginRequestDto body
    );

    @POST("auth/v1/token")
    Call<AuthResponseDto> refresh(
            @Query("grant_type") String grantType,
            @Body RefreshRequestDto body
    );
}