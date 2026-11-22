package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.core.executor.AppExecutors;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.recurring.RecurringSuggestionPreferences;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.transaction.DetectRecurringCandidatesUseCase;
import com.longvuong.plix.domain.usecase.transaction.RecurringCandidate;
import com.longvuong.plix.domain.usecase.transaction.RecurringPrefillDateCalculator;
import com.longvuong.plix.presentation.common.UiState;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

//Gợi ý tạo giao dịch định kỳ từ lịch sử giao dịch (FIN-17, FIN-18, FIN-19). Chỉ tính trên máy từ Room, không gọi mạng,
//không log ghi chú hay số tiền
@HiltViewModel
public class RecurringSuggestionViewModel extends ViewModel {
    private final AuthManager authManager;
    private final RecurringSuggestionPreferences preferences;
    private final DetectRecurringCandidatesUseCase detectRecurringCandidatesUseCase;
    private final RecurringPrefillDateCalculator dateCalculator;
    private final Executor diskExecutor;
    private final Clock clock;

    //Loading = chờ Room trả giao dịch lần đầu; Success = ứng viên đầu tiên (tối đa 1 ứng viên mỗi lần);
    //Empty = không có ứng viên hoặc người dùng vừa bấm Bỏ qua. Màn hình chỉ hiện thẻ khi Success
    private final MediatorLiveData<UiState<RecurringCandidate>> suggestion = new MediatorLiveData<>();

    @Inject
    public RecurringSuggestionViewModel(TransactionRepository transactionRepository,
                                        AuthManager authManager,
                                        RecurringSuggestionPreferences preferences,
                                        DetectRecurringCandidatesUseCase detectRecurringCandidatesUseCase,
                                        RecurringPrefillDateCalculator dateCalculator,
                                        AppExecutors appExecutors) {
        this(transactionRepository, authManager, preferences, detectRecurringCandidatesUseCase, dateCalculator,
                appExecutors.diskIO(), Clock.systemDefaultZone());
    }

    //Chỉ để test cùng package dùng: đặt sẵn executor chạy tại chỗ (AppExecutors cần Looper của Android nên không tạo được
    //trong unit test) và đồng hồ cố định để "hôm nay" là 2026-11-08 như BD1
    RecurringSuggestionViewModel(TransactionRepository transactionRepository,
                                 AuthManager authManager,
                                 RecurringSuggestionPreferences preferences,
                                 DetectRecurringCandidatesUseCase detectRecurringCandidatesUseCase,
                                 RecurringPrefillDateCalculator dateCalculator,
                                 Executor diskExecutor,
                                 Clock clock) {
        this.authManager = authManager;
        this.preferences = preferences;
        this.detectRecurringCandidatesUseCase = detectRecurringCandidatesUseCase;
        this.dateCalculator = dateCalculator;
        this.diskExecutor = diskExecutor;
        this.clock = clock;

        suggestion.setValue(new UiState.Loading<>());
        suggestion.addSource(transactionRepository.getAll(), this::computeSuggestion);
    }

    public LiveData<UiState<RecurringCandidate>> getSuggestion() {
        return suggestion;
    }

    //Người dùng bấm "Bỏ qua": lưu khoá type|categoryId theo người dùng hiện tại và ẩn thẻ ngay
    public void dismiss() {
        UiState<RecurringCandidate> current = suggestion.getValue();
        String userId = authManager.getCurrentUserId();
        if (!(current instanceof UiState.Success) || userId == null) {
            return;
        }
        preferences.dismiss(userId, ((UiState.Success<RecurringCandidate>) current).data.getKey());
        suggestion.setValue(new UiState.Empty<>());
    }

    //Người dùng bấm "Tạo": trả giá trị điền sẵn cho form Thêm giao dịch. Chưa tạo mẫu nào ở bước này,
    //chỉ khi người dùng bấm Lưu giao dịch trên form thì mẫu định kỳ mới được tạo. Không có ứng viên thì trả null
    @Nullable
    public RecurringPrefill accept() {
        UiState<RecurringCandidate> current = suggestion.getValue();
        if (!(current instanceof UiState.Success)) {
            return null;
        }
        RecurringCandidate candidate = ((UiState.Success<RecurringCandidate>) current).data;
        LocalDate dueDate = dateCalculator.calculate(candidate.dayOfMonth, LocalDate.now(clock));
        long occurredAt = dueDate.atStartOfDay(clock.getZone()).toInstant().toEpochMilli();
        return new RecurringPrefill(candidate.amount, candidate.type, candidate.categoryId, candidate.note,
                occurredAt, candidate.dayOfMonth);
    }

    //Mỗi lần Room phát danh sách giao dịch mới thì tính lại trên diskIO rồi đưa kết quả về LiveData
    private void computeSuggestion(List<TransactionEntity> transactions) {
        List<TransactionEntity> all = transactions != null ? transactions : new ArrayList<>();
        String userId = authManager.getCurrentUserId();
        diskExecutor.execute(() -> suggestion.postValue(findFirstSuggestion(all, userId)));
    }

    private UiState<RecurringCandidate> findFirstSuggestion(List<TransactionEntity> all, @Nullable String userId) {
        if (userId == null) {
            return new UiState.Empty<>(); //chưa đăng nhập thì không có gợi ý
        }
        //getAll() đã gồm cả mẫu định kỳ chưa xoá, nên lấy các mẫu từ chính danh sách này
        //(cùng điều kiện với TransactionDao.getActiveRecurringTemplates)
        List<TransactionEntity> activeTemplates = new ArrayList<>();
        for (TransactionEntity transaction : all) {
            if (transaction.isRecurring && transaction.recurrenceParentId == null && !transaction.isDeleted) {
                activeTemplates.add(transaction);
            }
        }
        List<RecurringCandidate> candidates = detectRecurringCandidatesUseCase.execute(all, activeTemplates,
                preferences.getDismissedKeys(userId), userId, LocalDate.now(clock), clock.getZone());
        if (candidates.isEmpty()) {
            return new UiState.Empty<>();
        }
        return new UiState.Success<>(candidates.get(0));
    }
}