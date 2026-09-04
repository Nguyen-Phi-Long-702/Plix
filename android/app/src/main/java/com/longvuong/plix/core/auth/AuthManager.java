package com.longvuong.plix.core.auth;

import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.data.remote.api.AuthApiService;
import com.longvuong.plix.data.remote.dto.AuthResponseDto;
import com.longvuong.plix.data.remote.dto.LoginRequestDto;
import com.longvuong.plix.data.remote.dto.SignupRequestDto;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthManager {

    public interface AuthCallback {
        void onSuccess(AuthResponseDto response);
        void onError(String message);
    }

    private final AuthApiService authApiService;

    public AuthManager() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new ApiKeyInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.authApiService = retrofit.create(AuthApiService.class);
    }

    public void register(String email, String password, AuthCallback callback) {
        authApiService.signup(new SignupRequestDto(email, password))
                .enqueue(new SimpleCallback(callback));
    }

    public void login(String email, String password, AuthCallback callback) {
        authApiService.login("password", new LoginRequestDto(email, password))
                .enqueue(new SimpleCallback(callback));
    }

    private static class SimpleCallback implements Callback<AuthResponseDto> {
        private final AuthCallback callback;

        SimpleCallback(AuthCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(Call<AuthResponseDto> call, retrofit2.Response<AuthResponseDto> response) {
            if (response.isSuccessful() && response.body() != null) {
                callback.onSuccess(response.body());
            } else {
                callback.onError("That bai (ma loi " + response.code() + ")");
            }
        }

        @Override
        public void onFailure(Call<AuthResponseDto> call, Throwable t) {
            callback.onError("Khong the ket noi den Supabase: " + t.getMessage());
        }
    }
    private static class ApiKeyInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request withApiKey = chain.request().newBuilder()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build();
            return chain.proceed(withApiKey);
        }
    }
}