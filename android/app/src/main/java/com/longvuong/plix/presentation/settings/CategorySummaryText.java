package com.longvuong.plix.presentation.settings;

import androidx.annotation.Nullable;

import com.longvuong.plix.data.local.entity.CategoryEntity;

import java.util.List;

//Dòng phụ dưới "Quản lý danh mục" ở màn Cài đặt, vd "12 hệ thống · 2 của bạn"
public final class CategorySummaryText {

    private CategorySummaryText() {
    }

    //Danh mục hệ thống có userId = null, danh mục do người dùng tạo có userId (cùng cách phân biệt nhãn "Hệ thống"/"Của bạn" ở màn Quản lý danh mục)
    public static String of(@Nullable List<CategoryEntity> categories) {
        int systemCount = 0;
        int customCount = 0;
        if (categories != null) {
            for (CategoryEntity category : categories) {
                if (category.userId == null) {
                    systemCount++;
                } else {
                    customCount++;
                }
            }
        }
        return systemCount + " hệ thống · " + customCount + " của bạn";
    }
}