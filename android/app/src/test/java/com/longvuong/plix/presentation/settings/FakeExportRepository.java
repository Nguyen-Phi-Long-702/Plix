package com.longvuong.plix.presentation.settings;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ExportRepository;

import java.io.File;

class FakeExportRepository implements ExportRepository {

    int exportCallCount;
    private RepositoryCallback<File> pendingCallback;

    @Override
    public void exportCsv(RepositoryCallback<File> callback) {
        exportCallCount++;
        pendingCallback = callback; //chưa trả ngay - test tự quyết định lúc nào trả kết quả để bắt được UiState.Loading
    }

    void complete(Result<File> result) {
        RepositoryCallback<File> callback = pendingCallback;
        pendingCallback = null;
        if (callback != null) {
            callback.onResult(result);
        }
    }
}