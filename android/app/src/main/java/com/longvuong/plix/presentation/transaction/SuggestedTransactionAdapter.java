package com.longvuong.plix.presentation.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.longvuong.plix.R;

import java.util.ArrayList;
import java.util.List;

public class SuggestedTransactionAdapter extends RecyclerView.Adapter<SuggestedTransactionAdapter.SuggestionViewHolder> {
    public interface OnSuggestionActionListener {
        void onSelectionChanged(String itemId, boolean selected);

        void onEditSuggestion(SuggestionItemUiModel item);
    }

    private final OnSuggestionActionListener actionListener;
    private List<SuggestionItemUiModel> items = new ArrayList<>();

    public SuggestedTransactionAdapter(OnSuggestionActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<SuggestionItemUiModel> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggested_transaction, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        holder.bind(items.get(position), actionListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox checkSelected;
        private final TextView textTitle;
        private final TextView textDetail;
        private final TextView textAmount;
        private final ImageButton buttonEdit;

        SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            checkSelected = itemView.findViewById(R.id.checkSelected);
            textTitle = itemView.findViewById(R.id.textSuggestionTitle);
            textDetail = itemView.findViewById(R.id.textSuggestionDetail);
            textAmount = itemView.findViewById(R.id.textSuggestionAmount);
            buttonEdit = itemView.findViewById(R.id.buttonEditSuggestion);
        }

        void bind(SuggestionItemUiModel item, OnSuggestionActionListener actionListener) {
            textTitle.setText(item.title);
            textDetail.setText(item.detail);
            textDetail.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    item.needsCategory ? R.color.warn_strong : R.color.info_text));
            textAmount.setText(item.amountText);
            textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    item.income ? R.color.color_income : R.color.color_expense));
            itemView.setBackgroundResource(item.needsCategory ? R.drawable.bg_anomaly_banner : 0);

            //Gỡ listener cũ trước khi đặt trạng thái tick, tránh việc setChecked của dòng được tái sử dụng lại gọi ngược về ViewModel
            checkSelected.setOnCheckedChangeListener(null);
            checkSelected.setChecked(item.selected);
            checkSelected.setEnabled(!item.needsCategory);
            checkSelected.setOnCheckedChangeListener((button, checked) -> actionListener.onSelectionChanged(item.itemId, checked));

            buttonEdit.setOnClickListener(v -> actionListener.onEditSuggestion(item));
        }
    }
}