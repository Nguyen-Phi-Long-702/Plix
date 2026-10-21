package com.longvuong.plix.presentation.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.longvuong.plix.R;

import java.util.HashMap;
import java.util.Map;

//Icon của danh mục, ánh xạ ở tầng presentation theo mã danh mục hệ thống (sys_*) vì CategoryEntity không có cột icon
public final class CategoryIconMapper {
    @DrawableRes
    private static final int DEFAULT_ICON = R.drawable.ic_tag;

    private static final Map<String, Integer> ICON_BY_SYSTEM_CATEGORY_ID = new HashMap<>();

    static {
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_an_uong", R.drawable.ic_food);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_di_chuyen", R.drawable.ic_transport);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_nhu_yeu_pham", R.drawable.ic_grocery);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_giai_tri", R.drawable.ic_entertainment);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_hoa_don", R.drawable.ic_bills);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_suc_khoe", R.drawable.ic_health);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_giao_duc", R.drawable.ic_education);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_nha_o", R.drawable.ic_housing);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_mua_sam", R.drawable.ic_shopping);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_khac_chi", R.drawable.ic_other);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_luong", R.drawable.ic_salary);
        ICON_BY_SYSTEM_CATEGORY_ID.put("sys_thu_nhap_khac", R.drawable.ic_gift);
    }

    private CategoryIconMapper() {
    }

    //Danh mục người dùng tạo (hoặc categoryId null) dùng icon mặc định
    @DrawableRes
    public static int iconFor(@Nullable String categoryId) {
        if (categoryId == null) {
            return DEFAULT_ICON;
        }
        Integer icon = ICON_BY_SYSTEM_CATEGORY_ID.get(categoryId);
        return icon != null ? icon : DEFAULT_ICON;
    }
}