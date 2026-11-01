package com.longvuong.plix.presentation.category;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

public final class CategoryGrouper {

    private CategoryGrouper() {
    }

    public static List<CategoryListItem> group(List<CategoryEntity> categories) {
        List<CategoryEntity> expenseCategories = new ArrayList<>();
        List<CategoryEntity> incomeCategories = new ArrayList<>();
        for (CategoryEntity category : categories) {
            if ("income".equals(category.type)) {
                incomeCategories.add(category);
            } else {
                expenseCategories.add(category);
            }
        }

        List<CategoryListItem> items = new ArrayList<>();
        addGroup(items, "Chi tiêu", expenseCategories);
        addGroup(items, "Thu nhập", incomeCategories);
        return items;
    }

    private static void addGroup(List<CategoryListItem> items, String title, List<CategoryEntity> categories) {
        if (categories.isEmpty()) {
            return;
        }
        items.add(new CategoryListItem.Header(title + " (" + categories.size() + ")"));
        for (CategoryEntity category : categories) {
            items.add(new CategoryListItem.Row(category));
        }
    }
}