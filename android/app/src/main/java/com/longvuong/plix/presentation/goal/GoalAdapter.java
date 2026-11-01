package com.longvuong.plix.presentation.goal;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.domain.usecase.goal.GoalProgress;
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
        private final CircularProgressIndicator progressGoal;
        private final TextView textGoalAmount;
        private final TextView textGoalDeadline;
        private final TextView textGoalRequiredMonthly;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textGoalName = itemView.findViewById(R.id.textGoalName);
            textGoalStatusBadge = itemView.findViewById(R.id.textGoalStatusBadge);
            textGoalPercent = itemView.findViewById(R.id.textGoalPercent);
            buttonDeleteGoal = itemView.findViewById(R.id.buttonDeleteGoal);
            progressGoal = itemView.findViewById(R.id.progressGoal);
            textGoalAmount = itemView.findViewById(R.id.textGoalAmount);
            textGoalDeadline = itemView.findViewById(R.id.textGoalDeadline);
            textGoalRequiredMonthly = itemView.findViewById(R.id.textGoalRequiredMonthly);
        }

        void bind(GoalEntity goal, Map<String, GoalProgress> progressByGoalId, OnGoalActionListener actionListener) {
            textGoalName.setText(goal.name);

            GoalProgress progress = progressByGoalId.get(goal.id);
            GoalStatus status = progress != null ? progress.status : GoalStatus.ACTIVE;
            int percent = progress != null ? Math.max(0, Math.min(progress.percent, 100)) : 0;

            textGoalStatusBadge.setText(GoalDisplayText.statusLabel(status));
            textGoalStatusBadge.setTextColor(color(GoalDisplayText.statusTextColorRes(status)));
            textGoalStatusBadge.setBackgroundTintList(ColorStateList.valueOf(color(GoalDisplayText.statusBackgroundColorRes(status))));
            textGoalPercent.setText(percent + "%");
            progressGoal.setIndicatorColor(color(percent >= 100 ? R.color.primary : R.color.color_income));
            progressGoal.setProgressCompat(percent, false);

            textGoalAmount.setText(formatCurrency(goal.currentAmount) + " / " + formatCurrency(goal.targetAmount));
            textGoalDeadline.setText("Hạn: " + formatDate(goal.deadline));

            if (progress != null) {
                textGoalRequiredMonthly.setText(GoalDisplayText.requiredMonthlyText(
                        status, progress.requiredMonthly, goal.currentAmount, goal.targetAmount));
                textGoalRequiredMonthly.setTextColor(color(GoalDisplayText.requiredMonthlyColorRes(status, progress.requiredMonthly)));
            } else {
                textGoalRequiredMonthly.setText("");
            }

            itemView.setOnClickListener(v -> actionListener.onGoalClick(goal));
            buttonDeleteGoal.setOnClickListener(v -> actionListener.onDeleteGoal(goal));
        }

        private int color(@ColorRes int colorRes) {
            return ContextCompat.getColor(itemView.getContext(), colorRes);
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