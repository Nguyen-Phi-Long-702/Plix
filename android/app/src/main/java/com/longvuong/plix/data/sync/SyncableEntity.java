package com.longvuong.plix.data.sync;

import com.longvuong.plix.data.remote.dto.SyncPullResponseDto;
import com.longvuong.plix.data.remote.dto.SyncPushResponseDto;

import java.util.List;

import retrofit2.Call;

public interface SyncableEntity<E, D> {
    //Tên bảng, cũng là phần khoá của con trỏ kéo dữ liệu (user_id, bảng)
    String tableName();

    //Các bản ghi sync_status = 'pending' của user, gồm cả bản ghi đã xoá mềm
    List<E> getPendingSync(String userId);

    String entityId(E entity);

    long entityUpdatedAt(E entity);

    D toRecord(E entity);

    //Chỉ đánh dấu synced khi updated_at không đổi, nếu người dùng vừa sửa thì giữ pending cho lượt sau
    void markSynced(String id, long updatedAt);

    Call<SyncPushResponseDto> push(List<D> records);

    Call<SyncPullResponseDto<D>> pull(long since, int limit);

    String recordId(D record);

    long recordUpdatedAt(D record);

    E findById(String id);

    void insert(E entity);

    void update(E entity);

    //Bản ghi kéo về coi như đã đồng bộ, user_id lấy từ phiên đăng nhập hiện tại
    E toEntity(D record, String userId);
}