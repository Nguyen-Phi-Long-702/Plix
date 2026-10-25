package com.longvuong.plix.presentation.budget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.domain.usecase.budget.BudgetProgress;
import com.longvuong.plix.presentation.common.CategoryIconMapper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    public interface OnBudgetActionListener {
        void onBudgetClick(BudgetEntity budget);

        void onDeleteBudget(BudgetEntity budget, String displayName);
    }

    private final OnBudgetActionListener actionListener;
    private List<BudgetEntity> budgets = new ArrayList<>();
    private Map<String, String> categoryNamesById = new HashMap<>();
    private Map<String, BudgetProgress> progressByBudgetId = new HashMap<>();

    public BudgetAdapter(OnBudgetActionListener actionListener) {
        this.actionListener = actionListener;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        holder.bind(budgets.get(position), categoryNamesById, progressByBudgetId, actionListener);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageBudgetIcon;
        private final TextView textBudgetName;
        private final TextView textThreshold;
        private final TextView textPercent;
        private final ImageButton buttonDeleteBudget;
        private final LinearProgressIndicator progressBudget;
        private final TextView textSpent;
        private final TextView textLimit;
        private final View layoutThresholdWarning;

        BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBudgetIcon = itemView.findViewById(R.id.imageBudgetIcon);
            textBudgetName = itemView.findViewById(R.id.textBudgetName);
            textThreshold = itemView.findViewById(R.id.textThreshold);
            textPercent = itemView.findViewById(R.id.textPercent);
            buttonDeleteBudget = itemView.findViewById(R.id.buttonDeleteBudget);
            progressBudget = itemView.findViewById(R.id.progressBudget);
            textSpent = itemView.findViewById(R.id.textSpent);
            textLimit = itemView.findViewById(R.id.textLimit);
            layoutThresholdWarning = itemView.findViewById(R.id.layoutThresholdWarning);
        }

        void bind(BudgetEntity budget, Map<String, String> categoryNamesById, Map<String, BudgetProgress> progressByBudgetId, OnBudgetActionListener actionListener) {
            String name = BudgetDisplayName.of(budget, categoryNamesById);
            imageBudgetIcon.setImageResource(budget.categoryId == null
                    ? R.drawable.ic_wallet : CategoryIconMapper.iconFor(budget.categoryId));
            textBudgetName.setText(name);
            textThreshold.setText("Ngưỡng cảnh báo " + budget.thresholdPercent + "%");
            BudgetProgress progress = progressByBudgetId.get(budget.id);
            long spentAmount = progress != null ? progress.spentAmount : 0L;
            int percent = progress != null ? progress.percent : 0;
            BudgetProgressLevel level = BudgetProgressLevel.of(percent, budget.thresholdPercent);
            textPercent.setText(percent + "%");
            textPercent.setTextColor(color(percentColorRes(level)));
            progressBudget.setIndicatorColor(color(barColorRes(level)));
            progressBudget.setProgressCompat(Math.min(percent, 100), false);
            textSpent.setText("Đã chi " + formatCurrency(spentAmount));
            textLimit.setText("Hạn mức " + formatCurrency(budget.limitAmount));
            layoutThresholdWarning.setVisibility(level == BudgetProgressLevel.NORMAL ? View.GONE : View.VISIBLE);
            itemView.setOnClickListener(v -> actionListener.onBudgetClick(budget));
            buttonDeleteBudget.setOnClickListener(v -> actionListener.onDeleteBudget(budget, name));
        }

        @ColorRes
        private int barColorRes(BudgetProgressLevel level) {
            switch (level) {
                case DANGER:
                    return R.color.budget_progress_danger;
                case WARNING:
                    return R.color.budget_progress_warning;
                default:
                    return R.color.budget_progress_normal;
            }
        }

        @ColorRes
        private int percentColorRes(BudgetProgressLevel level) {
            switch (level) {
                case DANGER:
                    return R.color.budget_progress_danger;
                case WARNING:
                    return R.color.warn_strong;
                default:
                    return R.color.text_primary;
            }
        }

        private int color(@ColorRes int colorRes) {
            return ContextCompat.getColor(itemView.getContext(), colorRes);
        }

        private String formatCurrency(long amount) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(amount) + "đ";
        }
    }
}