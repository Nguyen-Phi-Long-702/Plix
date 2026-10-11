package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.ParseNlItem;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class QuickAddNlViewModel extends ViewModel {
    private static final String EMPTY_TEXT_MESSAGE = "Vui lòng nhập nội dung cần phân tích";

    private final AiRepository aiRepository;
    //null = chưa gửi lần nào; Loading = đang phân tích câu; Success = thông báo kết quả đã định dạng sẵn để hiển thị;
    //Error = câu nhập rỗng hoặc lỗi từ Repository
    private final MutableLiveData<UiState<String>> parseState = new MutableLiveData<>();

    @Inject
    public QuickAddNlViewModel(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
    }

    public LiveData<UiState<String>> getParseState() {
        return parseState;
    }

    public void submit(String text) {
        if (parseState.getValue() instanceof UiState.Loading) {
            return; //đang phân tích rồi, bỏ qua lần bấm thứ hai
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            parseState.setValue(new UiState.Error<>(EMPTY_TEXT_MESSAGE));
            return;
        }
        parseState.setValue(new UiState.Loading<>());
        aiRepository.parseNl(trimmed, this::onParseResult);
    }

    private void onParseResult(Result<List<ParseNlItem>> result) {
        if (result instanceof Result.Error) {
            Result.Error<List<ParseNlItem>> error = (Result.Error<List<ParseNlItem>>) result;
            parseState.setValue(new UiState.Error<>(error.message));
            return;
        }
        List<ParseNlItem> items = ((Result.Success<List<ParseNlItem>>) result).data;
        parseState.setValue(new UiState.Success<>("Đã nhận diện " + items.size() + " giao dịch"));
    }
}