package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.AnomalyResult;
import com.longvuong.plix.domain.usecase.transaction.CheckTransactionAnomalyUseCase;
import com.longvuong.plix.domain.usecase.transaction.AddTransactionUseCase;
import com.longvuong.plix.domain.usecase.transaction.UpdateTransactionUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;
import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.CategorySuggestion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

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
        authManager = new AuthManager(null);
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
        return createViewModel(initialState, new FakeAiRepository());
    }

    private AddEditTransactionViewModel createViewModel(Map<String, Object> initialState, FakeAiRepository aiRepository) {
        SavedStateHandle savedStateHandle = new SavedStateHandle(initialState);
        AddTransactionUseCase addUseCase = new AddTransactionUseCase(
                fakeTransactionRepository, formValidator, fakeCheckBudgetThresholdUseCase());
        UpdateTransactionUseCase updateUseCase = new UpdateTransactionUseCase(
                fakeTransactionRepository, formValidator, fakeCheckBudgetThresholdUseCase());
        return new AddEditTransactionViewModel(savedStateHandle, fakeTransactionRepository,
                fakeCategoryRepository, addUseCase, updateUseCase, formValidator, authManager,
                aiRepository, new CheckTransactionAnomalyUseCase(aiRepository, fakeTransactionRepository));
    }

    private com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase fakeCheckBudgetThresholdUseCase() {
        return new com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase(
                new FakeBudgetRepository(),
                fakeTransactionRepository,
                new com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase(),
                new com.longvuong.plix.domain.usecase.budget.BudgetThresholdChecker(),
                (budget, spentAfterAmount) -> { });
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
    @Test
    public void categorize_beforeResponseArrives_showsLoading() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);

        UiState<CategorySuggestionUiModel> state = viewModel.getCategorySuggestionState().getValue();
        assertTrue(state instanceof UiState.Loading);
    }

    @Test
    public void categorize_successResult_showsCategoryLabelWithConfidence() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Success<>(
                new CategorySuggestion("sys_an_uong", category("sys_an_uong", "Ăn uống", "expense"), 0.62f)));

        UiState<CategorySuggestionUiModel> state = viewModel.getCategorySuggestionState().getValue();
        assertTrue(state instanceof UiState.Success);
        CategorySuggestionUiModel model = ((UiState.Success<CategorySuggestionUiModel>) state).data;
        assertEquals("Ăn uống · 62%", model.label);
        assertTrue(!model.lowConfidence);
    }

    @Test
    public void categorize_lowConfidenceResult_showsLowConfidenceLabel() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("khong biet");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Success<>(new CategorySuggestion(null, null, 0.10f)));

        UiState<CategorySuggestionUiModel> state = viewModel.getCategorySuggestionState().getValue();
        assertTrue(state instanceof UiState.Success);
        CategorySuggestionUiModel model = ((UiState.Success<CategorySuggestionUiModel>) state).data;
        assertEquals("Độ tin cậy thấp · 10%", model.label);
        assertTrue(model.lowConfidence);
    }

    @Test
    public void categorize_errorResult_showsErrorState() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Error<>(ErrorType.NETWORK, "Mat ket noi trong luc goi API", null));

        UiState<CategorySuggestionUiModel> state = viewModel.getCategorySuggestionState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không lấy được gợi ý, nhập tay", ((UiState.Error<CategorySuggestionUiModel>) state).message);
    }

    @Test
    public void connectivityLost_hidesSuggestionBlockEvenIfSuggestionWasShowing() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Success<>(
                new CategorySuggestion("sys_an_uong", category("sys_an_uong", "Ăn uống", "expense"), 0.62f)));
        assertTrue(viewModel.getCategorySuggestionState().getValue() instanceof UiState.Success);

        fakeAiRepository.setConnected(false);

        assertTrue(viewModel.getCategorySuggestionState().getValue() instanceof UiState.Empty);
    }

    private AddEditTransactionViewModel createReadyToSaveExpense(FakeAiRepository fakeAiRepository) {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.setAmount(350000);
        viewModel.setCategoryId("sys_an_uong");
        return viewModel;
    }

    @Test
    public void save_anomalyHigh_showsBannerAndKeepsScreenOpen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();

        assertTrue(fakeAiRepository.checkAnomalyCalled);
        assertEquals("sys_an_uong", fakeAiRepository.lastAnomalyCategoryId);
        assertEquals(350000L, fakeAiRepository.lastAnomalyAmount);
        assertTrue(viewModel.getAnomalyBannerState().getValue() instanceof UiState.Loading);
        assertEquals(Boolean.FALSE, viewModel.getCloseScreen().getValue());

        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.HIGH, "Cao hơn mức trung bình")));

        UiState<AnomalyBannerUiModel> state = viewModel.getAnomalyBannerState().getValue();
        assertTrue(state instanceof UiState.Success);
        AnomalyBannerUiModel banner = ((UiState.Success<AnomalyBannerUiModel>) state).data;
        assertTrue(banner.high);
        assertEquals("Chi tiêu cao bất thường", banner.title);
        assertEquals("Cao hơn mức trung bình", banner.explanation);
        assertTrue(!banner.showPendingNote);
        assertEquals(Boolean.FALSE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_anomalyLow_showsLowBannerWithReminder() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.LOW, "Thấp hơn mức trung bình.")));

        AnomalyBannerUiModel banner = ((UiState.Success<AnomalyBannerUiModel>) viewModel.getAnomalyBannerState().getValue()).data;
        assertTrue(!banner.high);
        assertEquals("Chi tiêu thấp bất thường", banner.title);
        assertEquals("Thấp hơn mức trung bình. Chỉ để bạn lưu ý, không phải lỗi.", banner.explanation);
    }

    @Test
    public void save_anomalyWithOtherPending_showsPendingNote() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository.countPendingResult = new Result.Success<>(2);
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.HIGH, "Cao")));

        AnomalyBannerUiModel banner = ((UiState.Success<AnomalyBannerUiModel>) viewModel.getAnomalyBannerState().getValue()).data;
        assertTrue(banner.showPendingNote);
    }

    @Test
    public void save_anomalyNormal_closesScreenWithoutBanner() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.NORMAL, null)));

        assertTrue(viewModel.getAnomalyBannerState().getValue() instanceof UiState.Empty);
        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_anomalyApiError_closesScreenSilently() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Error<>(ErrorType.NETWORK, "Mat mang", null));

        assertTrue(viewModel.getAnomalyBannerState().getValue() instanceof UiState.Empty);
        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_offline_skipsAnomalyCheckAndClosesScreen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        fakeAiRepository.setConnected(false);
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();

        assertTrue(!fakeAiRepository.checkAnomalyCalled);
        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_income_skipsAnomalyCheckAndClosesScreen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.setType("income");
        viewModel.setAmount(10000000);
        viewModel.setCategoryId("sys_luong");

        viewModel.save();

        assertTrue(!fakeAiRepository.checkAnomalyCalled);
        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_noCategory_skipsAnomalyCheckAndClosesScreen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.setAmount(350000);

        viewModel.save();

        assertTrue(!fakeAiRepository.checkAnomalyCalled);
        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    private static void waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }
}