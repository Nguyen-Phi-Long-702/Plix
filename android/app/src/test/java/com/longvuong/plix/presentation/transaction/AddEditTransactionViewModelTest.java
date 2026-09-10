package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.transaction.AddTransactionUseCase;
import com.longvuong.plix.domain.usecase.transaction.UpdateTransactionUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AddEditTransactionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeTransactionRepository fakeTransactionRepository;
    private FakeCategoryRepository fakeCategoryRepository;
    private FormValidator formValidator;
    private AuthManager authManager;

    @Before
    public void setUp() throws Exception {
        fakeTransactionRepository = new FakeTransactionRepository();
        fakeCategoryRepository = new FakeCategoryRepository(Arrays.asList(
                category("sys_an_uong", "Ăn uống", "expense"),
                category("sys_luong", "Lương", "income")
        ));
        formValidator = new FormValidator();
        authManager = new AuthManager();
        setFakeAccessToken(authManager, fakeJwtWithSub("user-1"));
    }

    private CategoryEntity category(String id, String name, String type) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.name = name;
        entity.type = type;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private static void setFakeAccessToken(AuthManager manager, String token) throws Exception {
        Field field = AuthManager.class.getDeclaredField("accessToken");
        field.setAccessible(true);
        field.set(manager, token);
    }

    private static String fakeJwtWithSub(String userId) {
        String payload = "{\"sub\":\"" + userId + "\"}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "header." + encodedPayload + ".signature";
    }

    private AddEditTransactionViewModel createViewModel(Map<String, Object> initialState) {
        SavedStateHandle savedStateHandle = new SavedStateHandle(initialState);
        AddTransactionUseCase addUseCase = new AddTransactionUseCase(fakeTransactionRepository, formValidator);
        UpdateTransactionUseCase updateUseCase = new UpdateTransactionUseCase(fakeTransactionRepository, formValidator);
        return new AddEditTransactionViewModel(savedStateHandle, fakeTransactionRepository,
                fakeCategoryRepository, addUseCase, updateUseCase, formValidator, authManager);
    }

    @Test
    public void addMode_initializesDefaultValues() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());

        assertEquals(0L, viewModel.getAmount());
        assertEquals("expense", viewModel.getType());
        assertNull(viewModel.getCategoryId());
        assertEquals("cash", viewModel.getPaymentMethod());
        assertEquals("", viewModel.getNote());
        assertTrue(Boolean.TRUE.equals(viewModel.getFormReady().getValue()));
        assertTrue(!viewModel.isEditMode());
    }

    @Test
    public void editMode_loadsExistingTransactionIntoDraftFields() {
        TransactionEntity existing = new TransactionEntity();
        existing.id = "tx-1";
        existing.userId = "user-1";
        existing.amount = 45000;
        existing.type = "expense";
        existing.categoryId = "sys_an_uong";
        existing.note = "Cà phê";
        existing.occurredAt = 1000L;
        existing.paymentMethod = "cash";
        existing.updatedAt = 1000L;
        existing.syncStatus = "synced";
        existing.isDeleted = false;
        fakeTransactionRepository.seed(existing);

        Map<String, Object> args = new HashMap<>();
        args.put(AddEditTransactionViewModel.ARG_TRANSACTION_ID, "tx-1");
        AddEditTransactionViewModel viewModel = createViewModel(args);

        assertTrue(viewModel.isEditMode());
        assertEquals(45000L, viewModel.getAmount());
        assertEquals("Cà phê", viewModel.getNote());
        assertEquals("sys_an_uong", viewModel.getCategoryId());
    }

    @Test
    public void save_invalidAmount_rejectsBeforeReachingRepository() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());
        viewModel.setAmount(0);
        viewModel.setCategoryId("sys_an_uong");

        viewModel.save();

        UiState<Void> state = viewModel.getSaveState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertTrue(!fakeTransactionRepository.insertCalled);
    }

    @Test
    public void save_validAmount_insertsSuccessfully() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());
        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");
        viewModel.setNote("Ăn trưa");

        viewModel.save();

        assertTrue(fakeTransactionRepository.insertCalled);
        assertEquals(45000L, fakeTransactionRepository.lastInserted.amount);
        UiState<Void> state = viewModel.getSaveState().getValue();
        assertTrue(state instanceof UiState.Success);
    }
}