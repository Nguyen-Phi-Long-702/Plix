package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.error.ErrorType;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.ParseNlItem;
import com.longvuong.plix.domain.usecase.transaction.AddSuggestedTransactionsUseCase;
import com.longvuong.plix.domain.validation.FormValidator;
import com.longvuong.plix.presentation.common.UiState;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class QuickAddNlViewModel extends ViewModel {
    private static final String EMPTY_TEXT_MESSAGE = "Vui lòng nhập nội dung cần phân tích";
    private static final String NO_SESSION_MESSAGE = "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại";
    private static final String QUICK_ADD_DISABLED_MESSAGE = "Nhập nhanh đang tắt trong Cài đặt";
    private static final String OFFLINE_MESSAGE = "Không có kết nối mạng";
    private static final String NO_CATEGORY_TITLE = "Chưa chọn danh mục";
    private static final String NO_CATEGORY_DETAIL = "Không xác định rõ danh mục — cần chọn tay";
    private static final String NO_AMOUNT_TITLE = "Chưa rõ số tiền";
    private static final String NO_AMOUNT_DETAIL = "Không nhận diện được số tiền — nhập tay riêng phần này";
    private static final String EDITED_DETAIL = "Đã chỉnh sửa";
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.4f; //dưới ngưỡng này không tự điền danh mục, giống quy tắc gợi ý ở màn thêm giao dịch
    private static final String DEFAULT_PAYMENT_METHOD = "cash"; //giống mặc định của màn thêm giao dịch

    private final AiRepository aiRepository;
    private final CategoryRepository categoryRepository;
    private final AuthManager authManager;
    private final AddSuggestedTransactionsUseCase addSuggestedTransactionsUseCase;
    private final FormValidator formValidator;

    //null = chưa gửi lần nào; Loading = đang phân tích câu; Success = danh sách giao dịch đề xuất (đã định dạng sẵn);
    //Empty = phân tích xong nhưng không nhận diện được giao dịch nào; Error = câu nhập rỗng hoặc lỗi từ Repository
    private final MutableLiveData<UiState<SuggestionListUiModel>> parseState = new MutableLiveData<>();
    //null = chưa lưu; Loading = đang lưu; Success = số giao dịch đã lưu; Error = lỗi hợp lệ hoặc lỗi ghi cơ sở dữ liệu
    private final MutableLiveData<UiState<Integer>> saveState = new MutableLiveData<>();

    //true = lần phân tích gần nhất báo "AI đang bận" (dịch vụ dự phòng Gemini lỗi/hết hạn mức) -> UI hiện thông báo kèm lối nhập tay
    private final MutableLiveData<Boolean> aiBusy = new MutableLiveData<>(false);
    private final List<SuggestionDraft> drafts = new ArrayList<>();

    @Inject
    public QuickAddNlViewModel(AiRepository aiRepository,
                               CategoryRepository categoryRepository,
                               AuthManager authManager,
                               AddSuggestedTransactionsUseCase addSuggestedTransactionsUseCase,
                               FormValidator formValidator) {
        this.aiRepository = aiRepository;
        this.categoryRepository = categoryRepository;
        this.authManager = authManager;
        this.addSuggestedTransactionsUseCase = addSuggestedTransactionsUseCase;
        this.formValidator = formValidator;
    }

    public LiveData<UiState<SuggestionListUiModel>> getParseState() {
        return parseState;
    }

    public LiveData<UiState<Integer>> getSaveState() {
        return saveState;
    }

    public LiveData<Boolean> getAiBusy() {
        return aiBusy;
    }

    //Danh sách danh mục đang dùng, để hộp thoại sửa hiển thị cho người dùng chọn
    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categoryRepository.getActiveCategories();
    }

    public Result<Void> validateAmountField(long amount) {
        return formValidator.validateAmount(amount);
    }

    public void submit(String text) {
        if (isBusy()) {
            return; //đang phân tích hoặc đang lưu, bỏ qua lần bấm thứ hai
        }
        aiBusy.setValue(false); //bắt đầu lần phân tích mới -> gỡ thông báo "AI đang bận" của lần trước
        if (!Boolean.TRUE.equals(aiRepository.observeQuickAddEnabled().getValue())) {
            parseState.setValue(new UiState.Error<>(QUICK_ADD_DISABLED_MESSAGE)); //đã tắt trong Cài đặt -> không gọi NL/Gemini
            return;
        }
        if (Boolean.FALSE.equals(aiRepository.observeConnectivity().getValue())) {
            parseState.setValue(new UiState.Error<>(OFFLINE_MESSAGE)); //AI-10: mất mạng -> không gọi NL/Gemini
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            parseState.setValue(new UiState.Error<>(EMPTY_TEXT_MESSAGE));
            return;
        }
        saveState.setValue(null); //câu mới -> xoá kết quả lưu cũ (nếu lần lưu trước bị lỗi)
        drafts.clear();
        parseState.setValue(new UiState.Loading<>());
        boolean allowGemini = Boolean.TRUE.equals(aiRepository.observeGeminiFallbackEnabled().getValue());
        aiRepository.parseNl(trimmed, allowGemini, this::onParseResult);
    }

    //Tick/bỏ tick một dòng. Dòng chưa có danh mục thì không cho tick (phải sửa và chọn danh mục trước)
    public void setItemSelected(String itemId, boolean selected) {
        SuggestionDraft draft = findDraft(itemId);
        if (draft == null || draft.category == null || draft.amount <= 0) {
            return;
        }
        draft.selected = selected;
        publishList();
    }

    //Người dùng sửa 1 dòng: cập nhật số tiền + danh mục, tự động tick dòng đó (đã được người dùng xác nhận)
    public void updateItem(String itemId, long amount, CategoryEntity category) {
        SuggestionDraft draft = findDraft(itemId);
        if (draft == null) {
            return;
        }
        draft.amount = amount;
        draft.category = category;
        draft.selected = true;
        draft.edited = true;
        publishList();
    }

    //Lưu các dòng đang được tick vào Room (kèm kiểm tra hợp lệ toàn bộ trước khi lưu)
    public void saveSelected() {
        if (isBusy()) {
            return;
        }
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            saveState.setValue(new UiState.Error<>(NO_SESSION_MESSAGE));
            return;
        }
        long now = System.currentTimeMillis();
        List<TransactionEntity> entities = new ArrayList<>();
        for (SuggestionDraft draft : drafts) {
            if (draft.selected && draft.category != null) {
                entities.add(toTransactionEntity(userId, draft, now));
            }
        }
        if (entities.isEmpty()) {
            return; //nút lưu đã bị khoá khi không có dòng nào được tick, đây chỉ là phòng hờ
        }
        saveState.setValue(new UiState.Loading<>());
        addSuggestedTransactionsUseCase.execute(entities, result -> {
            if (result instanceof Result.Success) {
                saveState.setValue(new UiState.Success<>(((Result.Success<Integer>) result).data));
            } else {
                saveState.setValue(new UiState.Error<>(((Result.Error<Integer>) result).message));
            }
        });
    }

    private void onParseResult(Result<List<ParseNlItem>> result) {
        if (result instanceof Result.Error) {
            Result.Error<List<ParseNlItem>> error = (Result.Error<List<ParseNlItem>>) result;
            if (error.type == ErrorType.AI_BUSY) {
                aiBusy.setValue(true); //đặt trước parseState để Fragment đọc đúng giá trị khi vẽ trạng thái lỗi
            }
            parseState.setValue(new UiState.Error<>(error.message));
            return;
        }
        List<ParseNlItem> items = ((Result.Success<List<ParseNlItem>>) result).data;
        drafts.clear();
        for (ParseNlItem item : items) {
            boolean confident = item.category != null && item.confidence >= LOW_CONFIDENCE_THRESHOLD;
            boolean hasAmount = item.amount > 0; //Repository đổi amount null (câu không có số tiền) thành 0
            drafts.add(new SuggestionDraft(UUID.randomUUID().toString(), item.amount,
                    confident ? item.category : null, item.confidence, confident && hasAmount));
        }
        if (drafts.isEmpty()) {
            parseState.setValue(new UiState.Empty<>());
            return;
        }
        publishList();
    }

    private void publishList() {
        List<SuggestionItemUiModel> items = new ArrayList<>();
        int selectedCount = 0;
        for (SuggestionDraft draft : drafts) {
            items.add(toUiModel(draft));
            if (draft.selected) {
                selectedCount++;
            }
        }
        parseState.setValue(new UiState.Success<>(new SuggestionListUiModel(
                items,
                "Kết quả nhận diện (" + drafts.size() + ")",
                "Lưu " + selectedCount + " giao dịch hợp lệ",
                selectedCount > 0)));
    }

    private SuggestionItemUiModel toUiModel(SuggestionDraft draft) {
        boolean needsCategory = draft.category == null;
        boolean needsAmount = draft.amount <= 0;
        boolean income = !needsCategory && "income".equals(draft.category.type);
        String title = needsCategory ? NO_CATEGORY_TITLE : draft.category.name;
        String detail;
        if (draft.edited) {
            detail = EDITED_DETAIL;
        } else if (needsCategory) {
            detail = NO_CATEGORY_DETAIL;
        } else {
            detail = "Độ tin cậy: " + Math.round(draft.confidence * 100) + "%";
        }
        if (needsAmount) {
            title = NO_AMOUNT_TITLE; //thiếu số tiền được ưu tiên hơn thiếu danh mục: phải nhập số tiền trước
            detail = NO_AMOUNT_DETAIL;
        }
        return new SuggestionItemUiModel(
                draft.itemId,
                draft.amount,
                needsCategory ? null : draft.category.id,
                title,
                detail,
                needsAmount ? "" : (income ? "+" : "-") + formatCurrency(draft.amount),
                income,
                draft.selected,
                needsCategory,
                needsAmount);
    }

    private TransactionEntity toTransactionEntity(String userId, SuggestionDraft draft, long now) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.amount = draft.amount;
        entity.type = draft.category.type;
        entity.categoryId = draft.category.id;
        entity.note = ""; //Endpoint /parse-nl chưa trả phần ghi chú cho từng giao dịch
        entity.paymentMethod = DEFAULT_PAYMENT_METHOD;
        entity.occurredAt = now;
        entity.isRecurring = false;
        entity.recurrenceRule = null;
        entity.recurrenceParentId = null;
        entity.updatedAt = now;
        entity.syncStatus = "pending";
        entity.isDeleted = false;
        return entity;
    }

    @Nullable
    private SuggestionDraft findDraft(String itemId) {
        for (SuggestionDraft draft : drafts) {
            if (draft.itemId.equals(itemId)) {
                return draft;
            }
        }
        return null;
    }

    private boolean isBusy() {
        return parseState.getValue() instanceof UiState.Loading || saveState.getValue() instanceof UiState.Loading;
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    //Trạng thái nội bộ của 1 dòng đề xuất (người dùng đang tick/sửa), chỉ ViewModel giữ
    private static class SuggestionDraft {
        final String itemId;
        long amount;
        @Nullable
        CategoryEntity category;
        final float confidence;
        boolean selected;
        boolean edited;

        SuggestionDraft(String itemId, long amount, @Nullable CategoryEntity category, float confidence, boolean selected) {
            this.itemId = itemId;
            this.amount = amount;
            this.category = category;
            this.confidence = confidence;
            this.selected = selected;
        }
    }
}