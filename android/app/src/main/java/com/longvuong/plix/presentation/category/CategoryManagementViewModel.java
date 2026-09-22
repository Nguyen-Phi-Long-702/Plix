package com.longvuong.plix.presentation.category;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.domain.usecase.category.CategoryUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CategoryManagementViewModel extends ViewModel {
    private final CategoryUseCase categoryUseCase;
    private final FormValidator formValidator;
    private final AuthManager authManager;
    private final MediatorLiveData<UiState<List<CategoryEntity>>> categoryListState = new MediatorLiveData<>();
    private final MutableLiveData<UiState<Void>> formState = new MutableLiveData<>();
    @Inject
    public CategoryManagementViewModel(CategoryRepository categoryRepository,
                                       CategoryUseCase categoryUseCase,
                                       FormValidator formValidator,
                                       AuthManager authManager) {
        this.categoryUseCase = categoryUseCase;
        this.formValidator = formValidator;
        this.authManager = authManager;
        categoryListState.setValue(new UiState.Loading<>());
        categoryListState.addSource(categoryRepository.getActiveCategories(), categories -> {
            if (categories == null || categories.isEmpty()) {
                categoryListState.setValue(new UiState.Empty<>());
            } else {
                categoryListState.setValue(new UiState.Success<>(categories));
            }
        });
    }

    public LiveData<UiState<List<CategoryEntity>>> getCategoryListState() {
        return categoryListState;
    }

    public LiveData<UiState<Void>> getFormState() {
        return formState;
    }

    @Nullable
    public String getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    public Result<Void> validateNameField(String name) {
        return formValidator.validateCategoryName(name);
    }

    public void addCategory(String name, String type) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            formState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }
        CategoryEntity entity = new CategoryEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.name = name != null ? name.trim() : "";
        entity.type = type;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        categoryUseCase.addCategory(entity, result -> formState.setValue(toUiState(result)));
    }

    public void updateCategory(CategoryEntity existing, String newName, String newType) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            formState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }
        existing.name = newName != null ? newName.trim() : "";
        existing.type = newType;
        existing.updatedAt = System.currentTimeMillis();
        existing.syncStatus = "pending";
        categoryUseCase.updateCategory(existing, userId, result -> formState.setValue(toUiState(result)));
    }

    public void deleteCategory(CategoryEntity existing) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            formState.setValue(new UiState.Error<>("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại"));
            return;
        }
        categoryUseCase.deleteCategory(existing, userId, result -> formState.setValue(toUiState(result)));
    }

    private UiState<Void> toUiState(Result<Void> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(null);
        }
        Result.Error<Void> error = (Result.Error<Void>) result;
        return new UiState.Error<>(error.message);
    }
}