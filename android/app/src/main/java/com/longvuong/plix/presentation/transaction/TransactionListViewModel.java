package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TransactionListViewModel extends ViewModel {

    private final MediatorLiveData<UiState<List<TransactionEntity>>> transactionListState =
            new MediatorLiveData<>();

    @Inject
    public TransactionListViewModel(TransactionRepository transactionRepository) {
        transactionListState.setValue(new UiState.Loading<>());

        LiveData<List<TransactionEntity>> source = transactionRepository.getAll();
        transactionListState.addSource(source, transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                transactionListState.setValue(new UiState.Empty<>());
            } else {
                transactionListState.setValue(new UiState.Success<>(transactions));
            }
        });
    }

    public LiveData<UiState<List<TransactionEntity>>> getTransactionListState() {
        return transactionListState;
    }
}