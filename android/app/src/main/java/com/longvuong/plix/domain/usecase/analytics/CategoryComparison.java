package com.longvuong.plix.domain.usecase.analytics;

//Một dòng so sánh chi tiêu của một danh mục: tháng này so với cùng kỳ tháng trước (FIN-14)
public class CategoryComparison {
    public final String categoryId;
    public final long currentAmount;
    public final long previousAmount;
    public final long deltaAmount;
    public final Integer percent; //null khi previousAmount = 0 (giao diện hiện "mới")

    public CategoryComparison(String categoryId, long currentAmount, long previousAmount, long deltaAmount, Integer percent) {
        this.categoryId = categoryId;
        this.currentAmount = currentAmount;
        this.previousAmount = previousAmount;
        this.deltaAmount = deltaAmount;
        this.percent = percent;
    }
}