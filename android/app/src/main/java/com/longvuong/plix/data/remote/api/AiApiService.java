package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.CategorizeRequestDto;
import com.longvuong.plix.data.remote.dto.CategorizeResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApiService {

    @POST("api/v1/categorize")
    Call<CategorizeResponseDto> categorize(@Body CategorizeRequestDto body);
}