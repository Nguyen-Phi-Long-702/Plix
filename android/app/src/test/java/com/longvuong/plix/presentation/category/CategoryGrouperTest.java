package com.longvuong.plix.presentation.category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryGrouperTest {

    private CategoryEntity category(String id, String name, String type) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.userId = null;
        entity.name = name;
        entity.type = type;
        entity.updatedAt = 0L;
        entity.syncStatus = "synced";
        entity.isDeleted = false;
        return entity;
    }

    private String titleAt(List<CategoryListItem> items, int index) {
        return ((CategoryListItem.Header) items.get(index)).title;
    }

    private String idAt(List<CategoryListItem> items, int index) {
        return ((CategoryListItem.Row) items.get(index)).category.id;
    }

    @Test
    public void group_emptyList_returnsEmptyList() {
        assertTrue(CategoryGrouper.group(Collections.emptyList()).isEmpty());
    }

    @Test
    public void group_expenseAndIncome_returnsExpenseGroupThenIncomeGroupWithCounts() {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(category("c-1", "Ăn uống", "expense"));
        categories.add(category("c-2", "Di chuyển", "expense"));
        categories.add(category("c-3", "Lương", "income"));

        List<CategoryListItem> items = CategoryGrouper.group(categories);

        assertEquals(5, items.size());
        assertEquals("Chi tiêu (2)", titleAt(items, 0));
        assertEquals("c-1", idAt(items, 1));
        assertEquals("c-2", idAt(items, 2));
        assertEquals("Thu nhập (1)", titleAt(items, 3));
        assertEquals("c-3", idAt(items, 4));
    }

    @Test
    public void group_mixedInputOrder_keepsExpenseFirstAndOriginalOrderInsideEachGroup() {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(category("c-income-1", "Lương", "income"));
        categories.add(category("c-expense-1", "Ăn uống", "expense"));
        categories.add(category("c-income-2", "Freelance", "income"));
        categories.add(category("c-expense-2", "Di chuyển", "expense"));

        List<CategoryListItem> items = CategoryGrouper.group(categories);

        assertEquals(6, items.size());
        assertEquals("Chi tiêu (2)", titleAt(items, 0));
        assertEquals("c-expense-1", idAt(items, 1));
        assertEquals("c-expense-2", idAt(items, 2));
        assertEquals("Thu nhập (2)", titleAt(items, 3));
        assertEquals("c-income-1", idAt(items, 4));
        assertEquals("c-income-2", idAt(items, 5));
    }

    @Test
    public void group_onlyExpense_hasNoIncomeHeader() {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(category("c-1", "Ăn uống", "expense"));

        List<CategoryListItem> items = CategoryGrouper.group(categories);

        assertEquals(2, items.size());
        assertEquals("Chi tiêu (1)", titleAt(items, 0));
    }

    @Test
    public void group_onlyIncome_hasNoExpenseHeader() {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(category("c-1", "Lương", "income"));

        List<CategoryListItem> items = CategoryGrouper.group(categories);

        assertEquals(2, items.size());
        assertEquals("Thu nhập (1)", titleAt(items, 0));
    }
}