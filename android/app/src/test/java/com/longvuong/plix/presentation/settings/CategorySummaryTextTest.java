package com.longvuong.plix.presentation.settings;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategorySummaryTextTest {

    private CategoryEntity category(String id, String userId) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.userId = userId;
        entity.name = id;
        entity.type = "expense";
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    @Test
    public void of_systemAndCustomCategories_countsEachGroup() {
        List<CategoryEntity> categories = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            categories.add(category("sys_" + i, null));
        }
        categories.add(category("custom-1", "user-1"));
        categories.add(category("custom-2", "user-1"));

        assertEquals("12 hệ thống · 2 của bạn", CategorySummaryText.of(categories));
    }

    @Test
    public void of_onlySystemCategories_showsZeroCustom() {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(category("sys_an_uong", null));
        categories.add(category("sys_luong", null));

        assertEquals("2 hệ thống · 0 của bạn", CategorySummaryText.of(categories));
    }

    @Test
    public void of_emptyList_showsZeroForBoth() {
        assertEquals("0 hệ thống · 0 của bạn", CategorySummaryText.of(Collections.emptyList()));
    }

    @Test
    public void of_nullList_showsZeroForBoth() {
        assertEquals("0 hệ thống · 0 của bạn", CategorySummaryText.of(null));
    }
}