package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.ParseNlItem;
import com.longvuong.plix.domain.usecase.transaction.AddSuggestedTransactionsUseCase;
import com.longvuong.plix.domain.usecase.transaction.AddTransactionUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class QuickAddNlViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private FakeTransactionRepository fakeTransactionRepository;
    private FakeCategoryRepository fakeCategoryRepository;
    private FormValidator formValidator;
    private AuthManager authManager;
    private QuickAddNlViewModel viewModel;

    private CategoryEntity anUong;
    private CategoryEntity diChuyen;
    private CategoryEntity luong;

    @Before
    public void setUp() throws Exception {
        fakeAiRepository = new FakeAiRepository();
        fakeTransactionRepository = new FakeTransactionRepository();
        anUong = category("sys_an_uong", "Ăn uống", "expense");
        diChuyen = category("sys_di_chuyen", "Di chuyển", "expense");
        luong = category("sys_luong", "Lương", "income");
        fakeCategoryRepository = new FakeCategoryRepository(Arrays.asList(anUong, diChuyen, luong));
        formValidator = new FormValidator();
        authManager = new AuthManager(null);
        setFakeAccessToken(authManager, fakeJwtWithSub("user-1"));
        viewModel = createViewModel();
    }

    private QuickAddNlViewModel createViewModel() {
        AddTransactionUseCase addTransactionUseCase = new AddTransactionUseCase(
                fakeTransactionRepository, formValidator, fakeCheckBudgetThresholdUseCase());
        return new QuickAddNlViewModel(fakeAiRepository, fakeCategoryRepository, authManager,
                new AddSuggestedTransactionsUseCase(addTransactionUseCase, formValidator), formValidator);
    }

    private com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase fakeCheckBudgetThresholdUseCase() {
        return new com.longvuong.plix.domain.usecase.budget.CheckBudgetThresholdUseCase(
                new FakeBudgetRepository(),
                fakeTransactionRepository,
                new com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase(),
                new com.longvuong.plix.domain.usecase.budget.BudgetThresholdChecker(),
                (budget, spentAfterAmount) -> { });
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

    //Câu mẫu "phở 50k, grab 35k" -> 2 giao dịch đúng danh mục và số tiền
    private Result<List<ParseNlItem>> twoItemsResult() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, anUong, 0.9f));
        items.add(new ParseNlItem(35_000L, diChuyen, 0.9f));
        return new Result.Success<>(items);
    }

    private SuggestionListUiModel submitAndGetList(Result<List<ParseNlItem>> parseResult) {
        viewModel.submit("phở 50k, grab 35k");
        fakeAiRepository.completeParseNl(parseResult);
        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Success);
        return ((UiState.Success<SuggestionListUiModel>) state).data;
    }

    private SuggestionListUiModel currentList() {
        return ((UiState.Success<SuggestionListUiModel>) viewModel.getParseState().getValue()).data;
    }

    @Test
    public void init_doesNotCallRepositoryUntilSubmit() {
        assertNull(viewModel.getParseState().getValue());
        assertNull(viewModel.getSaveState().getValue());
        assertEquals(0, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_blankText_becomesErrorAndDoesNotCallRepository() {
        viewModel.submit("   ");

        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Vui lòng nhập nội dung cần phân tích", ((UiState.Error<SuggestionListUiModel>) state).message);
        assertEquals(0, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_trimsTextAndShowsLoading() {
        viewModel.submit("  phở 50k, grab 35k  ");

        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Loading);
        assertEquals(1, fakeAiRepository.parseNlCallCount);
        assertEquals("phở 50k, grab 35k", fakeAiRepository.lastParseNlText);
    }

    @Test
    public void submit_success_buildsOneSelectedRowPerItemWithCategoryAndAmount() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());

        assertEquals(2, list.items.size());
        assertEquals("Kết quả nhận diện (2)", list.titleText);
        assertEquals("Lưu 2 giao dịch hợp lệ", list.saveButtonText);
        assertTrue(list.canSave);

        SuggestionItemUiModel first = list.items.get(0);
        assertEquals("Ăn uống", first.title);
        assertEquals("sys_an_uong", first.categoryId);
        assertEquals(50_000L, first.amount);
        assertEquals("-50.000đ", first.amountText);
        assertEquals("Độ tin cậy: 90%", first.detail);
        assertTrue(first.selected);
        assertFalse(first.needsCategory);
        assertFalse(first.income);

        SuggestionItemUiModel second = list.items.get(1);
        assertEquals("Di chuyển", second.title);
        assertEquals("-35.000đ", second.amountText);
    }

    @Test
    public void submit_success_emptyList_becomesEmpty() {
        viewModel.submit("abc");
        fakeAiRepository.completeParseNl(new Result.Success<>(new ArrayList<>()));

        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void submit_error_becomesErrorWithMessage() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", null));

        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không có kết nối mạng", ((UiState.Error<SuggestionListUiModel>) state).message);
    }

    @Test
    public void init_aiBusyIsFalse() {
        assertEquals(Boolean.FALSE, viewModel.getAiBusy().getValue());
    }

    @Test
    public void submit_aiBusyError_setsAiBusyAndParseStateError() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.AI_BUSY, "AI đang bận, vui lòng nhập tay", null));

        assertEquals(Boolean.TRUE, viewModel.getAiBusy().getValue());
        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("AI đang bận, vui lòng nhập tay", ((UiState.Error<SuggestionListUiModel>) state).message);
    }

    @Test
    public void submit_networkError_doesNotSetAiBusy() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", null));

        assertEquals(Boolean.FALSE, viewModel.getAiBusy().getValue());
    }

    @Test
    public void submit_afterAiBusy_resetsAiBusyAndCallsRepositoryAgain() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.AI_BUSY, "AI đang bận, vui lòng nhập tay", null));

        viewModel.submit("phở 50k");

        assertEquals(Boolean.FALSE, viewModel.getAiBusy().getValue());
        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Loading);
        assertEquals(2, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_blankTextAfterAiBusy_resetsAiBusy() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(new Result.Error<>(ErrorType.AI_BUSY, "AI đang bận, vui lòng nhập tay", null));

        viewModel.submit("   ");

        assertEquals(Boolean.FALSE, viewModel.getAiBusy().getValue());
        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Error);
    }

    @Test
    public void submit_whileLoading_doesNotCallRepositoryAgain() {
        viewModel.submit("phở 50k");
        viewModel.submit("phở 50k");

        assertEquals(1, fakeAiRepository.parseNlCallCount);
    }

    @Test
    public void submit_afterCompleted_canSubmitAgain() {
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(twoItemsResult());
        viewModel.submit("grab 35k");

        assertEquals(2, fakeAiRepository.parseNlCallCount);
        assertTrue(viewModel.getParseState().getValue() instanceof UiState.Loading);
    }

    @Test
    public void submit_lowConfidenceItem_needsCategoryAndIsNotSelected() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, anUong, 0.9f));
        items.add(new ParseNlItem(65_000L, anUong, 0.3f)); //dưới 0,4: không tự điền danh mục
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        SuggestionItemUiModel low = list.items.get(1);
        assertTrue(low.needsCategory);
        assertFalse(low.selected);
        assertNull(low.categoryId);
        assertEquals("Chưa chọn danh mục", low.title);
        assertEquals("Không xác định rõ danh mục — cần chọn tay", low.detail);
        assertEquals("Lưu 1 giao dịch hợp lệ", list.saveButtonText); //chỉ tính dòng đã được tick
    }

    @Test
    public void submit_itemWithoutKnownCategory_needsCategory() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, null, 0.9f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        assertTrue(list.items.get(0).needsCategory);
        assertFalse(list.canSave);
    }
    @Test
    public void submit_itemWithoutAmount_needsAmountAndIsNotSelected() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, anUong, 0.9f));
        items.add(new ParseNlItem(0L, anUong, 0.9f)); //câu không có số tiền: Repository đổi amount null thành 0
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        SuggestionItemUiModel noAmount = list.items.get(1);
        assertTrue(noAmount.needsAmount);
        assertFalse(noAmount.selected);
        assertEquals("Chưa rõ số tiền", noAmount.title);
        assertEquals("Không nhận diện được số tiền — nhập tay riêng phần này", noAmount.detail);
        assertEquals("", noAmount.amountText);
        assertEquals("sys_an_uong", noAmount.categoryId); //giữ danh mục để hộp thoại sửa chọn sẵn
        assertFalse(list.items.get(0).needsAmount);
        assertEquals("Lưu 1 giao dịch hợp lệ", list.saveButtonText); //chỉ tính dòng đã được tick
    }

    @Test
    public void submit_itemWithoutAmountAndCategory_needsBoth() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(0L, null, 0f)); //đúng như backend trả cho phần câu không có số tiền: amount null, category_id null, confidence 0
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        SuggestionItemUiModel item = list.items.get(0);
        assertTrue(item.needsAmount);
        assertTrue(item.needsCategory);
        assertFalse(item.selected);
        assertNull(item.categoryId);
        assertEquals("Chưa rõ số tiền", item.title);
        assertFalse(list.canSave);
    }

    @Test
    public void setItemSelected_onItemNeedingAmount_isIgnored() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(0L, anUong, 0.9f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        viewModel.setItemSelected(list.items.get(0).itemId, true);

        assertFalse(currentList().items.get(0).selected);
        assertFalse(currentList().canSave);
    }

    @Test
    public void updateItem_onItemWithoutAmount_enteringAmountSelectsRow() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(0L, null, 0f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        viewModel.updateItem(list.items.get(0).itemId, 40_000L, anUong);

        SuggestionItemUiModel edited = currentList().items.get(0);
        assertFalse(edited.needsAmount);
        assertFalse(edited.needsCategory);
        assertTrue(edited.selected);
        assertEquals("Ăn uống", edited.title);
        assertEquals("-40.000đ", edited.amountText);
        assertEquals("Đã chỉnh sửa", edited.detail);
        assertTrue(currentList().canSave);
    }
    @Test
    public void setItemSelected_untick_updatesSelectionAndSaveButton() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());

        viewModel.setItemSelected(list.items.get(0).itemId, false);

        SuggestionListUiModel updated = currentList();
        assertFalse(updated.items.get(0).selected);
        assertTrue(updated.items.get(1).selected);
        assertEquals("Lưu 1 giao dịch hợp lệ", updated.saveButtonText);
        assertTrue(updated.canSave);
    }

    @Test
    public void setItemSelected_untickAll_disablesSave() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());

        viewModel.setItemSelected(list.items.get(0).itemId, false);
        viewModel.setItemSelected(list.items.get(1).itemId, false);

        assertFalse(currentList().canSave);
    }

    @Test
    public void setItemSelected_onItemNeedingCategory_isIgnored() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(65_000L, anUong, 0.3f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        viewModel.setItemSelected(list.items.get(0).itemId, true);

        assertFalse(currentList().items.get(0).selected);
    }

    @Test
    public void updateItem_changesAmountAndCategoryAndSelectsRow() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(65_000L, anUong, 0.3f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));

        viewModel.updateItem(list.items.get(0).itemId, 70_000L, diChuyen);

        SuggestionItemUiModel edited = currentList().items.get(0);
        assertEquals("Di chuyển", edited.title);
        assertEquals("sys_di_chuyen", edited.categoryId);
        assertEquals(70_000L, edited.amount);
        assertEquals("-70.000đ", edited.amountText);
        assertEquals("Đã chỉnh sửa", edited.detail);
        assertTrue(edited.selected);
        assertFalse(edited.needsCategory);
        assertEquals("Lưu 1 giao dịch hợp lệ", currentList().saveButtonText);
    }

    @Test
    public void updateItem_toIncomeCategory_showsPlusSignAndIncomeFlag() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());

        viewModel.updateItem(list.items.get(0).itemId, 12_000_000L, luong);

        SuggestionItemUiModel edited = currentList().items.get(0);
        assertTrue(edited.income);
        assertEquals("+12.000.000đ", edited.amountText);
    }

    @Test
    public void validateAmountField_zeroIsRejected_positiveIsAccepted() {
        assertTrue(viewModel.validateAmountField(0L) instanceof Result.Error);
        assertTrue(viewModel.validateAmountField(50_000L) instanceof Result.Success);
    }

    @Test
    public void saveSelected_savesOnlyTickedRowsAsPendingTransactions() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());
        viewModel.setItemSelected(list.items.get(1).itemId, false); //bỏ tick "Grab 35k"

        viewModel.saveSelected();

        assertEquals(1, fakeTransactionRepository.insertedEntities.size());
        TransactionEntity saved = fakeTransactionRepository.insertedEntities.get(0);
        assertEquals("user-1", saved.userId);
        assertEquals(50_000L, saved.amount);
        assertEquals("expense", saved.type);
        assertEquals("sys_an_uong", saved.categoryId);
        assertEquals("", saved.note);
        assertEquals("cash", saved.paymentMethod);
        assertEquals("pending", saved.syncStatus);
        assertFalse(saved.isDeleted);
        assertFalse(saved.isRecurring);

        UiState<Integer> state = viewModel.getSaveState().getValue();
        assertTrue(state instanceof UiState.Success);
        assertEquals(1, (int) ((UiState.Success<Integer>) state).data);
    }

    @Test
    public void saveSelected_exampleSentence_savesTwoTransactionsWithCorrectCategoryAndAmount() {
        submitAndGetList(twoItemsResult());

        viewModel.saveSelected();

        assertEquals(2, fakeTransactionRepository.insertedEntities.size());
        assertEquals("sys_an_uong", fakeTransactionRepository.insertedEntities.get(0).categoryId);
        assertEquals(50_000L, fakeTransactionRepository.insertedEntities.get(0).amount);
        assertEquals("sys_di_chuyen", fakeTransactionRepository.insertedEntities.get(1).categoryId);
        assertEquals(35_000L, fakeTransactionRepository.insertedEntities.get(1).amount);
        assertEquals(2, (int) ((UiState.Success<Integer>) viewModel.getSaveState().getValue()).data);
    }

    @Test
    public void saveSelected_editedRowToIncomeCategory_savesWithIncomeType() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());
        viewModel.setItemSelected(list.items.get(1).itemId, false);
        viewModel.updateItem(list.items.get(0).itemId, 12_000_000L, luong);

        viewModel.saveSelected();

        TransactionEntity saved = fakeTransactionRepository.insertedEntities.get(0);
        assertEquals("income", saved.type);
        assertEquals("sys_luong", saved.categoryId);
        assertEquals(12_000_000L, saved.amount);
    }

    @Test
    public void saveSelected_withoutSession_becomesErrorAndSavesNothing() throws Exception {
        submitAndGetList(twoItemsResult());
        setFakeAccessToken(authManager, null);

        viewModel.saveSelected();

        UiState<Integer> state = viewModel.getSaveState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại", ((UiState.Error<Integer>) state).message);
        assertEquals(0, fakeTransactionRepository.insertedEntities.size());
    }

    @Test
    public void saveSelected_skipsItemWithoutAmount() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, anUong, 0.9f));
        items.add(new ParseNlItem(0L, diChuyen, 0.9f)); //không có số tiền: không được tick sẵn nên không bị lưu
        submitAndGetList(new Result.Success<>(items));

        viewModel.saveSelected();

        assertEquals(1, fakeTransactionRepository.insertedEntities.size());
        assertEquals(50_000L, fakeTransactionRepository.insertedEntities.get(0).amount);
        assertEquals(1, (int) ((UiState.Success<Integer>) viewModel.getSaveState().getValue()).data);
    }

    @Test
    public void saveSelected_invalidAmountInRow_becomesErrorAndSavesNothing() {
        List<ParseNlItem> items = new ArrayList<>();
        items.add(new ParseNlItem(50_000L, anUong, 0.9f));
        items.add(new ParseNlItem(0L, diChuyen, 0.9f));
        SuggestionListUiModel list = submitAndGetList(new Result.Success<>(items));
        viewModel.updateItem(list.items.get(1).itemId, 0L, diChuyen); //ép dòng thứ hai thành số tiền 0 đã tick (hộp thoại sửa không cho nhập 0, đây là kiểm tra phòng hờ ở bước lưu)

        viewModel.saveSelected();

        UiState<Integer> state = viewModel.getSaveState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Số tiền phải lớn hơn 0", ((UiState.Error<Integer>) state).message);
        assertEquals(0, fakeTransactionRepository.insertedEntities.size());
    }

    @Test
    public void saveSelected_nothingTicked_doesNothing() {
        SuggestionListUiModel list = submitAndGetList(twoItemsResult());
        viewModel.setItemSelected(list.items.get(0).itemId, false);
        viewModel.setItemSelected(list.items.get(1).itemId, false);

        viewModel.saveSelected();

        assertNull(viewModel.getSaveState().getValue());
        assertEquals(0, fakeTransactionRepository.insertedEntities.size());
    }

    @Test
    public void submit_afterSaveError_clearsOldSaveState() throws Exception {
        submitAndGetList(twoItemsResult());
        setFakeAccessToken(authManager, null);
        viewModel.saveSelected();
        assertTrue(viewModel.getSaveState().getValue() instanceof UiState.Error);

        viewModel.submit("grab 35k");

        assertNull(viewModel.getSaveState().getValue());
    }

    @Test
    public void submit_quickAddDisabled_doesNotCallParseNlAndShowsError() {
        fakeAiRepository.setQuickAddEnabled(false);

        viewModel.submit("phở 50k, grab 35k");

        assertEquals(0, fakeAiRepository.parseNlCallCount);
        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Nhập nhanh đang tắt trong Cài đặt", ((UiState.Error<SuggestionListUiModel>) state).message);
    }

    @Test
    public void submit_offline_doesNotCallParseNlAndShowsError() {
        fakeAiRepository.setConnected(false);

        viewModel.submit("phở 50k, grab 35k");

        assertEquals(0, fakeAiRepository.parseNlCallCount);
        UiState<SuggestionListUiModel> state = viewModel.getParseState().getValue();
        assertTrue(state instanceof UiState.Error);
        assertEquals("Không có kết nối mạng", ((UiState.Error<SuggestionListUiModel>) state).message);
    }

    @Test
    public void submit_geminiFallbackOff_sendsAllowGeminiFalse() {
        fakeAiRepository.setGeminiFallbackEnabled(false);

        viewModel.submit("phở 50k, grab 35k");

        assertEquals(1, fakeAiRepository.parseNlCallCount);
        assertFalse(fakeAiRepository.lastParseNlAllowGemini);
    }

    @Test
    public void submit_geminiFallbackOn_sendsAllowGeminiTrue() {
        fakeAiRepository.setGeminiFallbackEnabled(true);

        viewModel.submit("phở 50k, grab 35k");

        assertEquals(1, fakeAiRepository.parseNlCallCount);
        assertTrue(fakeAiRepository.lastParseNlAllowGemini);
    }

    @Test
    public void submit_readsGeminiToggleAtSubmitTime_notAtScreenOpen() {
        fakeAiRepository.setGeminiFallbackEnabled(true);
        viewModel.submit("phở 50k");
        fakeAiRepository.completeParseNl(twoItemsResult());
        assertTrue(fakeAiRepository.lastParseNlAllowGemini);

        fakeAiRepository.setGeminiFallbackEnabled(false); //người dùng tắt công tắc rồi gửi câu tiếp theo
        viewModel.submit("grab 35k");

        assertEquals(2, fakeAiRepository.parseNlCallCount);
        assertFalse(fakeAiRepository.lastParseNlAllowGemini);
    }
    @Test
    public void getOffline_followsConnectivity() {
        Observer<Boolean> observer = offline -> { };
        viewModel.getOffline().observeForever(observer); //LiveData biến đổi chỉ cập nhật khi đang có người quan sát
        try {
            assertEquals(Boolean.FALSE, viewModel.getOffline().getValue()); //FakeAiRepository mặc định có mạng

            fakeAiRepository.setConnected(false);
            assertEquals(Boolean.TRUE, viewModel.getOffline().getValue());

            fakeAiRepository.setConnected(true);
            assertEquals(Boolean.FALSE, viewModel.getOffline().getValue());
        } finally {
            viewModel.getOffline().removeObserver(observer);
        }
    }
}