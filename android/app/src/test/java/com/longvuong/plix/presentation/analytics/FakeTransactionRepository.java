package com.longvuong.plix.presentation.analytics;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

class FakeTransactionRepository implements TransactionRepository {

    int getAllOnceCallCount;
    Result<List<TransactionEntity>> allOnceResult = new Result.Success<>(new ArrayList<>());

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return null; //Không dùng trong test này
    }

    @Override
    public LiveData<List<TransactionEntity>> searchByNote(String query) {
        return null; //Không dùng trong test này
    }

    @Override
    public void insert(TransactionEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null)); //Không dùng trong test này
    }

    @Override
    public void update(TransactionEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null)); //Không dùng trong test này
    }

    @Override
    public void getById(String id, RepositoryCallback<TransactionEntity> callback) {
        callback.onResult(new Result.Success<>(null)); //Không dùng trong test này
    }

    @Override
    public void getAllOnce(RepositoryCallback<List<TransactionEntity>> callback) {
        getAllOnceCallCount++;
        callback.onResult(allOnceResult);
    }

    @Override
    public void countPendingByCategory(String userId, String categoryId, String excludeTransactionId, RepositoryCallback<Integer> callback) {
        callback.onResult(new Result.Success<>(0)); //Không dùng trong test này
    }
}