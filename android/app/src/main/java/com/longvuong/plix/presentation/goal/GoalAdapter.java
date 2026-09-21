package com.longvuong.plix.presentation.goal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.domain.usecase.goal.GoalProgress;
import com.longvuong.plix.domain.usecase.goal.GoalRequiredMonthly;
import com.longvuong.plix.domain.usecase.goal.GoalStatus;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {
    public interface OnGoalActionListener {
        void onGoalClick(GoalEntity goal);
        void onDeleteGoal(GoalEntity goal);
    }

    private final OnGoalActionListener actionListener;
    private List<GoalEntity> goals = new ArrayList<>();
    private Map<String, GoalProgress> progressByGoalId = new HashMap<>();

    public GoalAdapter(OnGoalActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<GoalEntity> newGoals) {
        this.goals = newGoals != null ? newGoals : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void submitProgress(Map<String, GoalProgress> newProgressByGoalId) {
        this.progressByGoalId = newProgressByGoalId != null ? newProgressByGoalId : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        holder.bind(goals.get(position), progressByGoalId, actionListener);
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        private final TextView textGoalName;
        private final TextView textGoalStatusBadge;
        private final TextView textGoalPercent;
        private final ImageButton buttonDeleteGoal;
        private final ProgressBar progressGoal;
        private final TextView textGoalAmount;
        private final TextView textGoalRequiredMonthly;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textGoalName = itemView.findViewById(R.id.textGoalName);
            textGoalStatusBadge = itemView.findViewById(R.id.textGoalStatusBadge);
            textGoalPercent = itemView.findViewById(R.id.textGoalPercent);
            buttonDeleteGoal = itemView.findViewById(R.id.buttonDeleteGoal);
            progressGoal = itemView.findViewById(R.id.progressGoal);
            textGoalAmount = itemView.findViewById(R.id.textGoalAmount);
            textGoalRequiredMonthly = itemView.findViewById(R.id.textGoalRequiredMonthly);
        }

        void bind(GoalEntity goal, Map<String, GoalProgress> progressByGoalId, OnGoalActionListener actionListener) {
            textGoalName.setText(goal.name);

            GoalProgress progress = progressByGoalId.get(goal.id);
            GoalStatus status = progress != null ? progress.status : GoalStatus.ACTIVE;
            int percent = progress != null ? progress.percent : 0;

            textGoalStatusBadge.setText(statusLabel(status));
            textGoalStatusBadge.setTextColor(statusColor(status));
            textGoalPercent.setText(percent + "%");
            progressGoal.setProgress(Math.max(0, Math.min(percent, 100)));

            textGoalAmount.setText("Đã có " + formatCurrency(goal.currentAmount) + " / " + formatCurrency(goal.targetAmount) + " · Hạn " + formatDate(goal.deadline));

            textGoalRequiredMonthly.setText(progress != null ? requiredMonthlyLabel(progress.requiredMonthly) : "");

            itemView.setOnClickListener(v -> actionListener.onGoalClick(goal));
            buttonDeleteGoal.setOnClickListener(v -> actionListener.onDeleteGoal(goal));
        }

        private String statusLabel(GoalStatus status) {
            switch (status) {
                case ACHIEVED:
                    return "Đã đạt mục tiêu";
                case EXPIRED:
                    return "Đã hết hạn";
                case ACTIVE:
                default:
                    return "Đang thực hiện";
            }
        }

        private int statusColor(GoalStatus status) {
            switch (status) {
                case ACHIEVED:
                    return ContextCompat.getColor(itemView.getContext(), R.color.color_income);
                case EXPIRED:
                    return ContextCompat.getColor(itemView.getContext(), R.color.budget_progress_danger);
                case ACTIVE:
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.black);
            }
        }

        private String requiredMonthlyLabel(GoalRequiredMonthly requiredMonthly) {
            switch (requiredMonthly.caseType) {
                case ACHIEVED:
                    return "Đã đạt mục tiêu, không cần tiết kiệm thêm";
                case EXCEEDED:
                    return "Đã vượt mục tiêu";
                case DUE_NOW:
                    return "Đến hạn, cần bổ sung ngay " + formatCurrency(requiredMonthly.monthlyAmount);
                case NORMAL:
                default:
                    return "Cần tiết kiệm " + formatCurrency(requiredMonthly.monthlyAmount) + "/tháng";
            }
        }

        private String formatCurrency(long amount) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(amount) + "đ";
        }

        private String formatDate(long epochMs) {
            return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }
}