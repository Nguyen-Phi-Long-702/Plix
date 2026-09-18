package com.longvuong.plix.presentation.budget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.domain.usecase.budget.BudgetProgress;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    public interface OnBudgetClickListener {
        void onBudgetClick(BudgetEntity budget);
    }

    private final OnBudgetClickListener clickListener;
    private List<BudgetEntity> budgets = new ArrayList<>();
    private Map<String, String> categoryNamesById = new HashMap<>();
    private Map<String, BudgetProgress> progressByBudgetId = new HashMap<>();

    public BudgetAdapter(OnBudgetClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<BudgetEntity> newBudgets) {
        this.budgets = newBudgets != null ? newBudgets : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void submitCategories(List<CategoryEntity> categories) {
        Map<String, String> map = new HashMap<>();
        if (categories != null) {
            for (CategoryEntity category : categories) {
                map.put(category.id, category.name);
            }
        }
        this.categoryNamesById = map;
        notifyDataSetChanged();
    }

    public void submitProgress(Map<String, BudgetProgress> newProgressByBudgetId) {
        this.progressByBudgetId = newProgressByBudgetId != null ? newProgressByBudgetId : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        holder.bind(budgets.get(position), categoryNamesById, progressByBudgetId, clickListener);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final TextView textBudgetName;
        private final TextView textThreshold;
        private final TextView textPercent;
        private final ProgressBar progressBudget;
        private final TextView textSpentOverLimit;

        BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            textBudgetName = itemView.findViewById(R.id.textBudgetName);
            textThreshold = itemView.findViewById(R.id.textThreshold);
            textPercent = itemView.findViewById(R.id.textPercent);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            textSpentOverLimit = itemView.findViewById(R.id.textSpentOverLimit);
        }

        void bind(BudgetEntity budget, Map<String, String> categoryNamesById,
                  Map<String, BudgetProgress> progressByBudgetId, OnBudgetClickListener clickListener) {
            String name;
            if (budget.categoryId == null) {
                name = "Ngân sách tổng";
            } else {
                String categoryName = categoryNamesById.get(budget.categoryId);
                name = categoryName != null ? categoryName : "Danh mục đã xoá";
            }
            textBudgetName.setText(name);
            textThreshold.setText("Ngưỡng cảnh báo " + budget.thresholdPercent + "%");
            BudgetProgress progress = progressByBudgetId.get(budget.id);
            long spentAmount = progress != null ? progress.spentAmount : 0L;
            int percent = progress != null ? progress.percent : 0;
            textPercent.setText(percent + "%");
            progressBudget.setProgress(Math.min(percent, 100));
            textSpentOverLimit.setText("Đã chi " + formatCurrency(spentAmount) + " / " + formatCurrency(budget.limitAmount));
            itemView.setOnClickListener(v -> clickListener.onBudgetClick(budget));
        }
        private String formatCurrency(long amount) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(amount) + "đ";
        }
    }
}