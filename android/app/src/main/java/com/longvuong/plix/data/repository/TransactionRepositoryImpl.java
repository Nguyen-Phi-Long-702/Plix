package com.longvuong.plix.data.repository;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionRepositoryImpl implements TransactionRepository {

    private final TransactionDao transactionDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    @Inject
    public TransactionRepositoryImpl(TransactionDao transactionDao,
                                     AppExecutors appExecutors,
                                     ErrorMapper errorMapper) {
        this.transactionDao = transactionDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
    }

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return transactionDao.getAll();
    }

    @Override
    public void insert(TransactionEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                transactionDao.insert(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    @Override
    public void update(TransactionEntity entity, RepositoryCallback<Void> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                transactionDao.update(entity);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, e);
            }
        });
    }

    private void notifySuccess(RepositoryCallback<Void> callback) {
        appExecutors.mainThread().execute(() -> callback.onResult(new Result.Success<>(null)));
    }

    private void notifyError(RepositoryCallback<Void> callback, Exception e) {
        Result<Void> error = errorMapper.mapThrowable(e);
        appExecutors.mainThread().execute(() -> callback.onResult(error));
    }
    @Override
    public void getById(String id, RepositoryCallback<TransactionEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                TransactionEntity entity = transactionDao.getById(id);
                appExecutors.mainThread().execute(() ->
                        callback.onResult(new Result.Success<>(entity)));
            } catch (Exception e) {
                Result<TransactionEntity> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }
}