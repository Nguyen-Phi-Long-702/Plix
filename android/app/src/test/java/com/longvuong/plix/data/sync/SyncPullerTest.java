package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import okhttp3.MediaType;
import okhttp3.ResponseBody;

import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;
import retrofit2.HttpException;

public class SyncPullerTest {
    private static final String USER_ID = "user-1";
    private static final long FIRST_UPDATED_AT = 1000L;

    private FakeTransactionDao fakeTransactionDao;
    private long savedCursor;
    private int applyCount;
    private TransactionSyncableEntity table;
    private SyncPuller.BatchApplier<TransactionSyncRecordDto> recordingApplier;

    @Before
    public void setUp() {
        fakeTransactionDao = new FakeTransactionDao();
        table = new TransactionSyncableEntity(fakeTransactionDao, null); //không gọi api nên không cần SyncApiService
        savedCursor = 0L;
        applyCount = 0;
        //Ghi bản ghi vào Room rồi mới lưu con trỏ
        recordingApplier = (records, newCursor) -> {
            SyncPuller.upsertBatch(table, records, USER_ID);
            savedCursor = newCursor;
            applyCount++;
        };
    }

    private TransactionEntity entity(String id, long updatedAt, String syncStatus, long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = USER_ID;
        entity.amount = amount;
        entity.type = "expense";
        entity.categoryId = "sys_an_uong";
        entity.note = "ghi chú " + id;
        entity.occurredAt = 1L;
        entity.updatedAt = updatedAt;
        entity.syncStatus = syncStatus;
        entity.isDeleted = false;
        return entity;
    }

    private TransactionSyncRecordDto record(String id, long updatedAt, long amount, boolean isDeleted) {
        TransactionEntity entity = entity(id, updatedAt, "synced", amount);
        entity.isDeleted = isDeleted;
        return TransactionSyncRecordDto.fromEntity(entity);
    }

