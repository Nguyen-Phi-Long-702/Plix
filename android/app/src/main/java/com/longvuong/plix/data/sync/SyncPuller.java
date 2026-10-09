package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.inject.Inject;

import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class SyncPuller {
    static final int PULL_LIMIT = 500;

    //Gọi api kéo 1 lô bản ghi từ mốc since
    interface PageFetcher<D> {
        Response<SyncPullResponseDto<D>> fetch(long since, int limit) throws IOException;
    }

    //Ghi 1 lô bản ghi vào Room và lưu con trỏ mới cùng lúc
    interface BatchApplier<D> {
        void apply(List<D> records, long newCursor);
    }

    private final AppDatabase appDatabase;
    private final SyncPreferences syncPreferences;

    @Inject
    public SyncPuller(AppDatabase appDatabase, SyncPreferences syncPreferences) {
        this.appDatabase = appDatabase;
        this.syncPreferences = syncPreferences;
    }

    //Kéo 1 bảng từ máy chủ về Room cho tới khi hết dữ liệu, không cần biết bản ghi nào thay đổi. Chạy trên luồng nền (gọi từ SyncWorker)
    public <E, D> void pull(SyncableEntity<E, D> table, String userId, BooleanSupplier shouldStop) throws IOException {
        pull(table, userId, shouldStop, new ArrayList<>());
    }

    //Như trên, đồng thời ghi nhận mọi bản ghi thực sự được ghi vào Room vào danh sách changes (để kiểm tra ngưỡng ngân sách sau khi kéo)
    //changes do người gọi truyền vào nên nếu kéo lỗi giữa chừng, các lô đã ghi thành công vẫn còn trong danh sách
    public <E, D> void pull(SyncableEntity<E, D> table, String userId, BooleanSupplier shouldStop, List<EntityChange<E>> changes) throws IOException {
        String tableName = table.tableName();
        long startCursor = syncPreferences.getPullCursor(userId, tableName);
        pullAll(
                table,
                (since, limit) -> table.pull(since, limit).execute(),
                startCursor,
                shouldStop,
                (records, newCursor) -> {
                    List<EntityChange<E>> batchChanges = appDatabase.runInTransaction(() -> {
                        List<EntityChange<E>> applied = upsertBatch(table, records, userId);
                        //Ghi con trỏ ở dòng cuối: nếu ghi bản ghi bị lỗi thì exception văng ra trước, con trỏ giữ nguyên
                        syncPreferences.setPullCursor(userId, tableName, newCursor);
                        return applied;
                    });
                    //Chỉ ghi nhận thay đổi sau khi giao dịch Room đã ghi xong
                    changes.addAll(batchChanges);
                });
    }

    static <E, D> void pullAll(SyncableEntity<E, D> table, PageFetcher<D> fetcher, long startCursor, BooleanSupplier shouldStop, BatchApplier<D> applier) throws IOException {
        long since = startCursor;
        Set<String> previousIds = null;
        while (!shouldStop.getAsBoolean()) {
            Response<SyncPullResponseDto<D>> response = fetcher.fetch(since, PULL_LIMIT);
            if (!response.isSuccessful() || response.body() == null) {
                throw new HttpException(response);
            }
            SyncPullResponseDto<D> body = response.body();
            List<D> records = body.records != null ? body.records : Collections.<D>emptyList();
            if (records.isEmpty()) {
                return;
            }
            Set<String> ids = idsOf(table, records);
            if (ids.equals(previousIds)) {
                //Máy chủ trả lại đúng tập bản ghi của lần trước -> sẽ lặp vô hạn nếu gọi tiếp, dừng ngay
                Timber.w("Hai lần kéo bảng %s liên tiếp trả về cùng một tập %d bản ghi, dừng vòng lặp để tránh lặp vô hạn", table.tableName(), ids.size());
                return;
            }
            long newCursor = maxUpdatedAtOf(table, records);
            applier.apply(records, newCursor);
            Timber.d("Đã kéo %d bản ghi bảng %s, con trỏ mới = %d, còn dữ liệu = %b", records.size(), table.tableName(), newCursor, body.hasMore);
            if (!body.hasMore) {
                return;
            }
            previousIds = ids;
            since = newCursor;
        }
    }

    //Chỉ ghi đè khi bản nhận về mới hơn, bằng hoặc cũ hơn thì giữ bản đang lưu
    //Trả về danh sách bản ghi thực sự được ghi (thêm mới hoặc ghi đè), bản bị bỏ qua không nằm trong danh sách
    static <E, D> List<EntityChange<E>> upsertBatch(SyncableEntity<E, D> table, List<D> records, String userId) {
        List<EntityChange<E>> changes = new ArrayList<>();
        for (D record : records) {
            E existing = table.findById(table.recordId(record));
            if (existing == null) {
                E created = table.toEntity(record, userId);
                table.insert(created);
                changes.add(new EntityChange<>(null, created));
            } else if (table.recordUpdatedAt(record) > table.entityUpdatedAt(existing)) {
                E updated = table.toEntity(record, userId);
                table.update(updated);
                changes.add(new EntityChange<>(existing, updated));
            }
        }
        return changes;
    }

    private static <E, D> Set<String> idsOf(SyncableEntity<E, D> table, List<D> records) {
        Set<String> ids = new HashSet<>();
        for (D record : records) {
            ids.add(table.recordId(record));
        }
        return ids;
    }

    private static <E, D> long maxUpdatedAtOf(SyncableEntity<E, D> table, List<D> records) {
        long max = Long.MIN_VALUE;
        for (D record : records) {
            max = Math.max(max, table.recordUpdatedAt(record));
        }
        return max;
    }
}