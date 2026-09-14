package com.longvuong.plix.presentation.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    public interface OnCategoryActionListener {
        void onEditCategory(CategoryEntity category);
        void onDeleteCategory(CategoryEntity category);
    }
    private final OnCategoryActionListener actionListener;
    private List<CategoryEntity> categories = new ArrayList<>();
    private String currentUserId;
    public CategoryAdapter(OnCategoryActionListener actionListener) {
        this.actionListener = actionListener;
    }
    public void submitList(List<CategoryEntity> newCategories, @Nullable String currentUserId) {
        this.categories = newCategories != null ? newCategories : new ArrayList<>();
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position), currentUserId, actionListener);
    }
    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCategoryName;
        private final TextView textCategoryBadge;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategoryName = itemView.findViewById(R.id.textCategoryName);
            textCategoryBadge = itemView.findViewById(R.id.textCategoryBadge);
            buttonEdit = itemView.findViewById(R.id.buttonEditCategory);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteCategory);
        }

        void bind(CategoryEntity category, @Nullable String currentUserId, OnCategoryActionListener actionListener) {
            String typeLabel = "income".equals(category.type) ? "Thu nhập" : "Chi tiêu";
            textCategoryName.setText(category.name + " · " + typeLabel);
            boolean isCustom = category.userId != null;
            textCategoryBadge.setText(isCustom ? "Của bạn" : "Hệ thống");
            boolean isOwnedByCurrentUser = isCustom && category.userId.equals(currentUserId);
            int visibility = isOwnedByCurrentUser ? View.VISIBLE : View.GONE;
            buttonEdit.setVisibility(visibility);
            buttonDelete.setVisibility(visibility);
            buttonEdit.setOnClickListener(v -> actionListener.onEditCategory(category));
            buttonDelete.setOnClickListener(v -> actionListener.onDeleteCategory(category));
        }
    }
}