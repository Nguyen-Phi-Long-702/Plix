package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

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

public class SyncPusher {
    static final int BATCH_SIZE = 50;

    @Inject
    public SyncPusher() {
    }

    //Đẩy mọi bản ghi pending của 1 bảng lên máy chủ theo từng lô 50. Chạy trên luồng nền (gọi từ SyncWorker)
    //Máy chủ trả lỗi thì ném HttpException, mất mạng thì ném IOException; các lô đã thành công trước đó vẫn giữ trạng thái synced
    public <E, D> void push(SyncableEntity<E, D> table, String userId, BooleanSupplier shouldStop) throws IOException {
        List<E> pending = table.getPendingSync(userId);
        if (pending.isEmpty()) {
            return;
        }
        Timber.d("Bắt đầu đẩy %d bản ghi bảng %s đang chờ đồng bộ", pending.size(), table.tableName());
        for (int from = 0; from < pending.size(); from += BATCH_SIZE) {
            if (shouldStop.getAsBoolean()) {
                return; //bị thay thế bởi lượt debounce mới, kết quả của lượt này sẽ bị bỏ qua
            }
            int to = Math.min(from + BATCH_SIZE, pending.size());
            pushBatch(table, pending.subList(from, to));
        }
    }

    private <E, D> void pushBatch(SyncableEntity<E, D> table, List<E> batch) throws IOException {
        List<D> records = new ArrayList<>();
        for (E entity : batch) {
            records.add(table.toRecord(entity));
        }
        Response<SyncPushResponseDto> response = table.push(records).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new HttpException(response);
        }
        markSynced(table, batch, response.body());
    }

    private <E, D> void markSynced(SyncableEntity<E, D> table, List<E> batch, SyncPushResponseDto body) {
        Set<String> upsertedIds = new HashSet<>(
                body.upsertedIds != null ? body.upsertedIds : Collections.<String>emptyList());
        for (E entity : batch) {
            String id = table.entityId(entity);
            if (upsertedIds.contains(id)) {
                //chỉ đánh dấu khi updated_at không đổi, nếu người dùng vừa sửa thì giữ pending cho lượt sau
                table.markSynced(id, table.entityUpdatedAt(entity));
            }
        }
        if (body.rejected != null && !body.rejected.isEmpty()) {
            Timber.w("Máy chủ từ chối %d bản ghi bảng %s, giữ nguyên trạng thái pending", body.rejected.size(), table.tableName());
        }
    }
}