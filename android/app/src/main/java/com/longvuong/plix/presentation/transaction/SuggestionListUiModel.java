package com.longvuong.plix.presentation.transaction;

import java.util.List;

//Toàn bộ phần "Kết quả nhận diện": danh sách dòng + tiêu đề + trạng thái nút lưu (đã tính sẵn trong ViewModel)
public class SuggestionListUiModel {
    public final List<SuggestionItemUiModel> items;
    public final String titleText; //vd "Kết quả nhận diện (2)"
    public final String saveButtonText; //vd "Lưu 2 giao dịch"
    public final boolean canSave; //true khi có ít nhất 1 dòng được tick

    public SuggestionListUiModel(List<SuggestionItemUiModel> items, String titleText, String saveButtonText, boolean canSave) {
        this.items = items;
        this.titleText = titleText;
        this.saveButtonText = saveButtonText;
        this.canSave = canSave;
    }
}