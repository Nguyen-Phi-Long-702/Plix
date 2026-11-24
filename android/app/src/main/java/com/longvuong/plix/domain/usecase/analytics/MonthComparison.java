package com.longvuong.plix.domain.usecase.analytics;

import java.time.LocalDate;
import java.util.List;

//Kết quả so sánh chi tiêu: tháng này (từ ngày 1 đến hôm nay) so với cùng kỳ tháng trước (FIN-14)
public class MonthComparison {
    public final long currentTotal;
    public final long previousTotal;
    public final long deltaTotal;
    public final Integer percentTotal; //null khi previousTotal = 0
    public final List<CategoryComparison> categories; //sắp xếp theo |chênh lệch| giảm dần
    public final LocalDate currentFrom;
    public final LocalDate currentTo;
    public final LocalDate previousFrom;
    public final LocalDate previousTo;

    public MonthComparison(long currentTotal, long previousTotal, long deltaTotal, Integer percentTotal,
                           List<CategoryComparison> categories, LocalDate currentFrom, LocalDate currentTo,
                           LocalDate previousFrom, LocalDate previousTo) {
        this.currentTotal = currentTotal;
        this.previousTotal = previousTotal;
        this.deltaTotal = deltaTotal;
        this.percentTotal = percentTotal;
        this.categories = categories;
        this.currentFrom = currentFrom;
        this.currentTo = currentTo;
        this.previousFrom = previousFrom;
        this.previousTo = previousTo;
    }
}