package com.longvuong.plix.presentation.transaction;

import androidx.annotation.Nullable;

//1 dòng giao dịch đề xuất từ câu nhập nhanh, đã được ViewModel format sẵn để Fragment/Adapter chỉ việc hiển thị
public class SuggestionItemUiModel {
    public final String itemId; //id cục bộ của dòng, dùng để ViewModel tìm lại dòng khi người dùng tick/sửa
    public final long amount; //VNĐ nguyên, dùng để điền sẵn vào hộp thoại sửa
    @Nullable
    public final String categoryId; //null = chưa xác định danh mục (cần người dùng chọn tay)
    public final String title; //tên danh mục, hoặc "Chưa chọn danh mục"
    public final String detail; //vd "Độ tin cậy: 90%", "Đã chỉnh sửa", hoặc lời nhắc cần chọn danh mục
    public final String amountText; //vd "-50.000đ" / "+12.000.000đ"
    public final boolean income; //true = thu nhập (màu xanh, dấu +), false = chi tiêu (màu đỏ, dấu -)
    public final boolean selected; //đã được tick để lưu
    public final boolean needsCategory; //true = chưa có danh mục: tô nền cảnh báo, khoá ô tick cho tới khi người dùng chọn danh mục
    public final boolean needsAmount; //true = câu không có số tiền hợp lệ (amount = 0): hiện nút "Nhập tay", khoá ô tick cho tới khi người dùng nhập số tiền

    public SuggestionItemUiModel(String itemId, long amount, @Nullable String categoryId, String title, String detail,
                                 String amountText, boolean income, boolean selected, boolean needsCategory, boolean needsAmount) {
        this.itemId = itemId;
        this.amount = amount;
        this.categoryId = categoryId;
        this.title = title;
        this.detail = detail;
        this.amountText = amountText;
        this.income = income;
        this.selected = selected;
        this.needsCategory = needsCategory;
        this.needsAmount = needsAmount;
    }
}