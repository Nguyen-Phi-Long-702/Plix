package com.longvuong.plix.presentation.category;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.presentation.common.CategoryIconMapper;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnCategoryActionListener {
        void onEditCategory(CategoryEntity category);
        void onDeleteCategory(CategoryEntity category);
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ROW = 1;

    private final OnCategoryActionListener actionListener;
    private List<CategoryListItem> items = new ArrayList<>();
    private String currentUserId;

    public CategoryAdapter(OnCategoryActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<CategoryListItem> newItems, @Nullable String currentUserId) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof CategoryListItem.Header ? VIEW_TYPE_HEADER : VIEW_TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_group_header, parent, false));
        }
        return new RowViewHolder(inflater.inflate(R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CategoryListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((CategoryListItem.Header) item);
            return;
        }
        //Vạch kẻ chỉ hiện giữa các danh mục trong cùng 1 nhóm (không hiện ở danh mục cuối nhóm)
        boolean hasRowBelow = position + 1 < items.size() && items.get(position + 1) instanceof CategoryListItem.Row;
        ((RowViewHolder) holder).bind(((CategoryListItem.Row) item).category, currentUserId, hasRowBelow, actionListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textGroupTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textGroupTitle = itemView.findViewById(R.id.textGroupTitle);
        }

        void bind(CategoryListItem.Header header) {
            textGroupTitle.setText(header.title);
        }
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        private final View layoutCategoryIcon;
        private final ImageView imageCategoryIcon;
        private final TextView textCategoryName;
        private final TextView textCategoryBadge;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        private final View viewDivider;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCategoryIcon = itemView.findViewById(R.id.layoutCategoryIcon);
            imageCategoryIcon = itemView.findViewById(R.id.imageCategoryIcon);
            textCategoryName = itemView.findViewById(R.id.textCategoryName);
            textCategoryBadge = itemView.findViewById(R.id.textCategoryBadge);
            buttonEdit = itemView.findViewById(R.id.buttonEditCategory);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteCategory);
            viewDivider = itemView.findViewById(R.id.viewDivider);
        }

        void bind(CategoryEntity category, @Nullable String currentUserId, boolean showDivider, OnCategoryActionListener actionListener) {
            Context context = itemView.getContext();
            boolean isIncome = "income".equals(category.type);
            int typeColor = ContextCompat.getColor(context, isIncome ? R.color.color_income : R.color.color_expense);

            layoutCategoryIcon.setBackgroundResource(isIncome ? R.drawable.bg_category_icon_income : R.drawable.bg_category_icon_expense);
            imageCategoryIcon.setImageResource(CategoryIconMapper.iconFor(category.id));
            imageCategoryIcon.setImageTintList(ColorStateList.valueOf(typeColor));

            textCategoryName.setText(category.name);
            boolean isCustom = category.userId != null;
            textCategoryBadge.setText(isCustom ? "Của bạn" : "Hệ thống");
            textCategoryBadge.setBackgroundResource(isCustom ? R.drawable.bg_badge_category_custom : R.drawable.bg_badge_category_system);
            textCategoryBadge.setTextColor(ContextCompat.getColor(context, isCustom ? R.color.primary_dark : R.color.text_secondary));

            boolean isOwnedByCurrentUser = isCustom && category.userId.equals(currentUserId);
            int visibility = isOwnedByCurrentUser ? View.VISIBLE : View.GONE;
            buttonEdit.setVisibility(visibility);
            buttonDelete.setVisibility(visibility);
            buttonEdit.setOnClickListener(v -> actionListener.onEditCategory(category));
            buttonDelete.setOnClickListener(v -> actionListener.onDeleteCategory(category));

            viewDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }
    }
}