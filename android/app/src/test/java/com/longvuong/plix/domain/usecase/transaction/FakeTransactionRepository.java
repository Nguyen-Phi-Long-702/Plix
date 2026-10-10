package com.longvuong.plix.domain.usecase.transaction;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

class FakeTransactionRepository implements TransactionRepository {
    boolean insertCalled;
    boolean updateCalled;
    TransactionEntity lastInserted;
    TransactionEntity lastUpdated;
    Result<Integer> countPendingResult = new Result.Success<>(0);
    boolean countPendingCalled;
    String lastExcludedTransactionId;

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return null;
    }

    @Override
    public LiveData<List<TransactionEntity>> searchByNote(String query) {
        return null;
    }

    @Override
    public void insert(TransactionEntity entity, RepositoryCallback<Void> callback) {
        insertCalled = true;
        lastInserted = entity;
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(TransactionEntity entity, RepositoryCallback<Void> callback) {
        updateCalled = true;
        lastUpdated = entity;
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<TransactionEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getAllOnce(RepositoryCallback<List<TransactionEntity>> callback) {
        callback.onResult(new Result.Success<>(new ArrayList<>()));
    }

    @Override
    public void countPendingByCategory(String userId, String categoryId, String excludeTransactionId, RepositoryCallback<Integer> callback) {
        countPendingCalled = true;
        lastExcludedTransactionId = excludeTransactionId;
        callback.onResult(countPendingResult);
    }
}