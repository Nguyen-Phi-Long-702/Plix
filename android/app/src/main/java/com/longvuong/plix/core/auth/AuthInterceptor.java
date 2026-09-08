package com.longvuong.plix.core.auth;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final AuthManager authManager;

    public AuthInterceptor(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String accessToken = authManager.getAccessToken();

        if (accessToken == null) {
            return chain.proceed(original);
        }

        Request withAuth = original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
        return chain.proceed(withAuth);
    }
}