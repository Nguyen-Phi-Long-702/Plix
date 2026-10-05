package com.longvuong.plix.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

//Token hết hạn giữa lúc đồng bộ: Authenticator phải làm mới token đúng 1 lần dù nhiều yêu cầu cùng nhận 401
public class AuthAuthenticatorTest {
    private static final String OLD_TOKEN = "old-token";
    private static final String NEW_TOKEN = "new-token";

    //AuthManager giả: không gọi mạng, chỉ đếm số lần làm mới token
    //refreshSync ngủ 300ms để các luồng còn lại kịp xếp hàng chờ khoá, nếu Authenticator làm sai thì sẽ đếm được nhiều hơn 1 lần
    private static class FakeAuthManager extends AuthManager {
        final AtomicInteger refreshCount = new AtomicInteger();
        final AtomicInteger sessionExpiredCount = new AtomicInteger();
        private volatile String currentToken;

        FakeAuthManager(String initialToken) {
            super(null);
            this.currentToken = initialToken;
        }

        @Override
        public String getAccessToken() {
            return currentToken;
        }

        @Override
        public boolean refreshSync() {
            refreshCount.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            currentToken = NEW_TOKEN;
            return true;
        }

        @Override
        public void notifySessionExpired() {
            sessionExpiredCount.incrementAndGet();
        }
    }

    //Phản hồi 401 của một yêu cầu đã gửi đi kèm token cũ
    private static Response unauthorizedResponseWithToken(String token) {
        Request request = new Request.Builder()
                .url("http://localhost/api/v1/sync/transactions/push")
                .header("Authorization", "Bearer " + token)
                .build();
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build();
    }

    @Test
    public void authenticate_expiredToken_refreshesOnceAndRetriesWithNewToken() throws Exception {
        FakeAuthManager authManager = new FakeAuthManager(OLD_TOKEN);
        AuthAuthenticator authenticator = new AuthAuthenticator(authManager);

        Request retried = authenticator.authenticate(null, unauthorizedResponseWithToken(OLD_TOKEN));

        assertNotNull(retried);
        assertEquals("Bearer " + NEW_TOKEN, retried.header("Authorization"));
        assertEquals(1, authManager.refreshCount.get());
        assertEquals(0, authManager.sessionExpiredCount.get());
    }

    @Test(timeout = 10000)
    public void authenticate_manyConcurrent401_refreshesExactlyOnce() throws Exception {
        final int requestCount = 5; //giả lập SyncWorker đẩy nhiều bảng cùng lúc, tất cả cùng nhận 401
        FakeAuthManager authManager = new FakeAuthManager(OLD_TOKEN);
        AuthAuthenticator authenticator = new AuthAuthenticator(authManager);

        CountDownLatch allReady = new CountDownLatch(requestCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(requestCount);
        List<Request> retriedRequests = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < requestCount; i++) {
            new Thread(() -> {
                try {
                    Response response = unauthorizedResponseWithToken(OLD_TOKEN);
                    allReady.countDown();
                    go.await(); //tất cả luồng xuất phát cùng một lúc
                    retriedRequests.add(authenticator.authenticate(null, response));
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    allDone.countDown();
                }
            }).start();
        }

        assertTrue(allReady.await(5, TimeUnit.SECONDS));
        go.countDown();
        assertTrue(allDone.await(5, TimeUnit.SECONDS));

        assertTrue("Có luồng bị lỗi: " + errors, errors.isEmpty());
        assertEquals(requestCount, retriedRequests.size());
        for (Request retried : retriedRequests) {
            assertNotNull(retried);
            assertEquals("Bearer " + NEW_TOKEN, retried.header("Authorization"));
        }
        assertEquals("Chỉ được làm mới token đúng 1 lần", 1, authManager.refreshCount.get());
        assertEquals(0, authManager.sessionExpiredCount.get());
    }
}