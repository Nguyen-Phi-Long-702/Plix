package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.transaction.DeleteTransactionUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.common.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionListViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final MutableLiveData<List<TransactionEntity>> allTransactions = new MutableLiveData<>(new ArrayList<>());
    private FakeAiRepository fakeAiRepository;
    private TransactionListViewModel viewModel;

    @Before
    public void setUp() {
        FakeTransactionRepository fakeTransactionRepository = new FakeTransactionRepository() {
            @Override
            public LiveData<List<TransactionEntity>> getAll() {
                return allTransactions;
            }
        };
        fakeAiRepository = new FakeAiRepository();
        viewModel = new TransactionListViewModel(
                fakeTransactionRepository,
                new FakeCategoryRepository(new ArrayList<>()),
                fakeAiRepository,
                new FilterTransactionsUseCase(),
                new DeleteTransactionUseCase(fakeTransactionRepository));
        viewModel.getTransactionListState().observeForever(state -> { }); //MediatorLiveData chỉ cập nhật khi có người quan sát
        viewModel.getQuickAddVisible().observeForever(visible -> { });
    }

    private TransactionEntity transactionOn(String id, LocalDate date) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = 50_000;
        entity.type = "expense";
        entity.occurredAt = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        entity.updatedAt = entity.occurredAt;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void transactionListState_whenNoTransactions_isEmpty() {
        assertTrue(viewModel.getTransactionListState().getValue() instanceof UiState.Empty);
    }

    @Test
    public void transactionListState_whenTransactionsExist_isSuccessWithDateHeadersAndRows() {
        LocalDate today = LocalDate.now();
        allTransactions.setValue(Arrays.asList(
                transactionOn("tx-1", today),
                transactionOn("tx-2", today),
                transactionOn("tx-3", today.minusDays(1))));

        UiState<List<TransactionListItem>> state = viewModel.getTransactionListState().getValue();

        assertTrue(state instanceof UiState.Success);
        List<TransactionListItem> items = ((UiState.Success<List<TransactionListItem>>) state).data;
        assertEquals(5, items.size());
        assertEquals("Hôm nay", ((TransactionListItem.Header) items.get(0)).title);
        assertTrue(items.get(1) instanceof TransactionListItem.Row);
        assertTrue(items.get(2) instanceof TransactionListItem.Row);
        assertEquals("Hôm qua", ((TransactionListItem.Header) items.get(3)).title);
        assertTrue(items.get(4) instanceof TransactionListItem.Row);
    }

    @Test
    public void quickAddVisible_whenEnabledAndOnline_isTrue() {
        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenDisabledInSettings_isFalse() {
        fakeAiRepository.setQuickAddEnabled(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenOffline_isFalse() {
        fakeAiRepository.setConnected(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_returnsWhenBackOnline() {
        fakeAiRepository.setConnected(false);
        fakeAiRepository.setConnected(true);

        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }
}