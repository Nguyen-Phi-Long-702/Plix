package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.CategorizeRequestDto;
import com.longvuong.plix.data.remote.dto.CategorizeResponseDto;
import com.longvuong.plix.data.remote.dto.CorrectionRequestDto;
import com.longvuong.plix.data.remote.dto.RetrainResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApiService {

    @POST("api/v1/categorize")
    Call<CategorizeResponseDto> categorize(@Body CategorizeRequestDto body);
    @POST("api/v1/correction")
    Call<Void> submitCorrection(@Body CorrectionRequestDto body);
    @POST("api/v1/retrain")
    Call<RetrainResponseDto> retrain();
}