package com.longvuong.plix.presentation.category;

import com.longvuong.plix.data.local.entity.CategoryEntity;

public abstract class CategoryListItem {

    private CategoryListItem() {
    }

    public static final class Header extends CategoryListItem {
        public final String title;

        public Header(String title) {
            this.title = title;
        }
    }

    public static final class Row extends CategoryListItem {
        public final CategoryEntity category;

        public Row(CategoryEntity category) {
            this.category = category;
        }
    }
}