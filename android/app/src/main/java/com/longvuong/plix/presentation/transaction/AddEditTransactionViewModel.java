package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.CategorySuggestion;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.domain.usecase.transaction.AnomalyCheckOutcome;
import com.longvuong.plix.domain.usecase.transaction.CheckTransactionAnomalyUseCase;
import com.longvuong.plix.domain.usecase.transaction.AddTransactionUseCase;
import com.longvuong.plix.domain.usecase.transaction.UpdateTransactionUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditTransactionViewModel extends ViewModel {
    public static final String ARG_TRANSACTION_ID = "transactionId";

    private static final String KEY_AMOUNT = "draft_amount";
    private static final String KEY_TYPE = "draft_type";
    private static final String KEY_CATEGORY_ID = "draft_category_id";
    private static final String KEY_OCCURRED_AT = "draft_occurred_at";
    private static final String KEY_PAYMENT_METHOD = "draft_payment_method";
    private static final String KEY_NOTE = "draft_note";
    private static final String KEY_LOADED = "draft_loaded";
    private static final long CATEGORIZE_DEBOUNCE_MS = 500;
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.4f;
    private static final String ANOMALY_TITLE_HIGH = "Chi tiêu cao bất thường";
    private static final String ANOMALY_TITLE_LOW = "Chi tiêu thấp bất thường";
    private static final String ANOMALY_LOW_REMINDER = " Chỉ để bạn lưu ý, không phải lỗi.";
    private final SavedStateHandle savedStateHandle;
    private final AddTransactionUseCase addTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final FormValidator formValidator;
    private final AuthManager authManager;
    private final AiRepository aiRepository;
    private final CheckTransactionAnomalyUseCase checkTransactionAnomalyUseCase;

    private final ScheduledExecutorService categorizeDebounceExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "categorize-debounce");
                thread.setDaemon(true);
                return thread;
            });
    private ScheduledFuture<?> pendingCategorizeTask;
    private volatile String lastCategorizedNote;
    private volatile String suggestedCategoryId;
    private volatile boolean suggestedCategoryLowConfidence;
    private final MediatorLiveData<UiState<CategorySuggestionUiModel>> categorySuggestionState = new MediatorLiveData<>();
    private final MutableLiveData<UiState<AnomalyBannerUiModel>> anomalyBannerState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> closeScreen = new MutableLiveData<>(false);

    private final String editingTransactionId;
    private TransactionEntity loadedEntity;

    private List<CategoryEntity> allCategories = new ArrayList<>();

    private final MutableLiveData<Boolean> formReady = new MutableLiveData<>(false);
    private final MutableLiveData<UiState<Void>> saveState = new MutableLiveData<>();
    private final MediatorLiveData<List<CategoryEntity>> filteredCategories = new MediatorLiveData<>();

    @Inject
    public AddEditTransactionViewModel(
            SavedStateHandle savedStateHandle,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            AddTransactionUseCase addTransactionUseCase,
            UpdateTransactionUseCase updateTransactionUseCase,
            FormValidator formValidator,
            AuthManager authManager,
            AiRepository aiRepository,
            CheckTransactionAnomalyUseCase checkTransactionAnomalyUseCase) {
        this.savedStateHandle = savedStateHandle;
        this.addTransactionUseCase = addTransactionUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.formValidator = formValidator;
        this.authManager = authManager;
        this.aiRepository = aiRepository;
        this.checkTransactionAnomalyUseCase = checkTransactionAnomalyUseCase;
        this.editingTransactionId = savedStateHandle.get(ARG_TRANSACTION_ID);

        Boolean alreadyLoaded = savedStateHandle.get(KEY_LOADED);
        if (Boolean.TRUE.equals(alreadyLoaded)) {
            formReady.setValue(true);
        } else if (editingTransactionId == null) {
            initDefaultsForAdd();
        } else {
            loadExistingTransaction(transactionRepository, editingTransactionId);
        }

        filteredCategories.addSource(categoryRepository.getActiveCategories(), categories -> {
            allCategories = categories != null ? categories : new ArrayList<>();
            updateFilteredCategories();
        });
        categorySuggestionState.setValue(new UiState.Empty<>());
        anomalyBannerState.setValue(new UiState.Empty<>());
        categorySuggestionState.addSource(aiRepository.observeConnectivity(), isConnected -> {
            if (Boolean.FALSE.equals(isConnected)) {
                hideSuggestionDueToOffline(); //mất mạng -> ẩn hẳn khối gợi ý
            }
        });
    }

    private void initDefaultsForAdd() {
        savedStateHandle.set(KEY_AMOUNT, 0L);
        savedStateHandle.set(KEY_TYPE, "expense");
        savedStateHandle.set(KEY_CATEGORY_ID, (String) null);
        savedStateHandle.set(KEY_OCCURRED_AT, System.currentTimeMillis());
        savedStateHandle.set(KEY_PAYMENT_METHOD, "cash");
        savedStateHandle.set(KEY_NOTE, "");
        savedStateHandle.set(KEY_LOADED, true);
        formReady.setValue(true);
    }

    private void loadExistingTransaction(TransactionRepository transactionRepository, String id) {
        transactionRepository.getById(id, result -> {
            if (result instanceof Result.Success) {
                TransactionEntity entity = ((Result.Success<TransactionEntity>) result).data;
                if (entity == null) {
                    saveState.setValue(new UiState.Error<>("Không tìm thấy giao dịch cần sửa"));
                    return;
                }
                loadedEntity = entity;
                savedStateHandle.set(KEY_AMOUNT, entity.amount);
                savedStateHandle.set(KEY_TYPE, entity.type);
                savedStateHandle.set(KEY_CATEGORY_ID, entity.categoryId);
                savedStateHandle.set(KEY_OCCURRED_AT, entity.occurredAt);
                savedStateHandle.set(KEY_PAYMENT_METHOD, entity.paymentMethod);
                savedStateHandle.set(KEY_NOTE, entity.note != null ? entity.note : "");
                savedStateHandle.set(KEY_LOADED, true);
                formReady.setValue(true);
                updateFilteredCategories();
            } else {
                Result.Error<TransactionEntity> error = (Result.Error<TransactionEntity>) result;
                saveState.setValue(new UiState.Error<>(error.message));
            }
        });
    }

    private void updateFilteredCategories() {
        String currentType = getType();
        List<CategoryEntity> filtered = new ArrayList<>();
        for (CategoryEntity category : allCategories) {
            if (category.type.equals(currentType)) {
                filtered.add(category);
            }
        }
        filteredCategories.setValue(filtered);
    }

    public boolean isEditMode() {
        return editingTransactionId != null;
    }

    public LiveData<Boolean> getFormReady() {
        return formReady;
    }

    public LiveData<List<CategoryEntity>> getFilteredCategories() {
        return filteredCategories;
    }

    public LiveData<UiState<Void>> getSaveState() {
        return saveState;
    }

    public long getAmount() {
        Long value = savedStateHandle.get(KEY_AMOUNT);
        return value != null ? value : 0L;
    }

    public void setAmount(long amount) {
        savedStateHandle.set(KEY_AMOUNT, amount);
    }

    public String getType() {
        String value = savedStateHandle.get(KEY_TYPE);
        return value != null ? value : "expense";
    }

    public void setType(String type) {
        savedStateHandle.set(KEY_TYPE, type);
        String currentCategoryId = getCategoryId();
        if (currentCategoryId != null) {
            boolean stillValid = false;
            for (CategoryEntity category : allCategories) {
                if (category.id.equals(currentCategoryId) && category.type.equals(type)) {
                    stillValid = true;
                    break;
                }
            }
            if (!stillValid) {
                setCategoryId(null);
            }
        }
        updateFilteredCategories();
    }

    @Nullable
    public String getCategoryId() {
        return savedStateHandle.get(KEY_CATEGORY_ID);
    }

    public void setCategoryId(@Nullable String categoryId) {
        savedStateHandle.set(KEY_CATEGORY_ID, categoryId);
    }

    public long getOccurredAt() {
        Long value = savedStateHandle.get(KEY_OCCURRED_AT);
        return value != null ? value : System.currentTimeMillis();
    }

    public void setOccurredAt(long occurredAt) {
        savedStateHandle.set(KEY_OCCURRED_AT, occurredAt);
    }

    @Nullable
    public String getPaymentMethod() {
        return savedStateHandle.get(KEY_PAYMENT_METHOD);
    }

    public void setPaymentMethod(@Nullable String paymentMethod) {
        savedStateHandle.set(KEY_PAYMENT_METHOD, paymentMethod);
    }

    public String getNote() {
        String value = savedStateHandle.get(KEY_NOTE);
        return value != null ? value : "";
    }

    public void setNote(String note) {
        savedStateHandle.set(KEY_NOTE, note);
        scheduleCategorize(note);
    }

    public Result<Void> validateAmountField(long amount) {
        return formValidator.validateAmount(amount);
    }

    public Result<Void> validateNoteField(String note) {
        return formValidator.validateNote(note);
    }

    public Result<Void> validateOccurredAtField(long occurredAt) {
        return formValidator.validateOccurredAt(occurredAt);
    }

    public void save() {
        String userId = authManager.getCurrentUserId();
        if (userId == null && !isEditMode()) {
            saveState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }

        String predictedCategoryIdSnapshot = suggestedCategoryId;

        TransactionEntity entity;
        if (isEditMode() && loadedEntity != null) {
            entity = loadedEntity;
        } else {
            entity = new TransactionEntity();
            entity.id = UUID.randomUUID().toString();
            entity.userId = userId;
            entity.isRecurring = false;
            entity.recurrenceRule = null;
            entity.recurrenceParentId = null;
            entity.isDeleted = false;
            entity.syncStatus = "pending";
            entity.updatedAt = System.currentTimeMillis();
        }

        entity.amount = getAmount();
        entity.type = getType();
        entity.categoryId = getCategoryId();
        entity.occurredAt = getOccurredAt();
        entity.paymentMethod = getPaymentMethod();
        entity.note = getNote();

        String transactionIdForCorrection = entity.id;
        String correctedCategoryIdForCorrection = entity.categoryId;
        String savedUserId = entity.userId;
        String savedType = entity.type;
        long savedAmount = entity.amount;

        saveState.setValue(new UiState.Loading<>());
        RepositoryCallback<Void> callback = result -> {
            saveState.setValue(toUiState(result));
            if (result instanceof Result.Success) {
                reportCorrectionIfNeeded(transactionIdForCorrection, predictedCategoryIdSnapshot, correctedCategoryIdForCorrection);
                startAnomalyCheckOrClose(transactionIdForCorrection, savedUserId, savedType, correctedCategoryIdForCorrection, savedAmount);
            }
        };

        if (isEditMode()) {
            updateTransactionUseCase.execute(entity, callback);
        } else {
            addTransactionUseCase.execute(entity, callback);
        }
    }

    private void reportCorrectionIfNeeded(String transactionId, @Nullable String predictedCategoryId, @Nullable String correctedCategoryId) {
        if (predictedCategoryId == null || correctedCategoryId == null || predictedCategoryId.equals(correctedCategoryId)) {
            return; //Không có gợi ý AI cho note hiện tại, hoặc user không sửa lại gợi ý -> không phải correction
        }
        aiRepository.submitCorrection(transactionId, predictedCategoryId, correctedCategoryId, result -> {
            //Best-effort, không cập nhật UI: đây là tín hiệu học cho AI, không ảnh hưởng tới giao dịch đã lưu thành công
        });
    }


    public LiveData<UiState<AnomalyBannerUiModel>> getAnomalyBannerState() {
        return anomalyBannerState;
    }

    public LiveData<Boolean> getCloseScreen() {
        return closeScreen;
    }

    private void startAnomalyCheckOrClose(String transactionId, @Nullable String userId, String type,
                                          @Nullable String categoryId, long amount) {
        boolean applicable = "expense".equals(type) && categoryId != null && userId != null;
        if (!applicable || isOffline()) {
            closeScreen.setValue(true); //thu nhập / chưa có danh mục / mất mạng -> không kiểm tra, đóng như cũ
            return;
        }
        anomalyBannerState.setValue(new UiState.Loading<>());
        checkTransactionAnomalyUseCase.execute(userId, transactionId, categoryId, amount, result -> {
            if (result instanceof Result.Success) {
                AnomalyCheckOutcome outcome = ((Result.Success<AnomalyCheckOutcome>) result).data;
                AnomalyResult.Level level = outcome.result.level;
                if (level == AnomalyResult.Level.HIGH || level == AnomalyResult.Level.LOW) {
                    anomalyBannerState.setValue(new UiState.Success<>(buildAnomalyBanner(outcome)));
                    return; //ở lại màn hình để người dùng đọc cảnh báo
                }
            }
            //Bình thường / chưa đủ dữ liệu / lỗi: giao dịch đã lưu xong, lỗi AI không được chặn luồng -> đóng như cũ
            anomalyBannerState.setValue(new UiState.Empty<>());
            closeScreen.setValue(true);
        });
    }

    private AnomalyBannerUiModel buildAnomalyBanner(AnomalyCheckOutcome outcome) {
        boolean high = outcome.result.level == AnomalyResult.Level.HIGH;
        String explanation = outcome.result.explanation != null ? outcome.result.explanation : "";
        if (!high) {
            explanation = (explanation + ANOMALY_LOW_REMINDER).trim();
        }
        return new AnomalyBannerUiModel(high, high ? ANOMALY_TITLE_HIGH : ANOMALY_TITLE_LOW,
                explanation, outcome.hasOtherPendingInCategory);
    }

    public LiveData<UiState<CategorySuggestionUiModel>> getCategorySuggestionState() {
        return categorySuggestionState;
    }

    public void retryCategorize() {
        if (lastCategorizedNote == null) {
            return;
        }
        if (isOffline()) {
            hideSuggestionDueToOffline(); //AI-10: đang mất mạng thì không thử gọi lại API
            return;
        }
        requestCategorize(lastCategorizedNote);
    }
    public void applySuggestedCategory() {
        String id = suggestedCategoryId;
        if (id == null || suggestedCategoryLowConfidence) {
            return; //độ tin cậy thấp -> không tự động điền danh mục, để user chọn tay
        }
        String currentType = getType();
        for (CategoryEntity category : allCategories) {
            if (category.id.equals(id) && category.type.equals(currentType)) {
                setCategoryId(id);
                break;
            }
        }
    }

    private void scheduleCategorize(String note) {
        if (pendingCategorizeTask != null) {
            pendingCategorizeTask.cancel(false);
        }
        aiRepository.cancelPendingCategorize();

        String trimmed = note == null ? "" : note.trim();
        if (trimmed.isEmpty()) {
            suggestedCategoryId = null;
            suggestedCategoryLowConfidence = false;
            categorySuggestionState.postValue(new UiState.Empty<>());
            return;
        }
        if (isOffline()) {
            hideSuggestionDueToOffline(); //AI-10: mất mạng -> không gọi AI, không lỗi, cho nhập tay
            return;
        }
        pendingCategorizeTask = categorizeDebounceExecutor.schedule(
                () -> requestCategorize(trimmed), CATEGORIZE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private boolean isOffline() {
        return Boolean.FALSE.equals(aiRepository.observeConnectivity().getValue());
    }

    private void hideSuggestionDueToOffline() {
        if (pendingCategorizeTask != null) {
            pendingCategorizeTask.cancel(false);
        }
        aiRepository.cancelPendingCategorize();
        suggestedCategoryId = null;
        suggestedCategoryLowConfidence = false;
        categorySuggestionState.setValue(new UiState.Empty<>());
    }

    private void requestCategorize(String note) {
        lastCategorizedNote = note;
        suggestedCategoryId = null;
        suggestedCategoryLowConfidence = false;
        categorySuggestionState.postValue(new UiState.Loading<>());
        aiRepository.categorize(note, result -> {
            if (!note.equals(lastCategorizedNote)) {
                return; //Đã có note mới hơn được gõ trong lúc chờ phản hồi - bỏ kết quả cũ này
            }
            if (result instanceof Result.Success) {
                CategorySuggestion suggestion = ((Result.Success<CategorySuggestion>) result).data;
                boolean lowConfidence = suggestion.confidence < LOW_CONFIDENCE_THRESHOLD;
                suggestedCategoryId = suggestion.categoryId; //vẫn giữ id gốc để báo correction nếu user tự chọn khác, dù không auto-fill
                suggestedCategoryLowConfidence = lowConfidence;
                categorySuggestionState.postValue(new UiState.Success<>(
                        new CategorySuggestionUiModel(formatSuggestionLabel(suggestion, lowConfidence), lowConfidence)));
            } else {
                categorySuggestionState.postValue(new UiState.Error<>("Không lấy được gợi ý, nhập tay"));
            }
        });
    }

    private String formatSuggestionLabel(CategorySuggestion suggestion, boolean lowConfidence) {
        int confidencePercent = Math.round(suggestion.confidence * 100);
        if (lowConfidence) {
            return "Độ tin cậy thấp · " + confidencePercent + "%"; //confidence<0.4 -> đổi hẳn nhãn, không hiện tên category đoán được
        }
        String categoryLabel = suggestion.category != null ? suggestion.category.name : "Danh mục không xác định";
        return categoryLabel + " · " + confidencePercent + "%";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingCategorizeTask != null) {
            pendingCategorizeTask.cancel(false);
        }
        aiRepository.cancelPendingCategorize();
        aiRepository.cancelPendingAnomalyCheck();
        categorizeDebounceExecutor.shutdownNow();
    }
    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}