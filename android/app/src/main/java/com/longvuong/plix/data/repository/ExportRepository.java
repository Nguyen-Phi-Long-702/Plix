package com.longvuong.plix.data.repository;

import com.longvuong.plix.core.error.RepositoryCallback;

import java.io.File;

public interface ExportRepository {

    //Gọi /export/csv, lưu file CSV vào thư mục tạm của ứng dụng và trả về file đó; callback chạy trên luồng chính
    void exportCsv(RepositoryCallback<File> callback);
}