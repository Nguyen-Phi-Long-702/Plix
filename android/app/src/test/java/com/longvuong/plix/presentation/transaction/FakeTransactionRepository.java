package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FakeTransactionRepository implements TransactionRepository {

    boolean insertCalled;
    TransactionEntity lastInserted;
    final List<TransactionEntity> insertedEntities = new ArrayList<>();
    Result<Integer> countPendingResult = new Result.Success<>(0);

    private final Map<String, TransactionEntity> storage = new HashMap<>();

    //Chưa có giá trị = Room chưa trả dữ liệu lần đầu; test đặt danh sách (đã sắp theo ngày giảm dần) bằng setValue
    final MutableLiveData<List<TransactionEntity>> allTransactions = new MutableLiveData<>();

    void seed(TransactionEntity entity) {
        storage.put(entity.id, entity);
    }

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return allTransactions;
    }

    @Override
    public LiveData<List<TransactionEntity>> searchByNote(String query) {
        return null; //Không dùng trong test usecase
    }

    @Override
    public void insert(TransactionEntity entity, RepositoryCallback<Void> callback) {
        insertCalled = true;
        lastInserted = entity;
        insertedEntities.add(entity);
        storage.put(entity.id, entity);
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(TransactionEntity entity, RepositoryCallback<Void> callback) {
        storage.put(entity.id, entity);
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<TransactionEntity> callback) {
        callback.onResult(new Result.Success<>(storage.get(id)));
    }

    @Override
    public void getAllOnce(RepositoryCallback<List<TransactionEntity>> callback) {
        callback.onResult(new Result.Success<>(new ArrayList<>(storage.values())));
    }

    @Override
    public void countPendingByCategory(String userId, String categoryId, String excludeTransactionId, RepositoryCallback<Integer> callback) {
        callback.onResult(countPendingResult);
    }
    @Override
    public void findSimilar(long amount, String categoryId, long dayStart, long dayEnd, RepositoryCallback<TransactionEntity> callback) {
        callback.onResult(new Result.Success<>(null)); //Không dùng trong test này
    }
}