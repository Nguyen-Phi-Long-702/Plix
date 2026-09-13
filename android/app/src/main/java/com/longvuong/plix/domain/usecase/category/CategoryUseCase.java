package com.longvuong.plix.domain.usecase.category;

import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.repository.CategoryRepository;

import javax.inject.Inject;

public class CategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Inject
    public CategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void addCategory(CategoryEntity entity, RepositoryCallback<Void> callback) {
        categoryRepository.findSystemCategoryByNameAndType(entity.name, entity.type, result -> {
            if (result instanceof Result.Error) {
                callback.onResult(propagateError((Result.Error<CategoryEntity>) result));
                return;
            }
            CategoryEntity existingSystemCategory = ((Result.Success<CategoryEntity>) result).data;
            if (existingSystemCategory != null) {
                callback.onResult(new Result.Error<>(ErrorType.VALIDATION,
                        "Tên danh mục đã tồn tại trong danh mục hệ thống, vui lòng chọn tên khác", null));
                return;
            }
            categoryRepository.insert(entity, callback);
        });
    }

    public void updateCategory(CategoryEntity entity, String currentUserId, RepositoryCallback<Void> callback) {
        Result<Void> ownership = validateOwnership(entity, currentUserId);
        if (ownership instanceof Result.Error) {
            callback.onResult(ownership);
            return;
        }
        categoryRepository.update(entity, callback);
    }

   public void deleteCategory(CategoryEntity entity, String currentUserId, RepositoryCallback<Void> callback) {
        Result<Void> ownership = validateOwnership(entity, currentUserId);
        if (ownership instanceof Result.Error) {
            callback.onResult(ownership);
            return;
        }
        entity.isDeleted = true;
        entity.updatedAt = System.currentTimeMillis();
        entity.syncStatus = "pending";
        categoryRepository.update(entity, callback);
    }

    private Result<Void> validateOwnership(CategoryEntity entity, String currentUserId) {
        if (entity.userId == null) {
            return new Result.Error<>(ErrorType.VALIDATION,
                    "Không thể chỉnh sửa hoặc xoá danh mục hệ thống", null);
        }
        if (!entity.userId.equals(currentUserId)) {
            return new Result.Error<>(ErrorType.VALIDATION,
                    "Bạn không có quyền chỉnh sửa hoặc xoá danh mục này", null);
        }
        return new Result.Success<>(null);
    }

    private static <T, R> Result<R> propagateError(Result.Error<T> error) {
        return new Result.Error<>(error.type, error.message, error.cause);
    }
}