    private List<TransactionSyncRecordDto> distinctRecords(int count) {
        List<TransactionSyncRecordDto> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(record("t" + i, FIRST_UPDATED_AT + i, 1000L, false));
        }
        return records;
    }

    private Response<SyncPullResponseDto<TransactionSyncRecordDto>> page(List<TransactionSyncRecordDto> records, boolean hasMore) {
        return Response.success(new SyncPullResponseDto<>(records, hasMore));
    }

    //Máy chủ trả mã lỗi HTTP (ví dụ 500, 503)
    private Response<SyncPullResponseDto<TransactionSyncRecordDto>> httpError(int code) {
        return Response.error(code, ResponseBody.create("", MediaType.get("application/json")));
    }

    //Máy chủ giả: dữ liệu đã sắp tăng dần theo updated_at, lọc >= since, cắt theo limit, has_more = còn bản ghi có updated_at > max(updated_at của lô này)
    private static class FakePullServer implements SyncPuller.PageFetcher<TransactionSyncRecordDto> {
        private final List<TransactionSyncRecordDto> data;
        final List<Long> sinceValues = new ArrayList<>();

        FakePullServer(List<TransactionSyncRecordDto> data) {
            this.data = data;
        }

        @Override
        public Response<SyncPullResponseDto<TransactionSyncRecordDto>> fetch(long since, int limit) {
            sinceValues.add(since);
            List<TransactionSyncRecordDto> matching = new ArrayList<>();
            for (TransactionSyncRecordDto record : data) {
                if (record.updatedAt >= since) {
                    matching.add(record);
                }
            }
            List<TransactionSyncRecordDto> page = new ArrayList<>(matching.subList(0, Math.min(limit, matching.size())));
            long pageMax = since;
            for (TransactionSyncRecordDto record : page) {
                pageMax = Math.max(pageMax, record.updatedAt);
            }
            boolean hasMore = false;
            for (TransactionSyncRecordDto record : matching) {
                if (record.updatedAt > pageMax) {
                    hasMore = true;
                }
            }
            return Response.success(new SyncPullResponseDto<>(page, hasMore));
        }
    }


    @Test
    public void pullAll_emptyFirstResponse_appliesNothingAndKeepsCursor() throws IOException {
        SyncPuller.PageFetcher<TransactionSyncRecordDto> fetcher = (since, limit) -> page(Collections.<TransactionSyncRecordDto>emptyList(), false);
        SyncPuller.pullAll(table, fetcher, 0L, () -> false, recordingApplier);
        assertEquals(0, applyCount);
        assertEquals(0L, savedCursor);
    }

    @Test
    public void pullAll_singleBatchWithoutMore_appliesOnceAndSavesMaxUpdatedAt() throws IOException {
        FakePullServer server = new FakePullServer(distinctRecords(3));
        SyncPuller.pullAll(table, server, 0L, () -> false, recordingApplier);
        assertEquals(1, server.sinceValues.size());
        assertEquals(1, applyCount);
        assertEquals(FIRST_UPDATED_AT + 2, savedCursor);
        assertEquals(3, fakeTransactionDao.getAllOnce().size());
    }

    @Test
    public void pullAll_moreThan500Records_loopsWithNewCursorUntilAllPulled() throws IOException {
        FakePullServer server = new FakePullServer(distinctRecords(1200));
        SyncPuller.pullAll(table, server, 0L, () -> false, recordingApplier);
        //lần 1 since=0, lần 2 since=max của lô 1 (1000+499), lần 3 since=max của lô 2 (1000+998)
        assertEquals(Arrays.asList(0L, 1499L, 1998L), server.sinceValues);
        assertEquals(3, applyCount);
        assertEquals(FIRST_UPDATED_AT + 1199, savedCursor);
        assertEquals(1200, fakeTransactionDao.getAllOnce().size());
    }

    @Test
    public void pullAll_501RecordsWithSameUpdatedAt_stopsWhenSameBatchReturnedTwice() throws IOException {
        List<TransactionSyncRecordDto> sameTimeRecords = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            sameTimeRecords.add(record("t" + i, 5000L, 1000L, false));
        }
        final int[] fetchCount = {0};
        //Máy chủ lỗi: luôn trả đúng 500 bản ghi đầu và has_more=true -> nếu không có safety guard sẽ lặp vô hạn
        SyncPuller.PageFetcher<TransactionSyncRecordDto> stuckServer = (since, limit) -> {
            fetchCount[0]++;
            if (fetchCount[0] > 10) {
                throw new AssertionError("Vòng lặp không dừng: safety guard không hoạt động");
            }
            return page(new ArrayList<>(sameTimeRecords.subList(0, limit)), true);
        };
        SyncPuller.pullAll(table, stuckServer, 0L, () -> false, recordingApplier);
        assertEquals(2, fetchCount[0]); //lần 2 phát hiện trùng lô lần 1 thì dừng
        assertEquals(1, applyCount); //lô trùng không ghi lại
        assertEquals(5000L, savedCursor);
        assertEquals(500, fakeTransactionDao.getAllOnce().size());
    }

    @Test
    public void pullAll_networkErrorOnSecondCall_keepsCursorOfFirstBatch() {
        List<TransactionSyncRecordDto> firstPage = distinctRecords(3);
        final int[] fetchCount = {0};
        SyncPuller.PageFetcher<TransactionSyncRecordDto> flakyServer = (since, limit) -> {
            fetchCount[0]++;
            if (fetchCount[0] == 1) {
                return page(firstPage, true);
            }
            throw new IOException("mất mạng giữa chừng");
        };
        assertThrows(IOException.class, () -> SyncPuller.pullAll(table, flakyServer, 0L, () -> false, recordingApplier));
        assertEquals(1, applyCount);
        assertEquals(FIRST_UPDATED_AT + 2, savedCursor); //giữ nguyên ở lần thành công gần nhất
    }

    @Test
    public void pullAll_applyFailsOnSecondBatch_cursorStaysAtFirstBatch() {
        FakePullServer server = new FakePullServer(distinctRecords(1200));
        final int[] applyCalls = {0};
        SyncPuller.BatchApplier<TransactionSyncRecordDto> failingOnSecond = (records, newCursor) -> {
            applyCalls[0]++;
            if (applyCalls[0] == 2) {
                throw new IllegalStateException("ghi Room thất bại");
            }
            recordingApplier.apply(records, newCursor);
        };
        assertThrows(IllegalStateException.class, () -> SyncPuller.pullAll(table, server, 0L, () -> false, failingOnSecond));
        assertEquals(1499L, savedCursor); //con trỏ lô 1, không tiến lên lô 2
    }

    @Test
    public void pullAll_serverErrorOnFirstCall_neverAppliesAndKeepsExistingCursor() {
        savedCursor = 7000L; //con trỏ đã có từ các lần kéo trước
        SyncPuller.PageFetcher<TransactionSyncRecordDto> failingServer = (since, limit) -> httpError(500);
        assertThrows(HttpException.class, () -> SyncPuller.pullAll(table, failingServer, 7000L, () -> false, recordingApplier));
        assertEquals(0, applyCount);
        assertEquals(7000L, savedCursor);
        assertTrue(fakeTransactionDao.getAllOnce().isEmpty());
    }

    @Test
    public void pullAll_serverErrorOnSecondCall_keepsCursorOfFirstBatch() {
        List<TransactionSyncRecordDto> firstPage = distinctRecords(3);
        final int[] fetchCount = {0};
        SyncPuller.PageFetcher<TransactionSyncRecordDto> flakyServer = (since, limit) -> {
            fetchCount[0]++;
            if (fetchCount[0] == 1) {
                return page(firstPage, true);
            }
            return httpError(503);
        };
        assertThrows(HttpException.class, () -> SyncPuller.pullAll(table, flakyServer, 0L, () -> false, recordingApplier));
        assertEquals(1, applyCount);
        assertEquals(FIRST_UPDATED_AT + 2, savedCursor); //giữ nguyên ở lô thành công gần nhất
    }

    @Test
    public void pullAll_freshInstallReceivesTombstone_storesItAsDeletedAndKeepsItHidden() throws IOException {
        FakePullServer server = new FakePullServer(Arrays.asList(
                record("t1", 2000L, 500L, false),
                record("t2", 3000L, 700L, true)));
        SyncPuller.pullAll(table, server, 0L, () -> false, recordingApplier); //cài lại app: Room trống, con trỏ = 0
        assertTrue(fakeTransactionDao.getById("t2").isDeleted);
        assertEquals(1, fakeTransactionDao.getAllOnce().size());
        assertEquals("t1", fakeTransactionDao.getAllOnce().get(0).id);
        assertEquals(3000L, savedCursor);
    }

    @Test
    public void pullAll_exactly500Records_stopsAfterOneCallWhenServerSaysNoMore() throws IOException {
        FakePullServer server = new FakePullServer(distinctRecords(500));
        SyncPuller.pullAll(table, server, 0L, () -> false, recordingApplier);
        assertEquals(Collections.singletonList(0L), server.sinceValues);
        assertEquals(1, applyCount);
        assertEquals(500, fakeTransactionDao.getAllOnce().size());
        assertEquals(FIRST_UPDATED_AT + 499, savedCursor);
    }

    @Test
    public void pullAll_shouldStopAlreadyTrue_doesNotCallApi() throws IOException {
        FakePullServer server = new FakePullServer(distinctRecords(3));
        SyncPuller.pullAll(table, server, 0L, () -> true, recordingApplier);
        assertTrue(server.sinceValues.isEmpty());
        assertEquals(0, applyCount);
    }

    @Test
    public void upsertBatch_unknownRecord_insertsAsSyncedForCurrentUser() {
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 2000L, 500L, false)), USER_ID);
        TransactionEntity saved = fakeTransactionDao.getById("t1");
        assertNotNull(saved);
        assertEquals(USER_ID, saved.userId);
        assertEquals("synced", saved.syncStatus);
        assertEquals(500L, saved.amount);
        assertEquals(2000L, saved.updatedAt);
    }

    @Test
    public void upsertBatch_incomingNewer_overwritesExisting() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 3000L, 900L, false)), USER_ID);
        TransactionEntity saved = fakeTransactionDao.getById("t1");
        assertEquals(900L, saved.amount);
        assertEquals(3000L, saved.updatedAt);
    }

    @Test
    public void upsertBatch_sameUpdatedAt_keepsExisting() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 2000L, 900L, false)), USER_ID);
        assertEquals(500L, fakeTransactionDao.getById("t1").amount);
    }

    @Test
    public void upsertBatch_incomingOlder_keepsExisting() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 1500L, 900L, false)), USER_ID);
        assertEquals(500L, fakeTransactionDao.getById("t1").amount);
    }

    @Test
    public void upsertBatch_incomingTombstoneNewer_marksDeletedLocally() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 3000L, 500L, true)), USER_ID);
        assertTrue(fakeTransactionDao.getById("t1").isDeleted);
        assertTrue(fakeTransactionDao.getAllOnce().isEmpty());
    }

    @Test
    public void upsertBatch_localPendingNewerThanIncoming_staysPendingAndUnchanged() {
        fakeTransactionDao.insert(entity("t1", 5000L, "pending", 500L));
        SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 4000L, 900L, false)), USER_ID);
        TransactionEntity saved = fakeTransactionDao.getById("t1");
        assertEquals("pending", saved.syncStatus);
        assertEquals(500L, saved.amount);
        assertFalse(saved.isDeleted);
    }

    @Test
    public void upsertBatch_unknownRecord_reportsInsertedChangeWithoutBefore() {
        List<EntityChange<TransactionEntity>> changes = SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 2000L, 500L, false)), USER_ID);
        assertEquals(1, changes.size());
        assertNull(changes.get(0).before);
        assertEquals(500L, changes.get(0).after.amount);
    }

    @Test
    public void upsertBatch_incomingNewer_reportsChangeWithOldAndNewVersion() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        List<EntityChange<TransactionEntity>> changes = SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 3000L, 900L, false)), USER_ID);
        assertEquals(1, changes.size());
        assertEquals(500L, changes.get(0).before.amount);
        assertEquals(900L, changes.get(0).after.amount);
    }

    @Test
    public void upsertBatch_incomingNotNewer_reportsNoChange() {
        fakeTransactionDao.insert(entity("t1", 2000L, "synced", 500L));
        assertTrue(SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 2000L, 900L, false)), USER_ID).isEmpty());
        assertTrue(SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 1500L, 900L, false)), USER_ID).isEmpty());
    }


    //Mô phỏng 1 thiết bị nhận lần lượt 2 phiên bản của cùng 1 bản ghi (2 lần kéo khác nhau)
    private List<EntityChange<TransactionEntity>> applyOneByOne(FakeTransactionDao dao, TransactionSyncRecordDto first, TransactionSyncRecordDto second) {
        TransactionSyncableEntity target = new TransactionSyncableEntity(dao, null);
        SyncPuller.upsertBatch(target, Collections.singletonList(first), USER_ID);
        return SyncPuller.upsertBatch(target, Collections.singletonList(second), USER_ID);
    }

    @Test
    public void upsertBatch_localPendingSameUpdatedAtAsIncoming_keepsLocalAndStaysPending() {
        fakeTransactionDao.insert(entity("t1", 5000L, "pending", 500L));

        List<EntityChange<TransactionEntity>> changes = SyncPuller.upsertBatch(table, Collections.singletonList(record("t1", 5000L, 900L, false)), USER_ID);

        TransactionEntity saved = fakeTransactionDao.getById("t1");
        assertEquals(500L, saved.amount);
        assertEquals("pending", saved.syncStatus);
        assertEquals(5000L, saved.updatedAt);
        assertTrue(changes.isEmpty());
    }

    @Test
    public void upsertBatch_differentUpdatedAt_newerWinsInEitherArrivalOrder() {
        TransactionSyncRecordDto older = record("t1", 2000L, 111L, false);
        TransactionSyncRecordDto newer = record("t1", 3000L, 222L, false);
        FakeTransactionDao daoOlderArrivesFirst = new FakeTransactionDao();
        FakeTransactionDao daoNewerArrivesFirst = new FakeTransactionDao();

        applyOneByOne(daoOlderArrivesFirst, older, newer);
        applyOneByOne(daoNewerArrivesFirst, newer, older);

        assertEquals(222L, daoOlderArrivesFirst.getById("t1").amount);
        assertEquals(3000L, daoOlderArrivesFirst.getById("t1").updatedAt);
        assertEquals(222L, daoNewerArrivesFirst.getById("t1").amount);
        assertEquals(3000L, daoNewerArrivesFirst.getById("t1").updatedAt);
    }

    @Test
    public void upsertBatch_sameUpdatedAt_firstStoredVersionWinsAndLaterOneNeverOverwrites() {
        TransactionSyncRecordDto fromDeviceA = record("t1", 4000L, 111L, false);
        TransactionSyncRecordDto fromDeviceB = record("t1", 4000L, 222L, false);
        FakeTransactionDao daoAFirst = new FakeTransactionDao();
        FakeTransactionDao daoBFirst = new FakeTransactionDao();

        List<EntityChange<TransactionEntity>> changesWhenBArrivesSecond = applyOneByOne(daoAFirst, fromDeviceA, fromDeviceB);
        List<EntityChange<TransactionEntity>> changesWhenAArrivesSecond = applyOneByOne(daoBFirst, fromDeviceB, fromDeviceA);

        assertEquals(111L, daoAFirst.getById("t1").amount); //A đến trước thì A thắng
        assertEquals(222L, daoBFirst.getById("t1").amount); //B đến trước thì B thắng
        assertEquals(4000L, daoAFirst.getById("t1").updatedAt);
        assertEquals(4000L, daoBFirst.getById("t1").updatedAt);
        assertTrue(changesWhenBArrivesSecond.isEmpty()); //bản đến sau bị bỏ qua, không ghi gì
        assertTrue(changesWhenAArrivesSecond.isEmpty());
    }
}