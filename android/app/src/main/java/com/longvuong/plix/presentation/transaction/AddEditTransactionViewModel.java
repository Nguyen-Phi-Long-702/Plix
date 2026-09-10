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
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.transaction.AddTransactionUseCase;
import com.longvuong.plix.domain.usecase.transaction.UpdateTransactionUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private final SavedStateHandle savedStateHandle;
    private final AddTransactionUseCase addTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final FormValidator formValidator;
    private final AuthManager authManager;

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
            AuthManager authManager) {
        this.savedStateHandle = savedStateHandle;
        this.addTransactionUseCase = addTransactionUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.formValidator = formValidator;
        this.authManager = authManager;

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

        saveState.setValue(new UiState.Loading<>());
        RepositoryCallback<Void> callback = result -> saveState.setValue(toUiState(result));

        if (isEditMode()) {
            updateTransactionUseCase.execute(entity, callback);
        } else {
            addTransactionUseCase.execute(entity, callback);
        }
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}