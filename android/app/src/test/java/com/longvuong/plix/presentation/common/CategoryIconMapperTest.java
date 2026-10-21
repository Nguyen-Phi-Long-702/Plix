package com.longvuong.plix.presentation.common;

import static org.junit.Assert.assertEquals;

import com.longvuong.plix.R;

import org.junit.Test;

public class CategoryIconMapperTest {
    @Test
    public void iconFor_systemExpenseCategories_returnsMatchingIcon() {
        assertEquals(R.drawable.ic_food, CategoryIconMapper.iconFor("sys_an_uong"));
        assertEquals(R.drawable.ic_transport, CategoryIconMapper.iconFor("sys_di_chuyen"));
        assertEquals(R.drawable.ic_grocery, CategoryIconMapper.iconFor("sys_nhu_yeu_pham"));
        assertEquals(R.drawable.ic_entertainment, CategoryIconMapper.iconFor("sys_giai_tri"));
        assertEquals(R.drawable.ic_bills, CategoryIconMapper.iconFor("sys_hoa_don"));
        assertEquals(R.drawable.ic_health, CategoryIconMapper.iconFor("sys_suc_khoe"));
        assertEquals(R.drawable.ic_education, CategoryIconMapper.iconFor("sys_giao_duc"));
        assertEquals(R.drawable.ic_housing, CategoryIconMapper.iconFor("sys_nha_o"));
        assertEquals(R.drawable.ic_shopping, CategoryIconMapper.iconFor("sys_mua_sam"));
        assertEquals(R.drawable.ic_other, CategoryIconMapper.iconFor("sys_khac_chi"));
    }

    @Test
    public void iconFor_systemIncomeCategories_returnsMatchingIcon() {
        assertEquals(R.drawable.ic_salary, CategoryIconMapper.iconFor("sys_luong"));
        assertEquals(R.drawable.ic_gift, CategoryIconMapper.iconFor("sys_thu_nhap_khac"));
    }

    @Test
    public void iconFor_userCategory_returnsDefaultIcon() {
        assertEquals(R.drawable.ic_tag, CategoryIconMapper.iconFor("custom-category-cua-nguoi-dung"));
    }

    @Test
    public void iconFor_nullCategoryId_returnsDefaultIcon() {
        assertEquals(R.drawable.ic_tag, CategoryIconMapper.iconFor(null));
    }
}