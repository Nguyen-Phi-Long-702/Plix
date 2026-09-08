package com.longvuong.plix.core.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class EncryptedTokenStore {

    private static final String TAG = "EncryptedTokenStore";
    private static final String PREFS_FILE_NAME = "plix_secure_token_store";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private final Context appContext;
    private SharedPreferences encryptedPrefs;
    private boolean available;

    @Inject
    public EncryptedTokenStore(@ApplicationContext Context context) {
        this.appContext = context;
        this.encryptedPrefs = createEncryptedPrefs(false);
    }

    public boolean isAvailable() {
        return available;
    }

    public void saveTokens(String accessToken, String refreshToken) {
        if (!available) {
            return;
        }
        encryptedPrefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return available ? encryptedPrefs.getString(KEY_ACCESS_TOKEN, null) : null;
    }

    public String getRefreshToken() {
        return available ? encryptedPrefs.getString(KEY_REFRESH_TOKEN, null) : null;
    }

    public void clearTokens() {
        if (!available) {
            return;
        }
        encryptedPrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }

    private SharedPreferences createEncryptedPrefs(boolean isRetryAfterFailure) {
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            available = true;
            return prefs;
        } catch (GeneralSecurityException | IOException e) {
            if (isRetryAfterFailure) {
                Log.e(TAG, "Bộ nhớ bảo mật hệ thống gặp sự cố, không thể khởi tạo lại", e);
                available = false;
                return null;
            }
            Log.e(TAG, "Khoi tao EncryptedSharedPreferences that bai lan dau, thu xoa va tao lai", e);
            appContext.deleteSharedPreferences(PREFS_FILE_NAME);
            return createEncryptedPrefs(true);
        }
    }
}