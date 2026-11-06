package com.longvuong.plix.data.remote.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

public interface ExportApiService {

    //Trả về nguyên file CSV; @Streaming để không đọc cả file vào bộ nhớ một lần
    @Streaming
    @GET("api/v1/export/csv")
    Call<ResponseBody> exportCsv();
}