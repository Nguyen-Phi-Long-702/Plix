package com.longvuong.plix.core.error;

public interface RepositoryCallback<T> {
    void onResult(Result<T> result);
}