package com.longvuong.plix.domain.usecase.budget;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

class FakeTransactionRepository implements TransactionRepository {
    private final List<TransactionEntity> transactions = new ArrayList<>();

    void seed(TransactionEntity entity) {
        transactions.add(entity);
    }

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
        transactions.add(entity);
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void update(TransactionEntity entity, RepositoryCallback<Void> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getById(String id, RepositoryCallback<TransactionEntity> callback) {
        callback.onResult(new Result.Success<>(null));
    }

    @Override
    public void getAllOnce(RepositoryCallback<List<TransactionEntity>> callback) {
        callback.onResult(new Result.Success<>(new ArrayList<>(transactions)));
    }
}