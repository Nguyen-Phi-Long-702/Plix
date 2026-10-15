package com.longvuong.plix.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import com.longvuong.plix.data.remote.dto.AnomalyResponseDto;
import com.longvuong.plix.data.remote.dto.ForecastResponseDto;
import com.longvuong.plix.data.remote.dto.ParseNlRequestDto;
import com.longvuong.plix.data.remote.dto.ParseNlResponseDto;
import com.longvuong.plix.core.network.NetworkObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class AiRepositoryImpl implements AiRepository {

    private static final String PREF_NAME = "ai_prefs";
    private static final String KEY_PENDING_CORRECTION_COUNT = "pending_correction_count";
    private static final String KEY_QUICK_ADD_ENABLED = "quick_add_enabled";
    private static final String KEY_GEMINI_FALLBACK_ENABLED = "gemini_fallback_enabled";
    private static final boolean DEFAULT_QUICK_ADD_ENABLED = true;
    private static final boolean DEFAULT_GEMINI_FALLBACK_ENABLED = false; //Gemini mặc định tắt vì liên quan quyền riêng tư
    //Giá trị status do /anomaly trả về
    private static final String STATUS_HIGH = "flagged_high";
    private static final String STATUS_LOW = "flagged_low";
    private static final String STATUS_NORMAL = "normal";
    private static final String STATUS_INSUFFICIENT_DATA = "insufficient_data";

    private final AiApiService aiApiService;
    private final CategoryDao categoryDao;
    private final AppExecutors appExecutors;
    private final ErrorMapper errorMapper;
    private final Context context;
    private final NetworkObserver networkObserver;

    private volatile Call<CategorizeResponseDto> currentCall;
    private volatile Call<AnomalyResponseDto> currentAnomalyCall;
    private volatile Call<ForecastResponseDto> currentForecastCall;

    private final MutableLiveData<Boolean> quickAddEnabled;
    private final MutableLiveData<Boolean> geminiFallbackEnabled;

    @Inject
    public AiRepositoryImpl(AiApiService aiApiService, CategoryDao categoryDao,
                            AppExecutors appExecutors, ErrorMapper errorMapper,
                            @ApplicationContext Context context, NetworkObserver networkObserver) {
        this.aiApiService = aiApiService;
        this.categoryDao = categoryDao;
        this.appExecutors = appExecutors;
        this.errorMapper = errorMapper;
        this.context = context;
        this.networkObserver = networkObserver;
        this.quickAddEnabled = new MutableLiveData<>(prefs().getBoolean(KEY_QUICK_ADD_ENABLED, DEFAULT_QUICK_ADD_ENABLED));
        this.geminiFallbackEnabled = new MutableLiveData<>(prefs().getBoolean(KEY_GEMINI_FALLBACK_ENABLED, DEFAULT_GEMINI_FALLBACK_ENABLED));
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
    public void checkAnomaly(String categoryId, long amount, RepositoryCallback<AnomalyResult> callback) {
        Call<AnomalyResponseDto> call = aiApiService.checkAnomaly(categoryId, amount);
        currentAnomalyCall = call;

        appExecutors.networkIO().execute(() -> {
            try {
                Response<AnomalyResponseDto> response = call.execute();
                Result<AnomalyResult> result;
                if (response.isSuccessful() && response.body() != null) {
                    AnomalyResponseDto dto = response.body();
                    AnomalyResult.Level level = mapAnomalyLevel(dto.status);
                    if (level != null) {
                        result = new Result.Success<>(new AnomalyResult(level, dto.explanation));
                    } else {
                        result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                    }
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code());
                }
                Result<AnomalyResult> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                if (call.isCanceled()) {
                    return;
                }
                Result<AnomalyResult> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void cancelPendingAnomalyCheck() {
        Call<AnomalyResponseDto> call = currentAnomalyCall;
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public void getForecast(RepositoryCallback<ForecastResult> callback) {
        Call<ForecastResponseDto> call = aiApiService.getForecast();
        currentForecastCall = call;

        appExecutors.networkIO().execute(() -> {
            try {
                Response<ForecastResponseDto> response = call.execute();
                Result<ForecastResult> result;
                if (response.isSuccessful() && response.body() != null) {
                    result = mapForecastResponse(response.body());
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code());
                }
                Result<ForecastResult> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                if (call.isCanceled()) {
                    return;
                }
                Result<ForecastResult> error = errorMapper.mapThrowable(e);
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    @Override
    public void cancelPendingForecast() {
        Call<ForecastResponseDto> call = currentForecastCall;
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public void parseNl(String text, boolean allowGemini, RepositoryCallback<List<ParseNlItem>> callback) {
        Call<ParseNlResponseDto> call = aiApiService.parseNl(new ParseNlRequestDto(text, allowGemini));

        appExecutors.networkIO().execute(() -> {
            try {
                Response<ParseNlResponseDto> response = call.execute();
                Result<List<ParseNlItem>> result;
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    result = new Result.Success<>(mapParseNlItems(response.body().items));
                } else if (response.isSuccessful()) {
                    result = new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
                } else {
                    result = errorMapper.mapHttpCode(response.code(), readErrorBodySafely(response));
                }
                Result<List<ParseNlItem>> finalResult = result;
                appExecutors.mainThread().execute(() -> callback.onResult(finalResult));
            } catch (Exception e) {
                Result<List<ParseNlItem>> error = errorMapper.mapThrowable(e); //mất mạng/đọc Room lỗi -> trả Result.Error, không để crash
                appExecutors.mainThread().execute(() -> callback.onResult(error));
            }
        });
    }

    //Đổi từng item của /parse-nl sang ParseNlItem; amount null (câu không có số tiền) -> 0, người dùng tự nhập khi sửa dòng
    private List<ParseNlItem> mapParseNlItems(List<ParseNlResponseDto.ParseNlItemDto> dtoItems) {
        List<ParseNlItem> items = new ArrayList<>();
        for (ParseNlResponseDto.ParseNlItemDto dto : dtoItems) {
            long amount = dto.amount != null ? dto.amount : 0L;
            items.add(newParseNlItem(amount, dto.categoryId, dto.confidence));
        }
        return items;
    }

    //Tra category_id ra danh mục đầy đủ từ Room (Mục 12.1c); category_id null, không có hoặc đã bị xoá -> category = null
    private ParseNlItem newParseNlItem(long amount, @Nullable String categoryId, float confidence) {
        CategoryEntity category = categoryId != null ? categoryDao.getById(categoryId) : null;
        if (category != null && category.isDeleted) {
            category = null;
        }
        return new ParseNlItem(amount, category, confidence);
    }

    private Result<ForecastResult> mapForecastResponse(ForecastResponseDto dto) {
        if (STATUS_INSUFFICIENT_DATA.equals(dto.status)) {
            return new Result.Success<>(new ForecastResult(ForecastResult.Status.INSUFFICIENT_DATA, null));
        }
        if (dto.status != null && dto.forecastNetAmount != null) {
            return new Result.Success<>(new ForecastResult(ForecastResult.Status.OK, Math.round(dto.forecastNetAmount)));
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Phản hồi không hợp lệ từ máy chủ", null);
    }

    @Nullable
    private AnomalyResult.Level mapAnomalyLevel(@Nullable String status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case STATUS_HIGH:
                return AnomalyResult.Level.HIGH;
            case STATUS_LOW:
                return AnomalyResult.Level.LOW;
            case STATUS_NORMAL:
                return AnomalyResult.Level.NORMAL;
            case STATUS_INSUFFICIENT_DATA:
                return AnomalyResult.Level.INSUFFICIENT_DATA;
            default:
                return null; //status lạ -> coi là phản hồi không hợp lệ
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
    @Override
    public LiveData<Boolean> observeConnectivity() {
        return networkObserver.getIsConnected();
    }

    @Override
    public LiveData<Boolean> observeQuickAddEnabled() {
        return quickAddEnabled;
    }

    @Override
    public void setQuickAddEnabled(boolean enabled) {
        if (Boolean.valueOf(enabled).equals(quickAddEnabled.getValue())) {
            return; //không đổi thì không ghi lại
        }
        prefs().edit().putBoolean(KEY_QUICK_ADD_ENABLED, enabled).apply();
        quickAddEnabled.setValue(enabled);
    }

    @Override
    public LiveData<Boolean> observeGeminiFallbackEnabled() {
        return geminiFallbackEnabled;
    }

    //Lưu lựa chọn của người dùng; QuickAddNlViewModel đọc giá trị này mỗi lần gửi và truyền xuống parseNl() -> field allow_gemini
    @Override
    public void setGeminiFallbackEnabled(boolean enabled) {
        if (Boolean.valueOf(enabled).equals(geminiFallbackEnabled.getValue())) {
            return;
        }
        prefs().edit().putBoolean(KEY_GEMINI_FALLBACK_ENABLED, enabled).apply();
        geminiFallbackEnabled.setValue(enabled);
    }
}