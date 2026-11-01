package com.longvuong.plix.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

//Đọc claim từ access token: email hiển thị ở đầu màn Cài đặt, sub là mã người dùng (đã dùng ở nhiều nơi nên cũng kiểm tra lại sau khi dùng chung hàm đọc claim)
public class AuthManagerTest {

    //AuthManager giả: không gọi mạng, chỉ trả về token do test chỉ định
    private static class TokenAuthManager extends AuthManager {
        private final String token;

        TokenAuthManager(String token) {
            super(null);
            this.token = token;
        }

        @Override
        public String getAccessToken() {
            return token;
        }
    }

    //JWT giả đủ 3 phần header.payload.signature, payload mã hoá Base64 URL không có dấu '=' giống token thật
    private static String fakeJwt(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @Test
    public void getCurrentUserEmail_tokenHasEmailClaim_returnsEmail() {
        AuthManager authManager = new TokenAuthManager(fakeJwt("{\"sub\":\"user-1\",\"email\":\"long.nguyen@gmail.com\"}"));

        assertEquals("long.nguyen@gmail.com", authManager.getCurrentUserEmail());
    }

    @Test
    public void getCurrentUserEmail_tokenWithoutEmailClaim_returnsNull() {
        AuthManager authManager = new TokenAuthManager(fakeJwt("{\"sub\":\"user-1\"}"));

        assertNull(authManager.getCurrentUserEmail());
    }

    @Test
    public void getCurrentUserEmail_noToken_returnsNull() {
        AuthManager authManager = new TokenAuthManager(null);

        assertNull(authManager.getCurrentUserEmail());
    }

    @Test
    public void getCurrentUserEmail_malformedToken_returnsNull() {
        assertNull(new TokenAuthManager("not-a-jwt").getCurrentUserEmail());
        assertNull(new TokenAuthManager("header.***.signature").getCurrentUserEmail());
    }

    @Test
    public void getCurrentUserId_tokenHasSubClaim_returnsSub() {
        AuthManager authManager = new TokenAuthManager(fakeJwt("{\"sub\":\"user-1\",\"email\":\"long.nguyen@gmail.com\"}"));

        assertEquals("user-1", authManager.getCurrentUserId());
    }
}