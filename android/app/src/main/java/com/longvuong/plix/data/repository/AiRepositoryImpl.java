package com.longvuong.plix.data.repository;

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

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class AiRepositoryImpl implements AiRepository {

    private final AiApiService aiApiService;
    private final CategoryDao categoryDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;

    private volatile Call<CategorizeResponseDto> currentCall;

    @Inject
    public AiRepositoryImpl(AiApiService aiApiService, CategoryDao categoryDao,
                            AppExecutors appExecutors, ErrorMapper errorMapper) {
        this.aiApiService = aiApiService;
        this.categoryDao = categoryDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
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
                    result = new Result.Success<>(new CategorySuggestion(category, dto.confidence));
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
}