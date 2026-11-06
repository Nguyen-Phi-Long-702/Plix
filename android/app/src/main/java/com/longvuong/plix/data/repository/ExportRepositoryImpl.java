package com.longvuong.plix.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.remote.api.ExportApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class ExportRepositoryImpl implements ExportRepository {

    private static final String EXPORT_DIR = "exports"; //khớp với <cache-path path="exports/"> trong res/xml/file_paths.xml
    private static final String FILE_NAME_PREFIX = "plix-giao-dich-";

    private final ExportApiService exportApiService;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;
    private final Context context;

    @Inject
    public ExportRepositoryImpl(ExportApiService exportApiService, AppExecutors appExecutors,
                                ErrorMapper errorMapper, @ApplicationContext Context context) {
        this.exportApiService = exportApiService;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
        this.context = context;
    }

    @Override
    public void exportCsv(RepositoryCallback<File> callback) {
        Call<ResponseBody> call = exportApiService.exportCsv();

        appExecutors.networkIO().execute(() -> {
            Result<File> result;
            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    result = saveToCache(response.body());
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code(), readErrorBodySafely(response)); //429 -> câu tiếng Việt do máy chủ gửi
                }
            } catch (Exception e) {
                result = errorMapper.mapThrowable(e);
            }
            Result<File> finalResult = result;
            appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
        });
    }

    //Ghi file vào cacheDir/exports/plix-giao-dich-yyyy-MM-dd.csv (cùng ngày thì ghi đè)
    private Result<File> saveToCache(ResponseBody body) {
        try (body) {
            File dir = new File(context.getCacheDir(), EXPORT_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Không tạo được thư mục xuất dữ liệu");
            }
            File file = new File(dir, FILE_NAME_PREFIX + LocalDate.now(ZoneId.systemDefault()) + ".csv");
            try (InputStream input = body.byteStream(); OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            return new Result.Success<>(file);
        } catch (IOException e) {
            return new Result.Error<>(ErrorType.UNKNOWN, "Không lưu được file CSV, vui lòng thử lại", e);
        }
    }

    @Nullable
    private String readErrorBodySafely(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            return null;
        }
    }
}