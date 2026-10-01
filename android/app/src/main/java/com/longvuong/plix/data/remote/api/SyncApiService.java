package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionPushRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SyncApiService {
    @POST("api/v1/sync/transactions/push")
    Call<SyncPushResponseDto> pushTransactions(@Body TransactionPushRequestDto body);
}