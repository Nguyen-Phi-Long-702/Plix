package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.remote.api.SyncApiService;
import com.longvuong.plix.data.remote.dto.TransactionPullResponseDto;
import com.longvuong.plix.data.remote.dto.TransactionSyncRecordDto;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.inject.Inject;

import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class TransactionPuller {
    static final String TABLE_TRANSACTIONS = "transactions";
    static final int PULL_LIMIT = 500;

    //Gọi api kéo 1 lô bản ghi từ mốc since
    interface PageFetcher {
        Response<TransactionPullResponseDto> fetch(long since, int limit) throws IOException;
    }

    //Ghi 1 lô bản ghi vào Room và lưu con trỏ mới cùng lúc
    interface BatchApplier {
        void apply(List<TransactionSyncRecordDto> records, long newCursor);
    }

    private final SyncApiService syncApiService;
    private final TransactionDao transactionDao;
    private final AppDatabase appDatabase;
    private final SyncPreferences syncPreferences;

    @Inject
    public TransactionPuller(SyncApiService syncApiService, TransactionDao transactionDao,
                             AppDatabase appDatabase, SyncPreferences syncPreferences) {
        this.syncApiService = syncApiService;
        this.transactionDao = transactionDao;
        this.appDatabase = appDatabase;
        this.syncPreferences = syncPreferences;
    }

    //Kéo giao dịch từ máy chủ về Room cho tới khi hết dữ liệu. Chạy trên luồng nền (gọi từ SyncWorker)
    public void pull(String userId, BooleanSupplier shouldStop) throws IOException {
        long startCursor = syncPreferences.getPullCursor(userId, TABLE_TRANSACTIONS);
        pullAll(
                (since, limit) -> syncApiService.pullTransactions(since, limit).execute(),
                startCursor,
                shouldStop,
                (records, newCursor) -> appDatabase.runInTransaction(() -> {
                    upsertBatch(transactionDao, records, userId);
                    //Ghi con trỏ ở dòng cuối: nếu ghi bản ghi bị lỗi thì exception văng ra trước, con trỏ giữ nguyên
                    syncPreferences.setPullCursor(userId, TABLE_TRANSACTIONS, newCursor);
                }));
    }

    static void pullAll(PageFetcher fetcher, long startCursor, BooleanSupplier shouldStop, BatchApplier applier) throws IOException {
        long since = startCursor;
        Set<String> previousIds = null;
        while (!shouldStop.getAsBoolean()) {
            Response<TransactionPullResponseDto> response = fetcher.fetch(since, PULL_LIMIT);
            if (!response.isSuccessful() || response.body() == null) {
                throw new HttpException(response);
            }
            TransactionPullResponseDto body = response.body();
            List<TransactionSyncRecordDto> records = body.records != null ? body.records : Collections.<TransactionSyncRecordDto>emptyList();
            if (records.isEmpty()) {
                return;
            }
            Set<String> ids = idsOf(records);
            if (ids.equals(previousIds)) {
                //Máy chủ trả lại đúng tập bản ghi của lần trước -> sẽ lặp vô hạn nếu gọi tiếp, dừng ngay
                Timber.w("Hai lần kéo liên tiếp trả về cùng một tập %d bản ghi, dừng vòng lặp để tránh lặp vô hạn", ids.size());
                return;
            }
            long newCursor = maxUpdatedAtOf(records);
            applier.apply(records, newCursor);
            Timber.d("Đã kéo %d giao dịch, con trỏ mới = %d, còn dữ liệu = %b", records.size(), newCursor, body.hasMore);
            if (!body.hasMore) {
                return;
            }
            previousIds = ids;
            since = newCursor;
        }
    }

    //Chỉ ghi đè khi bản nhận về mới hơn, bằng hoặc cũ hơn thì giữ bản đang lưu
    static void upsertBatch(TransactionDao transactionDao, List<TransactionSyncRecordDto> records, String userId) {
        for (TransactionSyncRecordDto record : records) {
            TransactionEntity existing = transactionDao.getById(record.id);
            if (existing == null) {
                transactionDao.insert(record.toEntity(userId));
            } else if (record.updatedAt > existing.updatedAt) {
                transactionDao.update(record.toEntity(userId));
            }
        }
    }

    private static Set<String> idsOf(List<TransactionSyncRecordDto> records) {
        Set<String> ids = new HashSet<>();
        for (TransactionSyncRecordDto record : records) {
            ids.add(record.id);
        }
        return ids;
    }

    private static long maxUpdatedAtOf(List<TransactionSyncRecordDto> records) {
        long max = Long.MIN_VALUE;
        for (TransactionSyncRecordDto record : records) {
            max = Math.max(max, record.updatedAt);
        }
        return max;
    }
}