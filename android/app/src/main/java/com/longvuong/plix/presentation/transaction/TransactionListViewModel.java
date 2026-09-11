package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
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
    private final MutableLiveData<String> searchQueryTrigger = new MutableLiveData<>("");

    private List<TransactionEntity> latestRawTransactions = new ArrayList<>();
    private String selectedCategoryId;
    private Long selectedStartDate;
    private Long selectedEndDate;
    private String currentSearchQuery = "";

    @Inject
    public TransactionListViewModel(TransactionRepository transactionRepository,
                                    CategoryRepository categoryRepository,
                                    FilterTransactionsUseCase filterTransactionsUseCase) {
        this.categoryRepository = categoryRepository;
        this.filterTransactionsUseCase = filterTransactionsUseCase;

        transactionListState.setValue(new UiState.Loading<>());

        transactionListState.addSource(transactionRepository.getAll(), transactions -> {
            latestRawTransactions = transactions != null ? transactions : new ArrayList<>();
            if (currentSearchQuery.isEmpty()) {
                publishFilteredResult();
            }
        });

        LiveData<List<TransactionEntity>> searchResultsSource = Transformations.switchMap(
                searchQueryTrigger,
                query -> {
                    if (query == null || query.trim().isEmpty()) {
                        return new MutableLiveData<List<TransactionEntity>>(new ArrayList<>());
                    }
                    return transactionRepository.searchByNote(query.trim());
                });

        transactionListState.addSource(searchResultsSource, results -> {
            if (!currentSearchQuery.isEmpty()) {
                publish(results);
            }
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
        if (currentSearchQuery.isEmpty()) {
            publishFilteredResult();
        }
    }

    public void setDateRange(@Nullable Long startDate, @Nullable Long endDate) {
        this.selectedStartDate = startDate;
        this.selectedEndDate = endDate;
        if (currentSearchQuery.isEmpty()) {
            publishFilteredResult();
        }
    }

    public void setSearchQuery(@Nullable String query) {
        currentSearchQuery = query != null ? query.trim() : "";
        searchQueryTrigger.setValue(currentSearchQuery);
        if (currentSearchQuery.isEmpty()) {
            publishFilteredResult();
        }
    }

    private void publishFilteredResult() {
        List<TransactionEntity> filtered = filterTransactionsUseCase.execute(
                latestRawTransactions, selectedCategoryId, selectedStartDate, selectedEndDate);
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