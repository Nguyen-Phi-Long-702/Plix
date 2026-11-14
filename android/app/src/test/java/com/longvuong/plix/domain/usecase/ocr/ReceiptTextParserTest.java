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
}