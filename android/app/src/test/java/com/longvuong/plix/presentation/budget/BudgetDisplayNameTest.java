package com.longvuong.plix.presentation.budget;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.data.local.entity.BudgetEntity;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BudgetDisplayNameTest {
    private BudgetEntity budget(String categoryId) {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = "user-1";
        entity.period = "2026-10";
        entity.categoryId = categoryId;
        entity.limitAmount = 1000000;
        entity.thresholdPercent = 80;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    //Chỉ chứa danh mục còn dùng, giống dữ liệu từ CategoryDao.getActiveCategories (không có danh mục đã xoá)
    private Map<String, String> activeCategoryNames() {
        Map<String, String> names = new HashMap<>();
        names.put("sys_an_uong", "Ăn uống");
        names.put("sys_di_chuyen", "Di chuyển");
        return names;
    }

    @Test
    public void of_overallBudget_returnsOverallName() {
        assertEquals("Ngân sách tổng", BudgetDisplayName.of(budget(null), activeCategoryNames()));
    }

    @Test
    public void of_categoryStillActive_returnsCategoryName() {
        assertEquals("Ăn uống", BudgetDisplayName.of(budget("sys_an_uong"), activeCategoryNames()));
    }

    @Test
    public void of_categoryDeleted_returnsUnknownCategoryName() {
        assertEquals("Danh mục không xác định", BudgetDisplayName.of(budget("custom-category-da-xoa"), activeCategoryNames()));
    }

    @Test
    public void periodLabel_formatsMonthAndYear() {
        assertEquals("Tháng 10/2026", BudgetDisplayName.periodLabel("2026-10"));
        assertEquals("Tháng 9/2026", BudgetDisplayName.periodLabel("2026-09"));
    }
}