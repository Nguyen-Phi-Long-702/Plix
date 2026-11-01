package com.longvuong.plix.data.remote.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HealthApiService {

    @GET("api/v1/health")
    Call<Void> health();
}