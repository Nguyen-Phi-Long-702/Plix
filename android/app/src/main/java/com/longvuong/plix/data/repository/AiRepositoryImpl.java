package com.longvuong.plix.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.remote.api.AiApiService;
import com.longvuong.plix.data.remote.dto.CategorizeRequestDto;
import com.longvuong.plix.data.remote.dto.CategorizeResponseDto;
import com.longvuong.plix.data.remote.dto.CorrectionRequestDto;
import com.longvuong.plix.data.remote.dto.RetrainResponseDto;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class AiRepositoryImpl implements AiRepository {

    private static final String PREF_NAME = "ai_prefs";
    private static final String KEY_PENDING_CORRECTION_COUNT = "pending_correction_count";

    private final AiApiService aiApiService;
    private final CategoryDao categoryDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;
    private final Context context;

    private volatile Call<CategorizeResponseDto> currentCall;

    @Inject
    public AiRepositoryImpl(AiApiService aiApiService, CategoryDao categoryDao,
                            AppExecutors appExecutors, ErrorMapper errorMapper,
                            @ApplicationContext Context context) {
        this.aiApiService = aiApiService;
        this.categoryDao = categoryDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
        this.context = context;
    }

    @Override
    public void categorize(String note, RepositoryCallback<CategorySuggestion> callback) {
        Call<CategorizeResponseDto> call = aiApiService.categorize(new CategorizeRequestDto(note));
        currentCall = call;

        appExecutors.networkIO().execute(() -> {
            try {
                Response<CategorizeResponseDto> response = call.execute();
                Result<CategorySuggestion> result;
                if (response.isSuccessful() && response.body() != null) {
                    CategorizeResponseDto dto = response.body();
                    CategoryEntity category = categoryDao.getById(dto.categoryId);
                    result = new Result.Success<>(new CategorySuggestion(dto.categoryId, category, dto.confidence));
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code());
                }
                Result<CategorySuggestion> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                if (call.isCanceled()) {
                    return; //Đã bị huỷ (debounce mới hoặc ViewModel dọn dẹp) — không cần trả kết quả nữa
                }
                Result<CategorySuggestion> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void cancelPendingCategorize() {
        Call<CategorizeResponseDto> call = currentCall;
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public void submitCorrection(String transactionId, @Nullable String predictedCategoryId, String correctedCategoryId, RepositoryCallback<Void> callback) {
        Call<Void> call = aiApiService.submitCorrection(new CorrectionRequestDto(transactionId, predictedCategoryId, correctedCategoryId));

        appExecutors.networkIO().execute(() -> {
            try {
                Response<Void> response = call.execute();
                Result<Void> result;
                if (response.isSuccessful()) {
                    incrementPendingCorrectionCount(); //Ghi nhận 1 lần sửa gợi ý mới cho huy hiệu ở Settings (Ngày 22)
                    result = new Result.Success<>(null);
                } else {
                    result = errorMapper.mapHttpCode(response.code());
                }
                Result<Void> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                Result<Void> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void retrain(RepositoryCallback<Void> callback) {
        Call<RetrainResponseDto> call = aiApiService.retrain();

        appExecutors.networkIO().execute(() -> {
            try {
                Response<RetrainResponseDto> response = call.execute();
                Result<Void> result;
                if (response.isSuccessful() && response.body() != null) {
                    resetPendingCorrectionCount(); //Đã huấn luyện lại xong, số correction tích luỹ coi như đã dùng
                    result = new Result.Success<>(null);
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code(), readErrorBodySafely(response));
                }
                Result<Void> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                Result<Void> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public int getPendingCorrectionCount() {
        return prefs().getInt(KEY_PENDING_CORRECTION_COUNT, 0);
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

    private void incrementPendingCorrectionCount() {
        int current = prefs().getInt(KEY_PENDING_CORRECTION_COUNT, 0);
        prefs().edit().putInt(KEY_PENDING_CORRECTION_COUNT, current + 1).apply();
    }

    private void resetPendingCorrectionCount() {
        prefs().edit().putInt(KEY_PENDING_CORRECTION_COUNT, 0).apply();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}