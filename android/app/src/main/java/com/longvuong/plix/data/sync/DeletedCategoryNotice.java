package com.longvuong.plix.data.sync;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

//Phát hiện sau khi kéo dữ liệu: danh mục vừa bị xoá trên thiết bị khác mà giao dịch trên máy này vẫn đang dùng
//Các giao dịch đó tự hiển thị "Danh mục không xác định", lớp này chỉ báo để giao diện hiện 1 thông báo ngắn
@Singleton
public class DeletedCategoryNotice {
    private final TransactionDao transactionDao;
    private final MutableLiveData<Boolean> shouldShow = new MutableLiveData<>(false);

    @Inject
    public DeletedCategoryNotice(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    //Chạy trên luồng nền (gọi từ SyncWorker) sau khi đã kéo xong mọi bảng, nên giao dịch vừa kéo về cũng đã nằm trong Room
    public void checkAfterPull(List<EntityChange<CategoryEntity>> pulledCategories) {
        List<String> deletedIds = newlyDeletedCategoryIds(pulledCategories);
        if (deletedIds.isEmpty()) {
            return;
        }
        if (transactionDao.countActiveByCategoryIds(deletedIds) > 0) {
            shouldShow.postValue(true);
        }
    }

    //"Vừa nhận được là đã bị xoá" = bản vừa kéo về có is_deleted = true và trước đó máy này chưa biết nó đã bị xoá
    //(chưa có bản nào, hoặc bản cũ còn dùng được); tombstone đã biết từ trước mà được ghi đè lại thì không báo lại
    static List<String> newlyDeletedCategoryIds(List<EntityChange<CategoryEntity>> changes) {
        List<String> ids = new ArrayList<>();
        for (EntityChange<CategoryEntity> change : changes) {
            boolean wasAlreadyDeleted = change.before != null && change.before.isDeleted;
            if (change.after.isDeleted && !wasAlreadyDeleted) {
                ids.add(change.after.id);
            }
        }
        return ids;
    }

    public LiveData<Boolean> getShouldShow() {
        return shouldShow;
    }

    //Gọi trên luồng chính sau khi đã hiện thông báo, để thông báo chỉ hiện 1 lần
    public void markHandled() {
        shouldShow.setValue(false);
    }
}