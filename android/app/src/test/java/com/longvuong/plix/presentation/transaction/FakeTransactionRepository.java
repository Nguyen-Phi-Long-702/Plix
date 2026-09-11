package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FakeTransactionRepository implements TransactionRepository {

    boolean insertCalled;
    TransactionEntity lastInserted;

    private final Map<String, TransactionEntity> storage = new HashMap<>();

    void seed(TransactionEntity entity) {
        storage.put(entity.id, entity);
    }

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return null;
    }

    @Override
    public LiveData<List<TransactionEntity>> searchByNote(String query) {
        return null; //Không dùng trong test usecase
    }

    @Override
    public void insert(TransactionEntity entity, RepositoryCallback<Void> callback) {
        insertCalled = true;
        lastInserted = entity;
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
}