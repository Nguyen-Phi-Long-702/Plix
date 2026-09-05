package com.longvuong.plix.core.auth;

import com.longvuong.plix.BuildConfig;
import com.longvuong.plix.data.remote.api.AuthApiService;
import com.longvuong.plix.data.remote.api.HealthApiService;
import com.longvuong.plix.data.remote.dto.AuthResponseDto;
import com.longvuong.plix.data.remote.dto.LoginRequestDto;
import com.longvuong.plix.data.remote.dto.RefreshRequestDto;
import com.longvuong.plix.data.remote.dto.SignupRequestDto;
import com.longvuong.plix.data.remote.dto.WhoamiResponseDto;

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

    public interface TestCallback {
        void onResult(String message);
    }

    private final AuthApiService authApiService;
    private final HealthApiService healthApiService;

    //Lưu tạm access/refresh token trong RAM
    private volatile String accessToken;
    private volatile String refreshToken;

    //Cờ đánh dấu phiên đăng nhập cần đăng nhập lại(refresh cũng thất bại).
    private volatile boolean sessionExpired = false;

    public AuthManager() {
        OkHttpClient supabaseClient = new OkHttpClient.Builder()
                .addInterceptor(new ApiKeyInterceptor())
                .build();

        Retrofit supabaseRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL)
                .client(supabaseClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.authApiService = supabaseRetrofit.create(AuthApiService.class);

        //Retrofit client tạm thời gọi backend của dự án
        OkHttpClient backendClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(this))
                .authenticator(new AuthAuthenticator(this))
                .build();

        Retrofit backendRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_BASE_URL)
                .client(backendClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.healthApiService = backendRetrofit.create(HealthApiService.class);
    }

    public void register(String email, String password, AuthCallback callback) {
        authApiService.signup(new SignupRequestDto(email, password))
                .enqueue(new SimpleCallback(callback));
    }

    public void login(String email, String password, AuthCallback callback) {
        authApiService.login("password", new LoginRequestDto(email, password))
                .enqueue(new SimpleCallback(callback));
    }
    public boolean refreshSync() {
        String currentRefreshToken = this.refreshToken;
        if (currentRefreshToken == null) {
            return false;
        }
        try {
            retrofit2.Response<AuthResponseDto> response = authApiService
                    .refresh("refresh_token", new RefreshRequestDto(currentRefreshToken))
                    .execute();
            if (response.isSuccessful() && response.body() != null) {
                saveSession(response.body());
                return true;
            }
            return false;
        } catch (IOException e) {
            //Lỗi mạng khi refresh thì coi như thất bại, để AuthAuthenticator xử lý tiếp
            return false;
        }
    }

    public void testBackendAuth(TestCallback callback) {
        healthApiService.health().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> healthResponse) {
                if (!healthResponse.isSuccessful()) {
                    callback.onResult("health thất bại (mã lỗi " + healthResponse.code() + ")");
                    return;
                }
                healthApiService.whoami().enqueue(new Callback<WhoamiResponseDto>() {
                    @Override
                    public void onResponse(Call<WhoamiResponseDto> call, retrofit2.Response<WhoamiResponseDto> whoamiResponse) {
                        if (whoamiResponse.isSuccessful() && whoamiResponse.body() != null) {
                            callback.onResult("health ok. whoami ok, user_id = " + whoamiResponse.body().userId);
                        } else {
                            callback.onResult("health ok. whoami thất bại(mã lỗi " + whoamiResponse.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<WhoamiResponseDto> call, Throwable t) {
                        callback.onResult("health ok. Không gọi được whoami: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult("Không gọi được health: " + t.getMessage());
            }
        });
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isSessionExpired() {
        return sessionExpired;
    }

    public void notifySessionExpired() {
        this.sessionExpired = true;
    }

    private void saveSession(AuthResponseDto response) {
        this.accessToken = response.accessToken;
        this.refreshToken = response.refreshToken;
        this.sessionExpired = false;
    }

    private class SimpleCallback implements Callback<AuthResponseDto> {
        private final AuthCallback callback;

        SimpleCallback(AuthCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(Call<AuthResponseDto> call, retrofit2.Response<AuthResponseDto> response) {
            if (response.isSuccessful() && response.body() != null) {
                saveSession(response.body());
                callback.onSuccess(response.body());
            } else {
                callback.onError("Thất bại(mã lỗi " + response.code() + ")");
            }
        }

        @Override
        public void onFailure(Call<AuthResponseDto> call, Throwable t) {
            callback.onError("không thể kết nối đến supabase: " + t.getMessage());
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