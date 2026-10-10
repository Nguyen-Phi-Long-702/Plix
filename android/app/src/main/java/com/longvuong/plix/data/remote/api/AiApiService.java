package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.AnomalyResponseDto;
import com.longvuong.plix.data.remote.dto.CategorizeRequestDto;
import com.longvuong.plix.data.remote.dto.CategorizeResponseDto;
import com.longvuong.plix.data.remote.dto.CorrectionRequestDto;
import com.longvuong.plix.data.remote.dto.RetrainResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AiApiService {

    @POST("api/v1/categorize")
    Call<CategorizeResponseDto> categorize(@Body CategorizeRequestDto body);
    @POST("api/v1/correction")
    Call<Void> submitCorrection(@Body CorrectionRequestDto body);
    @POST("api/v1/retrain")
    Call<RetrainResponseDto> retrain();

    @GET("api/v1/anomaly")
    Call<AnomalyResponseDto> checkAnomaly(@Query("category_id") String categoryId, @Query("amount") long amount);
}