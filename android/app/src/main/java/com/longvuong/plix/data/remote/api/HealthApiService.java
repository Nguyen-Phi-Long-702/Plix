package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.WhoamiResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HealthApiService {

    @GET("api/v1/health")
    Call<Void> health();

    @GET("api/v1/whoami")
    Call<WhoamiResponseDto> whoami();
}