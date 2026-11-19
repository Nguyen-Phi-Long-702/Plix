package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

//Phát hiện khoản lặp hằng tháng từ lịch sử giao dịch (FIN-17). Java thuần, không phụ thuộc Android.
//Quy tắc theo DP_02 mục 7.3: lọc, gom nhóm, loại nhóm đã có mẫu, tháng đơn, neo, đi lùi theo tháng với dung sai, bỏ qua, sắp xếp
public class DetectRecurringCandidatesUseCase {
    private static final int MIN_CONSECUTIVE_MONTHS = 3;
    private static final int AMOUNT_TOLERANCE_PERCENT = 10;
    private static final int DAY_TOLERANCE_DAYS = 3;
    private static final int LOOKBACK_MONTHS = 12;

    //Thứ tự kết quả: số tháng liên tiếp giảm dần, rồi số tiền giảm dần, rồi key tăng dần
    private static final Comparator<RecurringCandidate> CANDIDATE_ORDER = (a, b) -> {
        if (a.consecutiveMonths != b.consecutiveMonths) {
            return Integer.compare(b.consecutiveMonths, a.consecutiveMonths);
        }
        if (a.amount != b.amount) {
            return Long.compare(b.amount, a.amount);
        }
        return a.getKey().compareTo(b.getKey());
    };

    @Inject
    public DetectRecurringCandidatesUseCase() {
    }

    public List<RecurringCandidate> execute(List<TransactionEntity> all, List<TransactionEntity> activeTemplates,
                                            Set<String> dismissedKeys, String userId, LocalDate today, ZoneId zone) {
        List<RecurringCandidate> candidates = new ArrayList<>();
        //đầu ngày kế tiếp theo múi giờ thiết bị; giao dịch từ mốc này trở đi là giao dịch tương lai
        long endOfTodayExclusive = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        Map<String, List<TransactionEntity>> groups = groupByKey(filterEligible(all, userId, endOfTodayExclusive));
        Set<String> templateKeys = collectTemplateKeys(activeTemplates, userId);
        YearMonth currentMonth = YearMonth.from(today);

        for (Map.Entry<String, List<TransactionEntity>> group : groups.entrySet()) {
            if (templateKeys.contains(group.getKey())) {
                continue; //nhóm đã có mẫu định kỳ đang hoạt động cùng loại và danh mục
            }
            Map<YearMonth, List<TransactionEntity>> byMonth = groupByMonth(group.getValue(), zone);
            TransactionEntity reference = findAnchor(byMonth, currentMonth);
            if (reference == null) {
                continue; //tháng gần nhất không phải tháng hiện tại/tháng trước hoặc không phải tháng đơn
            }
            int consecutiveMonths = countConsecutiveMonths(byMonth, reference, zone);
            if (consecutiveMonths < MIN_CONSECUTIVE_MONTHS || dismissedKeys.contains(group.getKey())) {
                continue; //chuỗi quá ngắn, hoặc người dùng đã bấm "Bỏ qua" cho loại và danh mục này
            }
            candidates.add(new RecurringCandidate(reference.type, reference.categoryId, reference.amount,
                    localDateOf(reference, zone).getDayOfMonth(), reference.note, consecutiveMonths,
                    reference.occurredAt));
        }
        candidates.sort(CANDIDATE_ORDER);
        return candidates;
    }

    //Giữ giao dịch của đúng người dùng, chưa xoá, không phải bản do Worker sinh, không phải mẫu định kỳ,
    //có danh mục, số tiền dương và không nằm ở tương lai
    private static List<TransactionEntity> filterEligible(List<TransactionEntity> all, String userId,
                                                          long endOfTodayExclusive) {
        List<TransactionEntity> eligible = new ArrayList<>();
        for (TransactionEntity transaction : all) {
            if (!Objects.equals(userId, transaction.userId)
                    || transaction.isDeleted
                    || transaction.recurrenceParentId != null
                    || transaction.isRecurring
                    || transaction.categoryId == null
                    || transaction.amount <= 0
                    || transaction.occurredAt >= endOfTodayExclusive) {
                continue;
            }
            eligible.add(transaction);
        }
        return eligible;
    }

