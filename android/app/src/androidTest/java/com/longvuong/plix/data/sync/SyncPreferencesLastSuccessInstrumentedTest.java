package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SyncPreferencesLastSuccessInstrumentedTest {
    private static final String USER_A = "user-A";
    private static final String USER_B = "user-B";
    private static final String TABLE = "transactions";
    private static final long SYNC_AT_MILLIS = 1794121500000L; //14:05 08/11/2026 giờ Việt Nam
    private static final String ERROR_MESSAGE = "Máy chủ phản hồi lỗi (mã 503)";

    private SyncPreferences syncPreferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        syncPreferences = new SyncPreferences(context);
        cleanUp(); //bắt đầu từ trạng thái sạch, không phụ thuộc lần chạy trước
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        syncPreferences.clearLastSuccessAt(USER_A);
        syncPreferences.clearLastSuccessAt(USER_B);
        syncPreferences.clearPullCursors(USER_A);
        syncPreferences.clearLastError();
    }

    /**
     * TN5-I01
     * setLastSuccessAt('user-A', 1794121500000); getLastSuccessAt('user-B'); clearLastSuccessAt('user-A'):
     * user-B trả 0 (chưa có); user-A trả 1794121500000 trước khi xoá và 0 sau khi xoá;
     * clearPullCursors và clearLastError hiện có không bị ảnh hưởng.
     */
    @Test
    public void lastSuccessAt_isPerUser_andIndependentOfPullCursorsAndLastError() {
        syncPreferences.setPullCursor(USER_A, TABLE, 42L);
        syncPreferences.saveLastError(ERROR_MESSAGE);
        syncPreferences.setLastSuccessAt(USER_A, SYNC_AT_MILLIS);

        //Theo từng người dùng: user-B chưa có thì trả 0
        assertEquals(0L, syncPreferences.getLastSuccessAt(USER_B));
        assertEquals(SYNC_AT_MILLIS, syncPreferences.getLastSuccessAt(USER_A));

        //clearPullCursors và clearLastError không làm mất mốc đồng bộ thành công
        syncPreferences.clearPullCursors(USER_A);
        syncPreferences.clearLastError();
        assertEquals(SYNC_AT_MILLIS, syncPreferences.getLastSuccessAt(USER_A));
        assertEquals(0L, syncPreferences.getPullCursor(USER_A, TABLE));
        assertNull(syncPreferences.getLastError());

        //clearLastSuccessAt chỉ xoá mốc của user-A, không đụng con trỏ và lỗi cuối
        syncPreferences.setPullCursor(USER_A, TABLE, 42L);
        syncPreferences.saveLastError(ERROR_MESSAGE);
        syncPreferences.clearLastSuccessAt(USER_A);
        assertEquals(0L, syncPreferences.getLastSuccessAt(USER_A));
        assertEquals(42L, syncPreferences.getPullCursor(USER_A, TABLE));
        assertEquals(ERROR_MESSAGE, syncPreferences.getLastError());
    }
}