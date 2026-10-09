package com.longvuong.plix.data.remote.api;

import com.longvuong.plix.data.remote.dto.BudgetSyncRecordDto;
import com.longvuong.plix.data.remote.dto.CategorySyncRecordDto;
import com.longvuong.plix.data.remote.dto.CorrectionSyncRecordDto;
import com.longvuong.plix.data.remote.dto.GoalSyncRecordDto;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushRequestDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SyncApiService {
    @POST("api/v1/sync/transactions/push")
    Call<SyncPushResponseDto> pushTransactions(@Body SyncPushRequestDto<TransactionSyncRecordDto> body);

    @GET("api/v1/sync/transactions/pull")
    Call<SyncPullResponseDto<TransactionSyncRecordDto>> pullTransactions(@Query("since") long since, @Query("limit") int limit);

    @POST("api/v1/sync/categories/push")
    Call<SyncPushResponseDto> pushCategories(@Body SyncPushRequestDto<CategorySyncRecordDto> body);

    @GET("api/v1/sync/categories/pull")
    Call<SyncPullResponseDto<CategorySyncRecordDto>> pullCategories(@Query("since") long since, @Query("limit") int limit);

    @POST("api/v1/sync/budgets/push")
    Call<SyncPushResponseDto> pushBudgets(@Body SyncPushRequestDto<BudgetSyncRecordDto> body);

    @GET("api/v1/sync/budgets/pull")
    Call<SyncPullResponseDto<BudgetSyncRecordDto>> pullBudgets(@Query("since") long since, @Query("limit") int limit);

    @POST("api/v1/sync/goals/push")
    Call<SyncPushResponseDto> pushGoals(@Body SyncPushRequestDto<GoalSyncRecordDto> body);

    @GET("api/v1/sync/goals/pull")
    Call<SyncPullResponseDto<GoalSyncRecordDto>> pullGoals(@Query("since") long since, @Query("limit") int limit);

    @POST("api/v1/sync/corrections/push")
    Call<SyncPushResponseDto> pushCorrections(@Body SyncPushRequestDto<CorrectionSyncRecordDto> body);

    @GET("api/v1/sync/corrections/pull")
    Call<SyncPullResponseDto<CorrectionSyncRecordDto>> pullCorrections(@Query("since") long since, @Query("limit") int limit);
}