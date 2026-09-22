package com.longvuong.plix.data.sync;

import androidx.lifecycle.LiveData;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.List;

class FakeTransactionDao implements TransactionDao {
    private final List<TransactionEntity> storage = new ArrayList<>();

    @Override
    public void insert(TransactionEntity entity) {
        storage.add(entity);
    }

    @Override
    public void update(TransactionEntity entity) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).id.equals(entity.id)) {
                storage.set(i, entity);
                return;
            }
        }
    }

    @Override
    public LiveData<List<TransactionEntity>> getAll() {
        return null;
    }

    @Override
    public TransactionEntity getById(String id) {
        for (TransactionEntity entity : storage) {
            if (entity.id.equals(id)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public LiveData<List<TransactionEntity>> searchByNote(String query) {
        return null;
    }

    @Override
    public List<TransactionEntity> getActiveRecurringTemplates() {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity entity : storage) {
            if (entity.isRecurring && entity.recurrenceParentId == null && !entity.isDeleted) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public List<TransactionEntity> getInstancesByRecurrenceParentId(String templateId) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity entity : storage) {
            if (templateId.equals(entity.recurrenceParentId) && !entity.isDeleted) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public List<TransactionEntity> getAllInstancesByRecurrenceParentId(String templateId) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity entity : storage) {
            if (templateId.equals(entity.recurrenceParentId)) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public List<TransactionEntity> getAllOnce() {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity entity : storage) {
            if (!entity.isDeleted) {
                result.add(entity);
            }
        }
        return result;
    }
}