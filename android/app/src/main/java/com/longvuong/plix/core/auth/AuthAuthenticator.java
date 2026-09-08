package com.longvuong.plix.core.auth;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class AuthAuthenticator implements Authenticator {

    private static final Object REFRESH_LOCK = new Object();
    private static final int MAX_RESPONSE_COUNT = 2; //chỉ cho phép retry 1 lần

    private final AuthManager authManager;

    public AuthAuthenticator(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= MAX_RESPONSE_COUNT) {
            authManager.notifySessionExpired();
            return null;
        }

        synchronized (REFRESH_LOCK) {
            String tokenUsedInFailedRequest = response.request().header("Authorization");
            String currentAccessToken = authManager.getAccessToken();
            String currentAuthHeader = currentAccessToken != null ? "Bearer " + currentAccessToken : null;

            boolean tokenAlreadyRefreshedByAnotherThread =
                    currentAuthHeader != null && !currentAuthHeader.equals(tokenUsedInFailedRequest);

            if (!tokenAlreadyRefreshedByAnotherThread) {
                boolean refreshed = authManager.refreshSync();
                if (!refreshed) {
                    authManager.notifySessionExpired();
                    return null;
                }
                currentAccessToken = authManager.getAccessToken();
            }

            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + currentAccessToken)
                    .build();
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            count++;
            prior = prior.priorResponse();
        }
        return count;
    }
}