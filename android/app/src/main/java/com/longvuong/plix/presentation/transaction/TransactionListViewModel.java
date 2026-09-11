package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.common.UiState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionListViewModel extends ViewModel {

    private final CategoryRepository categoryRepository;
    private final FilterTransactionsUseCase filterTransactionsUseCase;

    private final MediatorLiveData<UiState<List<TransactionEntity>>> transactionListState =
            new MediatorLiveData<>();

    private List<TransactionEntity> latestRawTransactions = new ArrayList<>();
    private String selectedCategoryId;

    @Inject
    public TransactionListViewModel(TransactionRepository transactionRepository,
                                    CategoryRepository categoryRepository,
                                    FilterTransactionsUseCase filterTransactionsUseCase) {
        this.categoryRepository = categoryRepository;
        this.filterTransactionsUseCase = filterTransactionsUseCase;

        transactionListState.setValue(new UiState.Loading<>());

        transactionListState.addSource(transactionRepository.getAll(), transactions -> {
            latestRawTransactions = transactions != null ? transactions : new ArrayList<>();
            publishFilteredResult();
        });
    }

    public LiveData<UiState<List<TransactionEntity>>> getTransactionListState() {
        return transactionListState;
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categoryRepository.getActiveCategories();
    }

    public void setSelectedCategory(@Nullable String categoryId) {
        this.selectedCategoryId = categoryId;
        publishFilteredResult();
    }

    private void publishFilteredResult() {
        List<TransactionEntity> filtered = filterTransactionsUseCase.execute(
                latestRawTransactions, selectedCategoryId, null, null);
        publish(filtered);
    }

    private void publish(List<TransactionEntity> list) {
        if (list == null || list.isEmpty()) {
            transactionListState.setValue(new UiState.Empty<>());
        } else {
            transactionListState.setValue(new UiState.Success<>(list));
        }
    }
}