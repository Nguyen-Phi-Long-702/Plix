package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//Kéo dữ liệu với Room thật: 1 lô có bản ghi hỏng thì cả lô không được ghi vào Room và con trỏ giữ nguyên (all-or-nothing)
@RunWith(AndroidJUnit4.class)
public class SyncPullerRoomTest {
    private static final String USER_ID = "user-kiem-thu-ngay-36"; //userId riêng để không đụng tới con trỏ thật của app
    private static final String TABLE_NAME = "transactions";

    private AppDatabase database;
    private SyncPreferences syncPreferences;
    private SyncPuller syncPuller;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        syncPreferences = new SyncPreferences(context);
        syncPreferences.clearPullCursors(USER_ID);
        syncPuller = new SyncPuller(database, syncPreferences);
    }

    @After
    public void tearDown() {
        syncPreferences.clearPullCursors(USER_ID);
        database.close();
    }

    private TransactionSyncRecordDto record(String id, long updatedAt, String type) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = USER_ID;
        entity.amount = 1000L;
        entity.type = type;
        entity.occurredAt = 1L;
        entity.updatedAt = updatedAt;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return TransactionSyncRecordDto.fromEntity(entity);
    }

    //Bảng giao dịch ghi vào Room thật, còn "máy chủ" trả sẵn 1 lô bản ghi
    private TransactionSyncableEntity tableReturning(List<TransactionSyncRecordDto> records) {
        return new TransactionSyncableEntity(database.transactionDao(), null) {
            @Override
            public Call<SyncPullResponseDto<TransactionSyncRecordDto>> pull(long since, int limit) {
                return new FakePullCall(new SyncPullResponseDto<>(records, false));
            }
        };
    }

    @Test
    public void pull_validBatch_savesAllRecordsAndAdvancesCursorTogether() throws IOException {
        TransactionSyncableEntity table = tableReturning(Arrays.asList(
                record("t1", 1000L, "expense"),
                record("t2", 2000L, "income")));

        syncPuller.pull(table, USER_ID, () -> false);

        assertNotNull(database.transactionDao().getById("t1"));
        assertNotNull(database.transactionDao().getById("t2"));
        assertEquals(2000L, syncPreferences.getPullCursor(USER_ID, TABLE_NAME));
    }

    @Test
    public void pull_batchWithCorruptRecord_writesNothingAndKeepsCursor() {
        //Bản ghi thứ 2 thiếu type (cột bắt buộc) nên ghi vào Room sẽ lỗi sau khi bản ghi thứ 1 đã được ghi
        TransactionSyncableEntity table = tableReturning(Arrays.asList(
                record("t1", 1000L, "expense"),
                record("t2", 2000L, null)));

        assertThrows(RuntimeException.class, () -> syncPuller.pull(table, USER_ID, () -> false));

        assertNull(database.transactionDao().getById("t1")); //bản ghi thứ 1 phải được hoàn tác
        assertNull(database.transactionDao().getById("t2"));
        assertEquals(0L, syncPreferences.getPullCursor(USER_ID, TABLE_NAME));
    }

    //Call giả: trả sẵn kết quả, không gọi mạng
    private static class FakePullCall implements Call<SyncPullResponseDto<TransactionSyncRecordDto>> {
        private final SyncPullResponseDto<TransactionSyncRecordDto> body;

        FakePullCall(SyncPullResponseDto<TransactionSyncRecordDto> body) {
            this.body = body;
        }

        @Override
        public Response<SyncPullResponseDto<TransactionSyncRecordDto>> execute() {
            return Response.success(body);
        }

        @Override
        public void enqueue(Callback<SyncPullResponseDto<TransactionSyncRecordDto>> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Call<SyncPullResponseDto<TransactionSyncRecordDto>> clone() {
            return this;
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}