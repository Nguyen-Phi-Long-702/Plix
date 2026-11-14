package com.longvuong.plix.domain.usecase.ocr;

import com.longvuong.plix.domain.usecase.ocr.ReceiptParseResult.TotalSource;
import com.longvuong.plix.domain.validation.FormValidator;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

//Trích tổng tiền, ngày và tên cửa hàng từ các dòng chữ OCR của hoá đơn (AI-13). Java thuần, không phụ thuộc Android
public class ReceiptTextParser {

    //Mọi từ khoá bên dưới đã ở dạng chuẩn hoá: không dấu, chữ thường
    private static final String[] TOTAL_KEYWORDS_P1 = {
            "thanh toan", "tong thanh toan", "phai tra", "grand total", "amount due"};
    private static final String[] TOTAL_KEYWORDS_P2 = {
            "tong cong", "tong tien", "tong so tien", "total"};
    private static final String[] EXCLUDED_KEYWORDS = {
            "giam gia", "chiet khau", "khuyen mai", "tien thua", "khach dua", "tam tinh", "subtotal",
            "sub total", "change", "diem", "chua vat", "chua thue", "truoc thue"};

    private static final int MIN_BARE_DIGITS = 4;
    private static final int MAX_BARE_DIGITS = 12;
    private static final int MAX_AMOUNT_DIGITS = 15; //dài hơn thì chắc chắn vượt MAX_AMOUNT, bỏ qua để không tràn kiểu long

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    //dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy, dd/MM/yy; chạy trên dòng đã chuẩn hoá
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4}|\\d{2})(?!\\d)");

    //nhóm 1: số (có nhóm nghìn hoặc chữ số trần); nhóm 2: hậu tố tiền tệ vnd hoặc d (chữ đ sau chuẩn hoá)
    private static final Pattern MONEY_TOKEN = Pattern.compile(
            "(?<![\\d.,])(\\d{1,3}(?:[.,]\\d{3})+|\\d+)(?![.,]?\\d)(?:\\s*(vnd|d)(?![a-z0-9]))?");

    @Inject
    public ReceiptTextParser() {
    }

    public ReceiptParseResult parse(List<String> lines, LocalDate today) {
        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            normalized.add(normalize(line));
        }

        Long total = findKeywordTotal(normalized);
        TotalSource source = TotalSource.KEYWORD;
        if (total == null) {
            total = findFallbackMax(normalized);
            source = total != null ? TotalSource.FALLBACK_MAX : TotalSource.NONE;
        }
        return new ReceiptParseResult(total, source, null, null); //ngày và tên cửa hàng làm ở các bước sau
    }

    //NFD, bỏ dấu, đ thành d, chữ thường, gộp khoảng trắng. Ví dụ 'TỔNG CỘNG' và 'TONG CÔNG' đều thành 'tong cong'
    private static String normalize(String line) {
        String decomposed = Normalizer.normalize(line, Normalizer.Form.NFD);
        String withoutMarks = MARKS.matcher(decomposed).replaceAll("");
        String withoutD = withoutMarks.replace('đ', 'd').replace('Đ', 'D'); //đ không tách dấu được bằng NFD
        return WHITESPACE.matcher(withoutD.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    private static boolean containsAny(String line, String[] keywords) {
        for (String keyword : keywords) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTotalKeyword(String line) {
        return containsAny(line, TOTAL_KEYWORDS_P1) || containsAny(line, TOTAL_KEYWORDS_P2);
    }

    //Tổng tiền theo từ khoá: lấy mức ưu tiên cao nhất (P1 hơn P2), trong mức đó lấy dòng cuối cùng
    private Long findKeywordTotal(List<String> normalized) {
        int bestLine = -1;
        int bestPriority = 0;
        for (int i = 0; i < normalized.size(); i++) {
            String line = normalized.get(i);
            if (containsAny(line, EXCLUDED_KEYWORDS)) {
                continue;
            }
            int priority = containsAny(line, TOTAL_KEYWORDS_P1) ? 1 : (containsAny(line, TOTAL_KEYWORDS_P2) ? 2 : 0);
            if (priority == 0) {
                continue;
            }
            if (bestLine < 0 || priority <= bestPriority) { //duyệt từ trên xuống nên dấu <= chọn dòng sau khi cùng mức
                bestLine = i;
                bestPriority = priority;
            }
        }
        if (bestLine < 0) {
            return null;
        }

        List<Long> sameLine = extractAmounts(normalized.get(bestLine), true);
        if (!sameLine.isEmpty()) {
            return sameLine.get(sameLine.size() - 1); //token tiền cuối cùng trên dòng
        }
        if (bestLine + 1 < normalized.size()) {
            return amountOfWholeLine(normalized.get(bestLine + 1)); //nhãn và số nằm ở 2 dòng: chỉ xét đúng một dòng kế tiếp
        }
        return null;
    }

    //Dự phòng: số tiền lớn nhất trong các token tiền hợp lệ của các dòng không bị loại
    private Long findFallbackMax(List<String> normalized) {
        Long max = null;
        for (String line : normalized) {
            if (containsAny(line, EXCLUDED_KEYWORDS)) {
                continue;
            }
            for (Long amount : extractAmounts(line, hasTotalKeyword(line))) {
                if (max == null || amount > max) {
                    max = amount;
                }
            }
        }
        return max;
    }

    //Các số tiền hợp lệ trên một dòng theo thứ tự xuất hiện; chữ số trần chỉ nhận khi allowBare (dòng có từ khoá tổng)
    private List<Long> extractAmounts(String line, boolean allowBare) {
        List<Long> amounts = new ArrayList<>();
        Matcher matcher = MONEY_TOKEN.matcher(stripDates(line));
        while (matcher.find()) {
            Long amount = toAmount(matcher, allowBare);
            if (amount != null) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    //Dòng kế tiếp chỉ được nhận khi cả dòng là một token tiền (không nhận chữ số trần)
    private Long amountOfWholeLine(String line) {
        Matcher matcher = MONEY_TOKEN.matcher(stripDates(line).trim());
        if (!matcher.matches()) {
            return null;
        }
        return toAmount(matcher, false);
    }

    //Bỏ ngày khỏi dòng trước khi tìm số tiền để '2026' hay '11.202' trong ngày không bị hiểu là tiền
    private static String stripDates(String line) {
        return DATE_PATTERN.matcher(line).replaceAll(" ");
    }

    private Long toAmount(Matcher matcher, boolean allowBare) {
        String number = matcher.group(1);
        boolean grouped = number.indexOf('.') >= 0 || number.indexOf(',') >= 0;
        boolean hasSuffix = matcher.group(2) != null;
        String digits = number.replace(".", "").replace(",", "");
        boolean bareAccepted = allowBare && digits.length() >= MIN_BARE_DIGITS && digits.length() <= MAX_BARE_DIGITS;
        if (!grouped && !hasSuffix && !bareAccepted) {
            return null;
        }
        if (digits.length() > MAX_AMOUNT_DIGITS) {
            return null;
        }
        long amount = Long.parseLong(digits);
        if (amount < 1 || amount > FormValidator.MAX_AMOUNT) {
            return null; //ngoài khoảng của FormValidator thì coi như không có
        }
        return amount;
    }
}