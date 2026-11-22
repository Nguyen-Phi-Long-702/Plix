package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.core.auth.AuthManager;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.recurring.RecurringSuggestionPreferences;
import com.longvuong.plix.domain.usecase.transaction.DetectRecurringCandidatesUseCase;
import com.longvuong.plix.domain.usecase.transaction.RecurringCandidate;
import com.longvuong.plix.domain.usecase.transaction.RecurringPrefillDateCalculator;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecurringSuggestionViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String USER_A = "user-A";
    private static final String CAT_ENT = "cat-ent";

    private FakeTransactionRepository fakeTransactionRepository;
    private FakeRecurringSuggestionPreferences fakePreferences;
    private RecurringSuggestionViewModel viewModel;

    @Before
    public void setUp() {
        fakeTransactionRepository = new FakeTransactionRepository();
        fakePreferences = new FakeRecurringSuggestionPreferences();
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE); //hôm nay cố định = 2026-11-08
        viewModel = new RecurringSuggestionViewModel(fakeTransactionRepository, new FakeAuthManagerWithUser(USER_A),
                fakePreferences, new DetectRecurringCandidatesUseCase(), new RecurringPrefillDateCalculator(),
                Runnable::run, clock); //executor chạy tại chỗ nên kết quả có ngay sau khi Room phát dữ liệu
        viewModel.getSuggestion().observeForever(state -> { }); //MediatorLiveData chỉ cập nhật khi có người quan sát
    }

    //Giao dịch chi của user-A, danh mục cat-ent, năm 2026, giữa trưa theo ZONE; không xoá, không phải mẫu, không do Worker sinh
    private static TransactionEntity transaction(String id, int month, int day, long amount) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = USER_A;
        entity.type = "expense";
        entity.categoryId = CAT_ENT;
        entity.amount = amount;
        entity.occurredAt = LocalDate.of(2026, month, day).atTime(12, 0).atZone(ZONE).toInstant().toEpochMilli();
        return entity;
    }

    //BD1 (DP_02 mục 7.4): 260.000 vào 05/08, 05/09, 05/10/2026
    private static List<TransactionEntity> dataBd1() {
        List<TransactionEntity> all = new ArrayList<>();
        all.add(transaction("t-08", 8, 5, 260_000L));
        all.add(transaction("t-09", 9, 5, 260_000L));
        all.add(transaction("t-10", 10, 5, 260_000L));
        return all;
    }

    /**
     * TN6-U27
     * FakeTransactionRepository dữ liệu BD1, prefs rỗng: state Success với 1 ứng viên (260.000, ngày 5, 3 tháng); không có dữ liệu thì Empty.
     */
    @Test
    public void getSuggestion_bd1_emitsFirstCandidateAndEmptyWhenNoData() {
        fakeTransactionRepository.allTransactions.setValue(new ArrayList<TransactionEntity>());
        assertTrue(viewModel.getSuggestion().getValue() instanceof UiState.Empty);

        fakeTransactionRepository.allTransactions.setValue(dataBd1());

        UiState<RecurringCandidate> state = viewModel.getSuggestion().getValue();
        assertTrue(state instanceof UiState.Success);
        RecurringCandidate candidate = ((UiState.Success<RecurringCandidate>) state).data;
        assertEquals("expense", candidate.type);
        assertEquals(CAT_ENT, candidate.categoryId);
        assertEquals(260_000L, candidate.amount);
        assertEquals(5, candidate.dayOfMonth);
        assertEquals(3, candidate.consecutiveMonths);
    }

    /**
     * TN6-U28
     * dismiss() trên ứng viên BD1: prefs có khoá 'expense|cat-ent' cho user hiện tại; state chuyển Empty.
     */
    @Test
    public void dismiss_afterCandidateShown_savesKeyForCurrentUserAndEmitsEmpty() {
        fakeTransactionRepository.allTransactions.setValue(dataBd1());

        viewModel.dismiss();

        assertEquals(Collections.singleton("expense|cat-ent"), fakePreferences.getDismissedKeys(USER_A));
        assertTrue(viewModel.getSuggestion().getValue() instanceof UiState.Empty);
    }

    /**
     * TN6-U29
     * accept() với ứng viên BD1, today 2026-11-08: giá trị điền sẵn có amount 260000, type 'expense', categoryId 'cat-ent', occurredAt = đầu ngày 2026-12-05, recurringDay 5.
     */
    @Test
    public void accept_bd1_returnsPrefillWithNextDueDate() {
        fakeTransactionRepository.allTransactions.setValue(dataBd1());

        RecurringPrefill prefill = viewModel.accept();

        assertNotNull(prefill);
        assertEquals(260_000L, prefill.amount);
        assertEquals("expense", prefill.type);
        assertEquals(CAT_ENT, prefill.categoryId);
        assertEquals(LocalDate.of(2026, 12, 5).atStartOfDay(ZONE).toInstant().toEpochMilli(), prefill.occurredAt);
        assertEquals(5, prefill.recurringDay);
    }

    //AuthManager giả: không có phiên đăng nhập thật, chỉ trả userId do test đặt
    private static class FakeAuthManagerWithUser extends AuthManager {
        private final String userId;

        FakeAuthManagerWithUser(String userId) {
            super(null);
            this.userId = userId;
        }

        @Override
        public String getCurrentUserId() {
            return userId;
        }
    }

    //Danh sách bỏ qua lưu trong bộ nhớ, không dùng SharedPreferences thật
    private static class FakeRecurringSuggestionPreferences extends RecurringSuggestionPreferences {
        private final Map<String, Set<String>> dismissedByUser = new HashMap<>();

        FakeRecurringSuggestionPreferences() {
            //Hàm khởi tạo của lớp cha gọi context.getSharedPreferences, nên cần một Context không ném lỗi "not mocked"
            super(new ContextWrapper(null) {
                @Override
                public SharedPreferences getSharedPreferences(String name, int mode) {
                    return null; //hai hàm bên dưới được ghi đè nên không bao giờ dùng tới
                }
            });
        }

        @Override
        public Set<String> getDismissedKeys(String userId) {
            Set<String> keys = dismissedByUser.get(userId);
            return keys == null ? new HashSet<String>() : new HashSet<>(keys);
        }

        @Override
        public void dismiss(String userId, String key) {
            Set<String> keys = dismissedByUser.get(userId);
            if (keys == null) {
                keys = new HashSet<>();
                dismissedByUser.put(userId, keys);
            }
            keys.add(key);
        }
    }
}