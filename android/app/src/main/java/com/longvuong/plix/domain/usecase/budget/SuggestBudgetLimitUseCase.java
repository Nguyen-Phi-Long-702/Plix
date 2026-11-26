package com.longvuong.plix.domain.usecase.budget;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

//Gợi ý hạn mức ngân sách bằng trung bình chi tiêu của tối đa 3 tháng lịch liền trước kỳ ngân sách (FIN-16). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 5.4: chỉ tính tháng có dữ liệu chi, làm tròn nửa lên đến 1.000, không có tháng nào thì null. Đây là thống kê, không phải AI
public class SuggestBudgetLimitUseCase {
    private static final int MONTHS_BACK = 3; //số tháng lịch liền trước kỳ ngân sách được xét
    private static final long ROUNDING_STEP = 1_000L; //hạn mức người dùng đặt thường tròn nghìn

    @Inject
    public SuggestBudgetLimitUseCase() {
    }

    //categoryIdOrNull = null: ngân sách tổng (tính mọi giao dịch chi); khác null: chỉ tính giao dịch chi của danh mục đó. Trả về null khi không có dữ liệu
    public BudgetLimitSuggestion execute(List<TransactionEntity> all, String userId, String categoryIdOrNull,
                                         YearMonth target, ZoneId zone) {
        YearMonth earliest = target.minusMonths(MONTHS_BACK);
        YearMonth latest = target.minusMonths(1);

        //Tổng chi theo từng tháng; tháng nào không có giao dịch chi thì không có khoá trong map (không tính vào trung bình)
        Map<YearMonth, Long> expenseByMonth = new HashMap<>();
        if (all != null) {
            for (TransactionEntity transaction : all) {
                if (!"expense".equals(transaction.type)
                        || transaction.isDeleted
                        || !Objects.equals(userId, transaction.userId)) {
                    continue; //chỉ lấy giao dịch chi, chưa xoá, của đúng người dùng
                }
                if (categoryIdOrNull != null && !categoryIdOrNull.equals(transaction.categoryId)) {
                    continue; //ngân sách danh mục: chỉ lấy giao dịch của danh mục đó
                }
                YearMonth month = YearMonth.from(Instant.ofEpochMilli(transaction.occurredAt).atZone(zone));
                if (month.isBefore(earliest) || month.isAfter(latest)) {
                    continue; //ngoài 3 tháng liền trước kỳ ngân sách
                }
                expenseByMonth.merge(month, transaction.amount, Long::sum);
            }
        }
        if (expenseByMonth.isEmpty()) {
            return null;
        }

        long total = 0L;
        for (long monthTotal : expenseByMonth.values()) {
            total += monthTotal;
        }
        int monthsUsed = expenseByMonth.size();
        long average = total / monthsUsed;
        //Làm tròn nửa lên đến 1.000 gần nhất: 1000 * floorDiv(trung bình + 500, 1000)
        long suggestedAmount = ROUNDING_STEP * Math.floorDiv(average + ROUNDING_STEP / 2L, ROUNDING_STEP);
        return new BudgetLimitSuggestion(suggestedAmount, monthsUsed);
    }
}