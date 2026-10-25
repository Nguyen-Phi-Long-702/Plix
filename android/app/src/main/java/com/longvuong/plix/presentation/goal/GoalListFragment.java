package com.longvuong.plix.presentation.goal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.longvuong.plix.presentation.common.EmptyStateView;
import com.google.android.material.snackbar.Snackbar;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.presentation.common.UiState;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GoalListFragment extends Fragment {
    private GoalListViewModel viewModel;
    private GoalAdapter adapter;
    private RecyclerView recyclerGoals;
    private View layoutStateLoading;
    private EmptyStateView viewEmptyState;
    private FloatingActionButton fabAddGoal;
    private MaterialButton btnTabBudget;
    private MaterialButton btnTabGoal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goal_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GoalListViewModel.class);
        bindViews(view);
        setupTabToggle();
        setupRecyclerView();
        fabAddGoal.setOnClickListener(v -> navigateToAddGoal());
        viewEmptyState.setOnActionClickListener(v -> navigateToAddGoal());
        viewModel.getGoalListState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getProgressByGoalId().observe(getViewLifecycleOwner(), adapter::submitProgress);
        viewModel.getDeleteState().observe(getViewLifecycleOwner(), this::renderDeleteState);
    }

    private void bindViews(View view) {
        recyclerGoals = view.findViewById(R.id.recyclerGoals);
        layoutStateLoading = view.findViewById(R.id.layoutStateLoading);
        viewEmptyState = view.findViewById(R.id.viewEmptyState);
        fabAddGoal = view.findViewById(R.id.fabAddGoal);
        btnTabBudget = view.findViewById(R.id.btnTabBudget);
        btnTabGoal = view.findViewById(R.id.btnTabGoal);
    }

    private void setupTabToggle() {
        btnTabGoal.setSelected(true);
        btnTabBudget.setOnClickListener(v -> {
            NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.goalListFragment, true).build();
            Navigation.findNavController(requireView()).navigate(R.id.action_goalListFragment_to_budgetGoalFragment, null, options);
        });
    }

    private void setupRecyclerView() {
        adapter = new GoalAdapter(new GoalAdapter.OnGoalActionListener() {
            @Override
            public void onGoalClick(GoalEntity goal) {
                navigateToEditGoal(goal);
            }

            @Override
            public void onDeleteGoal(GoalEntity goal) {
                showDeleteConfirmationDialog(goal);
            }
        });
        recyclerGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerGoals.setAdapter(adapter);
    }

    private void navigateToAddGoal() {
        Navigation.findNavController(requireView()).navigate(R.id.action_goalListFragment_to_addEditGoalFragment);
    }

    private void navigateToEditGoal(GoalEntity goal) {
        Bundle args = new Bundle();
        args.putString(AddEditGoalViewModel.ARG_GOAL_ID, goal.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_goalListFragment_to_addEditGoalFragment, args);
    }

    private void showDeleteConfirmationDialog(GoalEntity goal) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá mục tiêu \"" + goal.name + "\"?")
                .setMessage("Hành động này không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (d, which) -> viewModel.deleteGoal(goal))
                .show();
    }

    private void renderState(UiState<List<GoalEntity>> state) {
        layoutStateLoading.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
        viewEmptyState.setVisibility(state instanceof UiState.Empty ? View.VISIBLE : View.GONE);
        boolean hasData = state instanceof UiState.Success;
        recyclerGoals.setVisibility(hasData ? View.VISIBLE : View.GONE);
        if (hasData) {
            adapter.submitList(((UiState.Success<List<GoalEntity>>) state).data);
        }
    }

    private void renderDeleteState(UiState<Void> state) {
        if (state instanceof UiState.Success) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Đã xoá mục tiêu", Snackbar.LENGTH_SHORT).show();
        } else if (state instanceof UiState.Error) {
            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    ((UiState.Error<Void>) state).message, Snackbar.LENGTH_LONG).show();
        }
    }
}