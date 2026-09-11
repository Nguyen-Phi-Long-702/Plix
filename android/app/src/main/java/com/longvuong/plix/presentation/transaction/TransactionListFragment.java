package com.longvuong.plix.presentation.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransactionListFragment extends Fragment {

    private TransactionListViewModel viewModel;
    private TransactionAdapter adapter;

    private RecyclerView recyclerTransactions;
    private ProgressBar progressLoading;
    private TextView textEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionListViewModel.class);

        bindViews(view);
        setupRecyclerView();

        viewModel.getTransactionListState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void bindViews(View view) {
        recyclerTransactions = view.findViewById(R.id.recyclerTransactions);
        progressLoading = view.findViewById(R.id.progressLoading);
        textEmptyState = view.findViewById(R.id.textEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTransactions.setAdapter(adapter);
    }

    private void renderState(UiState<List<TransactionEntity>> state) {
        progressLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        textEmptyState.setVisibility(
                (state instanceof UiState.Empty || state instanceof UiState.Error) ? View.VISIBLE : View.GONE);
        recyclerTransactions.setVisibility(state instanceof UiState.Success ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            adapter.submitList(((UiState.Success<List<TransactionEntity>>) state).data);
        } else if (state instanceof UiState.Error) {
            textEmptyState.setText(((UiState.Error<List<TransactionEntity>>) state).message);
        } else if (state instanceof UiState.Empty) {
            textEmptyState.setText("Chưa có giao dịch nào phù hợp");
        }
    }
}