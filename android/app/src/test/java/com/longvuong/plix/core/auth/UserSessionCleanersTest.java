package com.longvuong.plix.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UserSessionCleanersTest {

    //Bộ dọn ghi lại các userId nhận được (không dùng Mockito), ghi nhận được cả giá trị null
    private static class RecordingCleaner implements UserSessionCleaner {
        final List<String> receivedUserIds = new ArrayList<>();

        @Override
        public void clearForUser(@Nullable String userId) {
            receivedUserIds.add(userId);
        }
    }

    /**
     * TNX-U01
     * clearAll({cleanerA, cleanerB}, 'user-A'): mỗi bộ dọn nhận đúng 1 lần 'user-A'.
     */
    @Test
    public void clearAll_withTwoCleaners_callsEachOnceWithUserId() {
        RecordingCleaner cleanerA = new RecordingCleaner();
        RecordingCleaner cleanerB = new RecordingCleaner();
        Set<UserSessionCleaner> cleaners = new LinkedHashSet<>(Arrays.asList(cleanerA, cleanerB));

        UserSessionCleaners.clearAll(cleaners, "user-A");

        assertEquals(Arrays.asList("user-A"), cleanerA.receivedUserIds);
        assertEquals(Arrays.asList("user-A"), cleanerB.receivedUserIds);
    }

    /**
     * TNX-U02
     * cleanerA ném RuntimeException, cleanerB bình thường: cleanerB vẫn được gọi, clearAll không ném lỗi.
     */
    @Test
    public void clearAll_whenOneCleanerThrows_stillCallsOtherCleaners() {
        UserSessionCleaner cleanerA = userId -> {
            throw new RuntimeException("Lỗi giả lập khi dọn");
        };
        RecordingCleaner cleanerB = new RecordingCleaner();
        //LinkedHashSet giữ thứ tự thêm vào: cleanerA (ném lỗi) luôn chạy trước cleanerB
        Set<UserSessionCleaner> cleaners = new LinkedHashSet<>(Arrays.asList(cleanerA, cleanerB));

        UserSessionCleaners.clearAll(cleaners, "user-A");

        assertEquals(Arrays.asList("user-A"), cleanerB.receivedUserIds);
    }

    /**
     * TNX-U03
     * clearAll({cleanerA}, null): cleanerA nhận null.
     */
    @Test
    public void clearAll_withNullUserId_passesNullToCleaner() {
        RecordingCleaner cleanerA = new RecordingCleaner();
        Set<UserSessionCleaner> cleaners = new LinkedHashSet<>(Arrays.asList(cleanerA));

        UserSessionCleaners.clearAll(cleaners, null);

        assertEquals(1, cleanerA.receivedUserIds.size());
        assertNull(cleanerA.receivedUserIds.get(0));
    }
}