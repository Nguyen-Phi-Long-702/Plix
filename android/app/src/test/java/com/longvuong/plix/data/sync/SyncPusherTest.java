package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//Mất mạng giữa lúc đẩy: lô đã thành công giữ synced, phần còn lại giữ pending; lần chạy sau (WorkManager retry) đẩy nốt phần còn lại
public class SyncPusherTest {
    private static final String USER_ID = "user-1";

    private FakeTransactionDao fakeTransactionDao;
    private final SyncPusher syncPusher = new SyncPusher();

    @Before
    public void setUp() {
        fakeTransactionDao = new FakeTransactionDao();
    }

    private void insertPendingTransactions(int count) {
        for (int i = 0; i < count; i++) {
            TransactionEntity entity = new TransactionEntity();
            entity.id = "t" + i;
            entity.userId = USER_ID;
            entity.amount = 1000L;
            entity.type = "expense";
            entity.occurredAt = 1L;
            entity.updatedAt = 1000L + i;
            entity.syncStatus = "pending";
            entity.isDeleted = false;
            fakeTransactionDao.insert(entity);
        }
    }

    //failingCall = lần gọi push thứ mấy sẽ bị mất mạng (0 = không bao giờ mất mạng)
    private TransactionSyncableEntity tableFailingOnCall(int failingCall) {
        final int[] calls = {0};
        return new TransactionSyncableEntity(fakeTransactionDao, null) {
            @Override
            public Call<SyncPushResponseDto> push(List<TransactionSyncRecordDto> records) {
                calls[0]++;
                if (calls[0] == failingCall) {
                    return new FakeCall(null, new IOException("mất mạng giữa chừng"));
                }
                return new FakeCall(successFor(records), null);
            }
        };
    }

    private SyncPushResponseDto successFor(List<TransactionSyncRecordDto> records) {
        SyncPushResponseDto body = new SyncPushResponseDto();
        body.upsertedIds = new ArrayList<>();
        body.rejected = new ArrayList<>();
        for (TransactionSyncRecordDto record : records) {
            body.upsertedIds.add(record.id);
        }
        return body;
    }

    @Test
    public void push_networkLostOnSecondBatch_firstBatchSyncedRestStayPending() {
        insertPendingTransactions(SyncPusher.BATCH_SIZE + 5);
        TransactionSyncableEntity table = tableFailingOnCall(2);

        assertThrows(IOException.class, () -> syncPusher.push(table, USER_ID, () -> false));

        assertEquals(5, fakeTransactionDao.getPendingSync(USER_ID).size());
    }

    @Test
    public void push_afterNetworkReturns_remainingPendingRecordsGetSynced() throws IOException {
        insertPendingTransactions(SyncPusher.BATCH_SIZE + 5);
        assertThrows(IOException.class, () -> syncPusher.push(tableFailingOnCall(2), USER_ID, () -> false));

        syncPusher.push(tableFailingOnCall(0), USER_ID, () -> false);

        assertEquals(0, fakeTransactionDao.getPendingSync(USER_ID).size());
    }
    //Ghi lại id của từng lô được đẩy lên, để kiểm tra kích thước lô và không id nào bị gửi trùng hoặc bị bỏ sót
    private TransactionSyncableEntity recordingTable(List<List<String>> sentBatches) {
        return new TransactionSyncableEntity(fakeTransactionDao, null) {
            @Override
            public Call<SyncPushResponseDto> push(List<TransactionSyncRecordDto> records) {
                List<String> ids = new ArrayList<>();
                for (TransactionSyncRecordDto record : records) {
                    ids.add(record.id);
                }
                sentBatches.add(ids);
                return new FakeCall(successFor(records), null);
            }
        };
    }

    private static Set<String> distinctIds(List<List<String>> batches) {
        Set<String> ids = new HashSet<>();
        for (List<String> batch : batches) {
            ids.addAll(batch);
        }
        return ids;
    }

    @Test
    public void push_120PendingRecords_sendsBatchesOf50And50And20_eachIdExactlyOnce() throws IOException {
        insertPendingTransactions(120);
        List<List<String>> sentBatches = new ArrayList<>();

        syncPusher.push(recordingTable(sentBatches), USER_ID, () -> false);

        assertEquals(3, sentBatches.size());
        assertEquals(50, sentBatches.get(0).size());
        assertEquals(50, sentBatches.get(1).size());
        assertEquals(20, sentBatches.get(2).size());
        assertEquals(120, distinctIds(sentBatches).size()); //tổng 120 lượt gửi mà có đúng 120 id khác nhau: không id nào gửi trùng, không id nào bị bỏ sót
        assertEquals(0, fakeTransactionDao.getPendingSync(USER_ID).size());

        List<List<String>> secondRunBatches = new ArrayList<>();
        syncPusher.push(recordingTable(secondRunBatches), USER_ID, () -> false);
        assertEquals(0, secondRunBatches.size()); //lượt đồng bộ sau không gửi lại bản ghi nào
    }

    @Test
    public void push_exactly100PendingRecords_sendsTwoFullBatchesAndNoEmptyBatch() throws IOException {
        insertPendingTransactions(100);
        List<List<String>> sentBatches = new ArrayList<>();

        syncPusher.push(recordingTable(sentBatches), USER_ID, () -> false);

        assertEquals(2, sentBatches.size());
        assertEquals(50, sentBatches.get(0).size());
        assertEquals(50, sentBatches.get(1).size());
        assertEquals(0, fakeTransactionDao.getPendingSync(USER_ID).size());
    }

    @Test
    public void push_120PendingNetworkLostOnSecondBatch_retryOnlySendsTheRemaining70() throws IOException {
        insertPendingTransactions(120);
        assertThrows(IOException.class, () -> syncPusher.push(tableFailingOnCall(2), USER_ID, () -> false));
        Set<String> expectedRetryIds = new HashSet<>();
        for (TransactionEntity entity : fakeTransactionDao.getPendingSync(USER_ID)) {
            expectedRetryIds.add(entity.id);
        }
        assertEquals(70, expectedRetryIds.size()); //lô đầu 50 bản ghi đã synced, còn 70 bản ghi pending

        List<List<String>> retryBatches = new ArrayList<>();
        syncPusher.push(recordingTable(retryBatches), USER_ID, () -> false);

        assertEquals(2, retryBatches.size());
        assertEquals(50, retryBatches.get(0).size());
        assertEquals(20, retryBatches.get(1).size());
        assertEquals(expectedRetryIds, distinctIds(retryBatches)); //chỉ gửi đúng 70 bản ghi còn lại, không gửi lại 50 bản ghi đã synced
        assertEquals(0, fakeTransactionDao.getPendingSync(USER_ID).size());
    }
    //Call giả: trả sẵn kết quả, hoặc ném IOException như khi mất mạng
    private static class FakeCall implements Call<SyncPushResponseDto> {
        private final SyncPushResponseDto body;
        private final IOException failure;

        FakeCall(SyncPushResponseDto body, IOException failure) {
            this.body = body;
            this.failure = failure;
        }

        @Override
        public Response<SyncPushResponseDto> execute() throws IOException {
            if (failure != null) {
                throw failure;
            }
            return Response.success(body);
        }

        @Override
        public void enqueue(Callback<SyncPushResponseDto> callback) {
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
        public Call<SyncPushResponseDto> clone() {
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