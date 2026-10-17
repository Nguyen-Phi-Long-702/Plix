package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.data.repository.AnomalyResult;

public class AnomalyCheckOutcome {
    public final AnomalyResult result;
    public final boolean hasOtherPendingInCategory; //còn giao dịch khác cùng danh mục chưa đồng bộ lên máy chủ

    public AnomalyCheckOutcome(AnomalyResult result, boolean hasOtherPendingInCategory) {
        this.result = result;
        this.hasOtherPendingInCategory = hasOtherPendingInCategory;
    }
}