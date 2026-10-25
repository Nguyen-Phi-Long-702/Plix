package com.longvuong.plix.presentation.budget;

import com.longvuong.plix.data.local.entity.BudgetEntity;

import java.time.YearMonth;
import java.util.Map;

//Tên hiển thị của 1 ngân sách trên danh sách: ngân sách tổng, tên danh mục, hoặc "Danh mục không xác định" khi danh mục đã bị xoá
public final class BudgetDisplayName {
    private static final String OVERALL = "Ngân sách tổng";
    private static final String UNKNOWN_CATEGORY = "Danh mục không xác định";

    private BudgetDisplayName() {
    }

    //categoryNamesById chỉ chứa danh mục còn dùng (chưa bị xoá), nên danh mục đã xoá sẽ không có trong map
    public static String of(BudgetEntity budget, Map<String, String> categoryNamesById) {
        if (budget.categoryId == null) {
            return OVERALL;
        }
        String categoryName = categoryNamesById.get(budget.categoryId);
        return categoryName != null ? categoryName : UNKNOWN_CATEGORY;
    }

    //Nhãn kỳ ngân sách, vd period "2026-10" -> "Tháng 10/2026"
    public static String periodLabel(String period) {
        YearMonth yearMonth = YearMonth.parse(period);
        return "Tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear();
    }
}