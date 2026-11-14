package com.longvuong.plix.domain.usecase.ocr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.longvuong.plix.domain.usecase.ocr.ReceiptParseResult.TotalSource;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ReceiptTextParserTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    //4 hoá đơn cố định, đúng như DP_02 mục 2.7
    private static final List<String> HD1 = Arrays.asList(
            "HIGHLANDS COFFEE",
            "DC: 12 Le Loi, Q1",
            "08/11/2026 09:15",
            "Ca phe sua da 45.000",
            "Banh mi 30.000",
            "Tam tinh 75.000",
            "Phi dich vu 60.000",
            "Tong cong 135.000",
            "Thanh toan 135.000");

    private static final List<String> HD2 = Arrays.asList(
            "CIRCLE K",
            "07/11/2026 18:40",
            "Sua chua 18.000",
            "Tong tien 18.000",
            "Tien khach dua 20.000",
            "Tien thua 2.000");

    private static final List<String> HD3 = Arrays.asList(
            "BACH HOA XANH",
            "08.11.2026",
            "Sua tuoi 62.000",
            "Rau cai 85.000",
            "TONG CONG",
            "147.000");

    private static final List<String> HD4 = Arrays.asList(
            "QUAN COM TAM",
            "Com suon 45.000",
            "Tra da 5.000",
            "Cam on quy khach");

    private final ReceiptTextParser parser = new ReceiptTextParser();

    private ReceiptParseResult parse(String... lines) {
        return parser.parse(Arrays.asList(lines), TODAY);
    }

    /**
     * TN2-U01
     * HD1 (có THANH TOÁN và TỔNG CỘNG): lấy dòng 'Thanh toan' (P1 cao hơn P2) = 135000, ngày 2026-11-08, tên 'HIGHLANDS COFFEE'.
     */
    @Test
    public void parse_hd1WithPaymentAndTotalLines_prefersPaymentLine() {
        ReceiptParseResult result = parser.parse(HD1, TODAY);

        assertEquals(Long.valueOf(135_000L), result.totalAmount);
        assertEquals(TotalSource.KEYWORD, result.totalSource);
        assertEquals(LocalDate.of(2026, 11, 8), result.date);
        assertEquals("HIGHLANDS COFFEE", result.storeName);
    }

    /**
     * TN2-U02
     * HD2 (Tong tien + tiền khách đưa + tiền thừa): total = 18000, ngày 2026-11-07, tên 'CIRCLE K'; hai dòng tiền khách đưa và tiền thừa không được chọn.
     */
    @Test
    public void parse_hd2WithChangeLines_ignoresCashGivenAndChange() {
        ReceiptParseResult result = parser.parse(HD2, TODAY);

        assertEquals(Long.valueOf(18_000L), result.totalAmount);
        assertEquals(TotalSource.KEYWORD, result.totalSource);
        assertEquals(LocalDate.of(2026, 11, 7), result.date);
        assertEquals("CIRCLE K", result.storeName);
    }

    /**
     * TN2-U03
     * HD3 (nhãn 'TONG CONG' và số tiền nằm ở 2 dòng): lấy dòng kế tiếp = 147000, ngày 2026-11-08 từ '08.11.2026', tên 'BACH HOA XANH'.
     */
    @Test
    public void parse_hd3WithLabelAndAmountOnSeparateLines_takesNextLine() {
        ReceiptParseResult result = parser.parse(HD3, TODAY);

        assertEquals(Long.valueOf(147_000L), result.totalAmount);
        assertEquals(TotalSource.KEYWORD, result.totalSource);
        assertEquals(LocalDate.of(2026, 11, 8), result.date);
        assertEquals("BACH HOA XANH", result.storeName);
    }

    /**
     * TN2-U04
     * HD4 (không có từ khoá tổng): dự phòng lấy số lớn nhất = 45000, totalSource = FALLBACK_MAX, không có ngày, tên 'QUAN COM TAM'.
     */
    @Test
    public void parse_hd4WithoutTotalKeyword_fallsBackToLargestAmount() {
        ReceiptParseResult result = parser.parse(HD4, TODAY);

        assertEquals(Long.valueOf(45_000L), result.totalAmount);
        assertEquals(TotalSource.FALLBACK_MAX, result.totalSource);
        assertNull(result.date);
        assertEquals("QUAN COM TAM", result.storeName);
    }

    /**
     * TN2-U05
     * ['CUA HANG ABC', 'Cam on quy khach'] không có số tiền: total = null (NONE), date = null, tên 'CUA HANG ABC'.
     */
    @Test
    public void parse_withoutAnyAmount_returnsNullTotalAndNoneSource() {
        ReceiptParseResult result = parse("CUA HANG ABC", "Cam on quy khach");

        assertNull(result.totalAmount);
        assertEquals(TotalSource.NONE, result.totalSource);
        assertNull(result.date);
        assertEquals("CUA HANG ABC", result.storeName);
    }

    /**
     * TN2-U06
     * ['Tam tinh 100.000', 'Giam gia 10.000', 'Tong cong 90.000']: tạm tính và giảm giá không phải tổng, total = 90000.
     */
    @Test
    public void parse_withSubtotalAndDiscount_returnsTotalLine() {
        ReceiptParseResult result = parse("Tam tinh 100.000", "Giam gia 10.000", "Tong cong 90.000");

        assertEquals(Long.valueOf(90_000L), result.totalAmount);
    }

    /**
     * TN2-U07
     * ['Tong tien giam gia 10.000', 'Thanh toan 90.000']: dòng 'Tong tien giam gia' bị loại dù có 'tong tien', total = 90000.
     */
    @Test
    public void parse_totalLineContainingDiscountWord_isExcluded() {
        ReceiptParseResult result = parse("Tong tien giam gia 10.000", "Thanh toan 90.000");

        assertEquals(Long.valueOf(90_000L), result.totalAmount);
        //nếu chỉ có dòng giảm giá thì dòng đó không được nhận làm tổng, cũng không lọt vào bước dự phòng
        assertNull(parse("Tong tien giam gia 10.000").totalAmount);
    }

    /**
     * TN2-U08
     * ['Tổng cộng 125.000'] (có dấu) và ['Tong cong 125.000'] (không dấu) đều cho total = 125000.
     */
    @Test
    public void parse_accentedAndUnaccentedKeyword_giveSameTotal() {
        ReceiptParseResult accented = parse("Tổng cộng 125.000");
        ReceiptParseResult unaccented = parse("Tong cong 125.000");

        assertEquals(Long.valueOf(125_000L), accented.totalAmount);
        assertEquals(TotalSource.KEYWORD, accented.totalSource); //khớp từ khoá, không phải dự phòng số lớn nhất
        assertEquals(Long.valueOf(125_000L), unaccented.totalAmount);
        assertEquals(TotalSource.KEYWORD, unaccented.totalSource);
    }

    /**
     * TN2-U09
     * ['TONG CÔNG 125.000'] (Ô thay cho Ộ do OCR nhận sai dấu): chuẩn hoá bỏ dấu ra 'tong cong' nên total = 125000.
     */
    @Test
    public void parse_misrecognizedAccent_stillMatchesKeyword() {
        ReceiptParseResult result = parse("TONG CÔNG 125.000");

        assertEquals(Long.valueOf(125_000L), result.totalAmount);
        assertEquals(TotalSource.KEYWORD, result.totalSource); //khớp từ khoá, không phải dự phòng số lớn nhất
    }

    /**
     * TN2-U10
     * Các định dạng số tiền (mỗi chuỗi là một dòng hoá đơn): '45,000', '45.000', '45.000đ', '45.000 VND', '1.250.000', '45000đ'
     * cho 45000, 45000, 45000, 45000, 1250000, 45000.
     */
    @Test
    public void parse_variousAmountFormats_areReadCorrectly() {
        assertEquals(Long.valueOf(45_000L), parse("45,000").totalAmount);
        assertEquals(Long.valueOf(45_000L), parse("45.000").totalAmount);
        assertEquals(Long.valueOf(45_000L), parse("45.000đ").totalAmount);
        assertEquals(Long.valueOf(45_000L), parse("45.000 VND").totalAmount);
        assertEquals(Long.valueOf(1_250_000L), parse("1.250.000").totalAmount);
        assertEquals(Long.valueOf(45_000L), parse("45000đ").totalAmount);
    }

    /**
     * TN2-U11
     * ['Tong cong 1.000.000.000.000'] lớn hơn FormValidator.MAX_AMOUNT (999.999.999.999): total = null.
     */
    @Test
    public void parse_amountAboveMaxAmount_returnsNullTotal() {
        ReceiptParseResult result = parse("Tong cong 1.000.000.000.000");

        assertNull(result.totalAmount);
        assertEquals(TotalSource.NONE, result.totalSource);
    }
}