    //Gom theo khoá type|categoryId (không gom theo ghi chú vì ghi chú thường thay đổi)
    private static Map<String, List<TransactionEntity>> groupByKey(List<TransactionEntity> eligible) {
        Map<String, List<TransactionEntity>> groups = new HashMap<>();
        for (TransactionEntity transaction : eligible) {
            String key = RecurringCandidate.buildKey(transaction.type, transaction.categoryId);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }
        return groups;
    }

    //Khoá type|categoryId của các mẫu định kỳ đang hoạt động của người dùng; mẫu không có danh mục không khớp nhóm nào nên bỏ qua
    private static Set<String> collectTemplateKeys(List<TransactionEntity> activeTemplates, String userId) {
        Set<String> keys = new HashSet<>();
        for (TransactionEntity template : activeTemplates) {
            if (Objects.equals(userId, template.userId)
                    && template.isRecurring
                    && template.recurrenceParentId == null
                    && !template.isDeleted
                    && template.categoryId != null) {
                keys.add(RecurringCandidate.buildKey(template.type, template.categoryId));
            }
        }
        return keys;
    }

    //Gom giao dịch của một nhóm theo tháng của ngày địa phương (theo zone)
    private static Map<YearMonth, List<TransactionEntity>> groupByMonth(List<TransactionEntity> transactions, ZoneId zone) {
        Map<YearMonth, List<TransactionEntity>> byMonth = new HashMap<>();
        for (TransactionEntity transaction : transactions) {
            YearMonth month = YearMonth.from(Instant.ofEpochMilli(transaction.occurredAt).atZone(zone));
            byMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(transaction);
        }
        return byMonth;
    }

    //Neo: tháng gần nhất của nhóm phải là tháng hiện tại hoặc tháng liền trước, và phải là tháng đơn (đúng 1 giao dịch).
    //Trả về giao dịch duy nhất của tháng đó (giao dịch tham chiếu), hoặc null khi không có neo
    private static TransactionEntity findAnchor(Map<YearMonth, List<TransactionEntity>> byMonth, YearMonth currentMonth) {
        YearMonth latest = Collections.max(byMonth.keySet());
        if (!latest.equals(currentMonth) && !latest.equals(currentMonth.minusMonths(1))) {
            return null;
        }
        List<TransactionEntity> inLatest = byMonth.get(latest);
        return inLatest.size() == 1 ? inLatest.get(0) : null;
    }

    //Đi lùi từng tháng liền trước tháng của giao dịch tham chiếu. Mỗi tháng phải là tháng đơn, số tiền lệch tối đa
    //AMOUNT_TOLERANCE_PERCENT% so với tham chiếu và ngày lệch tối đa DAY_TOLERANCE_DAYS so với min(ngày tham chiếu, số ngày của tháng).
    //Dừng ở tháng đầu tiên không khớp hoặc khi đã xét đủ LOOKBACK_MONTHS tháng (kể cả tháng neo). Trả về độ dài chuỗi, tính cả tháng neo
    private static int countConsecutiveMonths(Map<YearMonth, List<TransactionEntity>> byMonth,
                                              TransactionEntity reference, ZoneId zone) {
        LocalDate referenceDate = localDateOf(reference, zone);
        YearMonth anchorMonth = YearMonth.from(referenceDate);
        int months = 1; //tháng neo
        while (months < LOOKBACK_MONTHS) {
            YearMonth month = anchorMonth.minusMonths(months);
            List<TransactionEntity> inMonth = byMonth.get(month);
            if (inMonth == null || inMonth.size() != 1) {
                break; //thiếu tháng hoặc tháng không đơn
            }
            TransactionEntity transaction = inMonth.get(0);
            int expectedDay = Math.min(referenceDate.getDayOfMonth(), month.lengthOfMonth());
            boolean amountMatches = Math.abs(transaction.amount - reference.amount) * 100
                    <= reference.amount * AMOUNT_TOLERANCE_PERCENT;
            boolean dayMatches = Math.abs(localDateOf(transaction, zone).getDayOfMonth() - expectedDay)
                    <= DAY_TOLERANCE_DAYS;
            if (!amountMatches || !dayMatches) {
                break;
            }
            months++;
        }
        return months;
    }

    //Ngày địa phương của giao dịch theo múi giờ zone
    private static LocalDate localDateOf(TransactionEntity transaction, ZoneId zone) {
        return Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate();
    }
}