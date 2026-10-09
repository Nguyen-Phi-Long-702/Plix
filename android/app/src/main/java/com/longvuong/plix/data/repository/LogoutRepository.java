package com.longvuong.plix.data.repository;

//Các thao tác dữ liệu phục vụ luồng đăng xuất (Mục 18.5). Mọi method chạy đồng bộ, chỉ được gọi trên luồng nền
public interface LogoutRepository {
    //Số bản ghi sync_status = 'pending' của người dùng hiện tại ở cả 5 bảng (gồm cả bản ghi xoá mềm)
    int countPendingChanges();

    //Có mạng hay không, theo NetworkObserver
    boolean isOnline();

    //Huỷ SyncWorker (định kỳ và chạy ngay) rồi chờ lượt đang chạy dở kết thúc hẳn
    void cancelRunningSync();

    //Thử đẩy lần cuối cả 5 bảng theo thứ tự cố định, chờ tối đa timeoutMillis
    //Thất bại hay quá hạn đều không ném lỗi, người gọi tự đếm lại bản ghi pending để biết kết quả
    void pushAllPending(long timeoutMillis);

    //Đăng xuất thật: chờ SyncWorker xong rồi xoá dữ liệu cục bộ của người dùng, con trỏ đồng bộ và token
    void clearLocalSession();

    //Người dùng huỷ đăng xuất: bật lại đồng bộ nền đã bị huỷ ở cancelRunningSync()
    void resumeSync();
}