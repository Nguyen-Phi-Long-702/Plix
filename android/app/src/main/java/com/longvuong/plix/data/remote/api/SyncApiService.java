package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionPullResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionPushRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SyncApiService {
    @POST("api/v1/sync/transactions/push")
    Call<SyncPushResponseDto> pushTransactions(@Body TransactionPushRequestDto body);

    @GET("api/v1/sync/transactions/pull")
    Call<TransactionPullResponseDto> pullTransactions(@Query("since") long since, @Query("limit") int limit);
}