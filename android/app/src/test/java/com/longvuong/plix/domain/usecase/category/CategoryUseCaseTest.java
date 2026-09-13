package com.longvuong.plix.domain.usecase.category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import org.junit.Test;

public class CategoryUseCaseTest {
    private final FakeCategoryRepository fakeRepository = new FakeCategoryRepository();
    private final CategoryUseCase useCase = new CategoryUseCase(fakeRepository);
    private CategoryEntity systemCategory(String name, String type) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = "sys_1";
        entity.userId = null;
        entity.name = name;
        entity.type = type;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private CategoryEntity customCategory(String userId) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = "cat-1";
        entity.userId = userId;
        entity.name = "Tiền điện";
        entity.type = "expense";
        entity.updatedAt = 1_000_000L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void addCategory_newNameNotDuplicateWithSystem_insertsAndReturnsSuccess() {
        CategoryEntity entity = customCategory("user-1");
        useCase.addCategory(entity, result -> assertTrue(result instanceof Result.Success));
        assertTrue(fakeRepository.insertCalled);
    }

    @Test
    public void addCategory_duplicateNameAndTypeWithSystemCategory_rejectsBeforeInsert() {
        fakeRepository.seedSystemCategory(systemCategory("Tiền điện", "expense"));
        CategoryEntity entity = customCategory("user-1");
        useCase.addCategory(entity, result -> assertTrue(result instanceof Result.Error));
        assertFalse(fakeRepository.insertCalled);
    }

    @Test
    public void addCategory_sameNameDifferentType_allowsInsert() {
        fakeRepository.seedSystemCategory(systemCategory("Tiền điện", "expense"));
        CategoryEntity entity = customCategory("user-1");
        entity.type = "income"; //khác loại nên không coi là trùng
        useCase.addCategory(entity, result -> assertTrue(result instanceof Result.Success));
        assertTrue(fakeRepository.insertCalled);
    }

    @Test
    public void updateCategory_ownedByCurrentUser_updatesAndReturnsSuccess() {
        CategoryEntity entity = customCategory("user-1");
        useCase.updateCategory(entity, "user-1", result -> assertTrue(result instanceof Result.Success));
        assertTrue(fakeRepository.updateCalled);
    }

    @Test
    public void updateCategory_systemCategory_rejectsBeforeUpdate() {
        CategoryEntity entity = systemCategory("Ăn uống", "expense");
        useCase.updateCategory(entity, "user-1", result -> assertTrue(result instanceof Result.Error));
        assertFalse(fakeRepository.updateCalled);
    }

    @Test
    public void updateCategory_ownedByAnotherUser_rejectsBeforeUpdate() {
        CategoryEntity entity = customCategory("user-2");
        useCase.updateCategory(entity, "user-1", result -> assertTrue(result instanceof Result.Error));
        assertFalse(fakeRepository.updateCalled);
    }

    @Test
    public void deleteCategory_ownedByCurrentUser_marksSoftDeleteAndUpdates() {
        CategoryEntity entity = customCategory("user-1");
        useCase.deleteCategory(entity, "user-1", result -> assertTrue(result instanceof Result.Success));
        assertTrue(fakeRepository.updateCalled);
        assertTrue(fakeRepository.lastUpdated.isDeleted);
        assertEquals("pending", fakeRepository.lastUpdated.syncStatus);
        assertTrue(fakeRepository.lastUpdated.updatedAt > 1_000_000L);
    }

    @Test
    public void deleteCategory_systemCategory_rejectsBeforeUpdate() {
        CategoryEntity entity = systemCategory("Ăn uống", "expense");
        useCase.deleteCategory(entity, "user-1", result -> assertTrue(result instanceof Result.Error));
        assertFalse(fakeRepository.updateCalled);
    }
}