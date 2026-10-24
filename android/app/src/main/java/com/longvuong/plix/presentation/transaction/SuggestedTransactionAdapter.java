package com.longvuong.plix.presentation.transaction;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.longvuong.plix.R;
import com.longvuong.plix.presentation.common.CategoryIconMapper;

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
        private static final float NO_AMOUNT_ROW_ALPHA = 0.85f; //dòng thiếu số tiền hiển thị mờ hơn các dòng khác, theo thiết kế

        private final MaterialCardView cardSuggestion;
        private final MaterialCheckBox checkSelected;
        private final View layoutIcon;
        private final ImageView imageIcon;
        private final TextView textTitle;
        private final TextView textDetail;
        private final TextView textAmount;
        private final Chip chipEnterAmount;
        private final ImageButton buttonEdit;

        SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSuggestion = itemView.findViewById(R.id.cardSuggestion);
            checkSelected = itemView.findViewById(R.id.checkSelected);
            layoutIcon = itemView.findViewById(R.id.layoutSuggestionIcon);
            imageIcon = itemView.findViewById(R.id.imageSuggestionIcon);
            textTitle = itemView.findViewById(R.id.textSuggestionTitle);
            textDetail = itemView.findViewById(R.id.textSuggestionDetail);
            textAmount = itemView.findViewById(R.id.textSuggestionAmount);
            chipEnterAmount = itemView.findViewById(R.id.chipEnterAmount);
            buttonEdit = itemView.findViewById(R.id.buttonEditSuggestion);
        }

        void bind(SuggestionItemUiModel item, OnSuggestionActionListener actionListener) {
            Context context = itemView.getContext();
            //3 kiểu dòng: bình thường / cần chọn danh mục (nền cảnh báo) / thiếu số tiền (nền xám, mờ). Thiếu số tiền được ưu tiên hơn thiếu danh mục
            boolean needsAmount = item.needsAmount;
            boolean needsCategoryOnly = item.needsCategory && !item.needsAmount;
            int typeColor = ContextCompat.getColor(context, item.income ? R.color.color_income : R.color.color_expense);

            cardSuggestion.setCardBackgroundColor(ContextCompat.getColor(context,
                    needsCategoryOnly ? R.color.warn_tint : (needsAmount ? R.color.surface_alt : R.color.surface)));
            cardSuggestion.setStrokeColor(ContextCompat.getColor(context,
                    needsCategoryOnly ? R.color.budget_progress_warning : (needsAmount ? R.color.border_strong : R.color.border)));
            cardSuggestion.setAlpha(needsAmount ? NO_AMOUNT_ROW_ALPHA : 1f);

            if (needsAmount || needsCategoryOnly) {
                layoutIcon.setBackgroundResource(R.drawable.bg_category_icon_plain);
                imageIcon.setImageResource(R.drawable.ic_warning);
                imageIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                        needsAmount ? R.color.text_tertiary : R.color.primary_dark)));
            } else {
                layoutIcon.setBackgroundResource(item.income ? R.drawable.bg_category_icon_income : R.drawable.bg_category_icon_expense);
                imageIcon.setImageResource(CategoryIconMapper.iconFor(item.categoryId));
                imageIcon.setImageTintList(ColorStateList.valueOf(typeColor));
            }

            textTitle.setText(item.title);
            textDetail.setText(item.detail);
            textDetail.setTextColor(ContextCompat.getColor(context,
                    needsCategoryOnly ? R.color.warn_strong : R.color.text_secondary));

            //Dòng thiếu số tiền: ẩn số tiền và nút sửa, hiện chip "Nhập tay" (mở cùng hộp thoại sửa để nhập số tiền và chọn danh mục)
            textAmount.setVisibility(needsAmount ? View.GONE : View.VISIBLE);
            textAmount.setText(item.amountText);
            textAmount.setTextColor(typeColor);
            chipEnterAmount.setVisibility(needsAmount ? View.VISIBLE : View.GONE);
            chipEnterAmount.setOnClickListener(v -> actionListener.onEditSuggestion(item));
            buttonEdit.setVisibility(needsAmount ? View.GONE : View.VISIBLE);
            buttonEdit.setOnClickListener(v -> actionListener.onEditSuggestion(item));

            //Gỡ listener cũ trước khi đặt trạng thái tick, tránh việc setChecked của dòng được tái sử dụng lại gọi ngược về ViewModel
            checkSelected.setOnCheckedChangeListener(null);
            checkSelected.setChecked(item.selected);
            checkSelected.setEnabled(!item.needsCategory && !item.needsAmount);
            checkSelected.setOnCheckedChangeListener((button, checked) -> actionListener.onSelectionChanged(item.itemId, checked));
        }
    }
}