package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.List;

public interface TransactionRepository {

    LiveData<List<TransactionEntity>> getAll();

    void insert(TransactionEntity entity, RepositoryCallback<Void> callback);

    void update(TransactionEntity entity, RepositoryCallback<Void> callback);
}