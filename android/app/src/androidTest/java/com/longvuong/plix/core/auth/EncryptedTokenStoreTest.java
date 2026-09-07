package com.longvuong.plix.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EncryptedTokenStoreTest {

    @Test
    public void saveAndReadTokens_returnsSameValues() {
        Context context = ApplicationProvider.getApplicationContext();
        EncryptedTokenStore store = new EncryptedTokenStore(context);

        assertTrue("EncryptedTokenStore phai khoi tao thanh cong tren may ao test", store.isAvailable());

        store.saveTokens("access-123", "refresh-456");

        assertEquals("access-123", store.getAccessToken());
        assertEquals("refresh-456", store.getRefreshToken());
    }

    @Test
    public void clearTokens_removesBothTokens() {
        Context context = ApplicationProvider.getApplicationContext();
        EncryptedTokenStore store = new EncryptedTokenStore(context);

        store.saveTokens("access-abc", "refresh-def");
        store.clearTokens();

        assertNull(store.getAccessToken());
        assertNull(store.getRefreshToken());
    }
}