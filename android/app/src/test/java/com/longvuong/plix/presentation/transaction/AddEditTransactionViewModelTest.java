package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;

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
import com.longvuong.plix.domain.usecase.transaction.FindSimilarTransactionUseCase;
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
    private static final String INSUFFICIENT_DATA_MESSAGE = "Danh mục này chưa có đủ giao dịch để Plix nhận diện bất thường (cần tối thiểu 5 giao dịch). Cứ tiếp tục ghi chép — tính năng sẽ tự bật khi có đủ dữ liệu.";
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
                aiRepository, new CheckTransactionAnomalyUseCase(aiRepository, fakeTransactionRepository),
                new FindSimilarTransactionUseCase(fakeTransactionRepository));
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
        assertEquals(AnomalyBannerUiModel.Kind.HIGH, banner.kind);
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
        assertEquals(AnomalyBannerUiModel.Kind.LOW, banner.kind);
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
    public void save_anomalyInsufficientData_showsPositiveInfoBannerAndKeepsScreenOpen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.INSUFFICIENT_DATA, null)));

        UiState<AnomalyBannerUiModel> state = viewModel.getAnomalyBannerState().getValue();
        assertTrue(state instanceof UiState.Success); //không phải UiState.Error: chưa đủ dữ liệu không phải là lỗi
        AnomalyBannerUiModel banner = ((UiState.Success<AnomalyBannerUiModel>) state).data;
        assertEquals(AnomalyBannerUiModel.Kind.INSUFFICIENT_DATA, banner.kind);
        assertEquals(INSUFFICIENT_DATA_MESSAGE, banner.explanation);
        assertTrue(!banner.showPendingNote);
        assertEquals(Boolean.FALSE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void save_anomalyInsufficientData_usesFixedMessageEvenWhenServerSendsExplanation() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository.countPendingResult = new Result.Success<>(2);
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);

        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.INSUFFICIENT_DATA, "Not enough data")));

        AnomalyBannerUiModel banner = ((UiState.Success<AnomalyBannerUiModel>) viewModel.getAnomalyBannerState().getValue()).data;
        assertEquals(INSUFFICIENT_DATA_MESSAGE, banner.explanation);
        assertTrue(!banner.showPendingNote);
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

    @Test
    public void finishAfterSave_afterInsufficientDataBanner_closesScreen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);
        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.INSUFFICIENT_DATA, null)));
        assertEquals(Boolean.FALSE, viewModel.getCloseScreen().getValue()); //đang ở lại để người dùng đọc

        viewModel.finishAfterSave();

        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    @Test
    public void finishAfterSave_afterHighAnomalyBanner_closesScreen() {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createReadyToSaveExpense(fakeAiRepository);
        viewModel.save();
        fakeAiRepository.completeAnomaly(new Result.Success<>(
                new AnomalyResult(AnomalyResult.Level.HIGH, "Cao")));
        assertEquals(Boolean.FALSE, viewModel.getCloseScreen().getValue());

        viewModel.finishAfterSave();

        assertEquals(Boolean.TRUE, viewModel.getCloseScreen().getValue());
    }

    private TransactionEntity existingTransaction(String id) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = 3200000;
        entity.type = "expense";
        entity.categoryId = "sys_an_uong";
        entity.note = "Tiền nhà";
        entity.occurredAt = System.currentTimeMillis();
        entity.paymentMethod = "bank_transfer";
        entity.updatedAt = 1000L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        fakeTransactionRepository.seed(entity);
        return entity;
    }

    private AddEditTransactionViewModel createEditViewModel(String transactionId) {
        Map<String, Object> args = new HashMap<>();
        args.put(AddEditTransactionViewModel.ARG_TRANSACTION_ID, transactionId);
        return createViewModel(args);
    }

    @Test
    public void addMode_recurringOffByDefault_withTodayAsRecurringDay() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());

        assertTrue(!viewModel.isRecurring());
        assertEquals(LocalDate.now().getDayOfMonth(), viewModel.getRecurringDay());
        assertTrue(!viewModel.isAutoGenerated());
    }

    @Test
    public void save_recurringOn_savesTemplateWithMonthlyRule() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());
        viewModel.setAmount(3200000);
        viewModel.setCategoryId("sys_an_uong");
        viewModel.setRecurring(true);
        viewModel.setRecurringDay(15);

        viewModel.save();

        assertTrue(fakeTransactionRepository.lastInserted.isRecurring);
        assertEquals("MONTHLY:15", fakeTransactionRepository.lastInserted.recurrenceRule);
        assertNull(fakeTransactionRepository.lastInserted.recurrenceParentId);
    }

    @Test
    public void save_recurringOff_savesRegularTransaction() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());
        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");

        viewModel.save();

        assertTrue(!fakeTransactionRepository.lastInserted.isRecurring);
        assertNull(fakeTransactionRepository.lastInserted.recurrenceRule);
    }

    @Test
    public void editMode_recurringTemplate_loadsSwitchAndDay() {
        TransactionEntity template = existingTransaction("tx-template");
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:20";

        AddEditTransactionViewModel viewModel = createEditViewModel("tx-template");

        assertTrue(viewModel.isRecurring());
        assertEquals(20, viewModel.getRecurringDay());
        assertTrue(!viewModel.isAutoGenerated());
    }

    @Test
    public void editMode_templateTurnedOff_clearsRecurrence() {
        TransactionEntity template = existingTransaction("tx-template");
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:20";
        AddEditTransactionViewModel viewModel = createEditViewModel("tx-template");

        viewModel.setRecurring(false);
        viewModel.save();

        assertTrue(!template.isRecurring);
        assertNull(template.recurrenceRule);
    }

    @Test
    public void editMode_autoGeneratedInstance_exposesSourceNoteAndKeepsRecurrenceFields() {
        TransactionEntity template = existingTransaction("tx-template");
        template.note = "Tiền nhà tháng 9";
        template.isRecurring = true;
        template.recurrenceRule = "MONTHLY:1";
        TransactionEntity instance = existingTransaction("tx-instance");
        instance.recurrenceParentId = "tx-template";

        AddEditTransactionViewModel viewModel = createEditViewModel("tx-instance");

        assertTrue(viewModel.isAutoGenerated());
        assertEquals("Tiền nhà tháng 9", viewModel.getAutoGeneratedSourceNote().getValue());

        viewModel.setRecurring(true);
        viewModel.save();

        assertTrue(!instance.isRecurring);
        assertNull(instance.recurrenceRule);
        assertEquals("tx-template", instance.recurrenceParentId);
    }

    private AddEditTransactionViewModel createAddViewModelWithCategoriesLoaded() {
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>());
        viewModel.getFilteredCategories().observeForever(categories -> { }); //để ViewModel nạp danh sách danh mục (lấy tên danh mục cho cảnh báo)
        return viewModel;
    }

    private TransactionEntity similarTransactionAt(long occurredAt) {
        TransactionEntity similar = new TransactionEntity();
        similar.id = "tx-similar";
        similar.userId = "user-1";
        similar.amount = 45000;
        similar.type = "expense";
        similar.categoryId = "sys_an_uong";
        similar.occurredAt = occurredAt;
        return similar;
    }

    @Test
    public void similar_sameAmountAndCategory_showsWarningWithDetails() {
        fakeTransactionRepository.findSimilarResult =
                new Result.Success<>(similarTransactionAt(System.currentTimeMillis()));
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();

        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");

        UiState<SimilarWarningUiModel> state = viewModel.getSimilarWarningState().getValue();
        assertTrue(state instanceof UiState.Success);
        SimilarWarningUiModel model = ((UiState.Success<SimilarWarningUiModel>) state).data;
        assertEquals("Giao dịch tương tự đã tồn tại", model.title);
        assertTrue(model.detail.contains("45.000đ"));
        assertTrue(model.detail.contains("Ăn uống"));
        assertTrue(model.detail.contains("hôm nay"));
    }

    @Test
    public void similar_onAnotherDay_mentionsTheDate() {
        long threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000;
        fakeTransactionRepository.findSimilarResult = new Result.Success<>(similarTransactionAt(threeDaysAgo));
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();

        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");

        SimilarWarningUiModel model = ((UiState.Success<SimilarWarningUiModel>)
                viewModel.getSimilarWarningState().getValue()).data;
        assertTrue(model.detail.contains("ngày "));
        assertTrue(!model.detail.contains("hôm nay"));
    }

    @Test
    public void similar_noMatch_showsNothing() {
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();

        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");

        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Empty);
        assertEquals(1, fakeTransactionRepository.findSimilarCallCount);
    }

    @Test
    public void similar_withoutCategory_doesNotQuery() {
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();

        viewModel.setAmount(45000);

        assertEquals(0, fakeTransactionRepository.findSimilarCallCount);
        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void similar_editMode_neverQueries() {
        existingTransaction("tx-1");
        AddEditTransactionViewModel viewModel = createEditViewModel("tx-1");

        viewModel.setAmount(50000);
        viewModel.setCategoryId("sys_an_uong");

        assertEquals(0, fakeTransactionRepository.findSimilarCallCount);
    }

    @Test
    public void similar_amountCleared_hidesWarning() {
        fakeTransactionRepository.findSimilarResult =
                new Result.Success<>(similarTransactionAt(System.currentTimeMillis()));
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();
        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");
        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Success);

        viewModel.setAmount(0);

        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void save_success_clearsSimilarWarning() {
        fakeTransactionRepository.findSimilarResult =
                new Result.Success<>(similarTransactionAt(System.currentTimeMillis()));
        AddEditTransactionViewModel viewModel = createAddViewModelWithCategoriesLoaded();
        viewModel.setAmount(45000);
        viewModel.setCategoryId("sys_an_uong");
        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Success);

        viewModel.save();

        assertTrue(fakeTransactionRepository.insertCalled);
        assertTrue(viewModel.getSimilarWarningState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void categorize_successResult_exposesCategoryNameAndPercentSeparately() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });

        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Success<>(
                new CategorySuggestion("sys_an_uong", category("sys_an_uong", "Ăn uống", "expense"), 0.62f)));

        CategorySuggestionUiModel model = ((UiState.Success<CategorySuggestionUiModel>)
                viewModel.getCategorySuggestionState().getValue()).data;
        assertEquals("Ăn uống", model.categoryName);
        assertEquals(62, model.confidencePercent);
    }

    @Test
    public void dismissCategorySuggestion_hidesTheCard() throws InterruptedException {
        FakeAiRepository fakeAiRepository = new FakeAiRepository();
        AddEditTransactionViewModel viewModel = createViewModel(new HashMap<>(), fakeAiRepository);
        viewModel.getCategorySuggestionState().observeForever(state -> { });
        viewModel.setNote("cà phê");
        waitUntil(() -> fakeAiRepository.categorizeCalled, 2000);
        fakeAiRepository.completeCategorize(new Result.Success<>(
                new CategorySuggestion("sys_an_uong", category("sys_an_uong", "Ăn uống", "expense"), 0.62f)));
        assertTrue(viewModel.getCategorySuggestionState().getValue() instanceof UiState.Success);

        viewModel.dismissCategorySuggestion();

        assertTrue(viewModel.getCategorySuggestionState().getValue() instanceof UiState.Empty);
    }
}