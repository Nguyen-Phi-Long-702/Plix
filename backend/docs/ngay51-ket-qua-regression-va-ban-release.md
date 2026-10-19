# Kết quả regression và kiểm thử bản release (R8) — Ngày 51

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Ngày chạy:** 20/10/2026
**Thiết bị:** Medium Phone, Android 16.0 (emulator)

## 1. Regression các Sprint trước

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Unit test Android | `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | 288 test, 0 lỗi — ĐẠT |
| Test instrumented Android | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | 25 test, 0 lỗi — ĐẠT (lần chạy lại; lần chạy đầu xem mục 4) |
| Backend (do Vương chạy) | `python -m pytest -q` (trong `backend`) | 379 test, 0 lỗi — ĐẠT |

Năm nhóm Danh mục, Thống kê, Giao dịch định kỳ, Ngân sách, Mục tiêu tiết kiệm đều nằm trong lần chạy trên: ĐẠT (tổng số test chạy khớp số `@Test` đếm được trong mã nguồn: 288 và 25).

## 2. Bật R8 cho bản release

- Cấu hình: `isMinifyEnabled = true` cho `release`, dùng `proguard-android-optimize.txt` và `proguard-rules.pro` (quy tắc Vương chuẩn bị Ngày 11).
- `assembleRelease`: BUILD SUCCESSFUL; có file `mapping.txt` (R8 đã chạy).
- Quy tắc phải bổ sung thêm: không có

## 3. Luồng cơ bản trên bản release (đã thu gọn mã)

| Bước | Nội dung | Kết quả |
|---|---|---|
| C1 | Đăng nhập | <ĐẠT/KHÔNG ĐẠT> |
| C2 | Thêm giao dịch (45.000, "cà phê test R8") | <ĐẠT/KHÔNG ĐẠT> |
| C3 | Đồng bộ (icon hết quay, dòng có trên bảng `transactions`) | <ĐẠT/KHÔNG ĐẠT> |
| C4 | Gợi ý phân loại ("cà phê") | <ĐẠT/KHÔNG ĐẠT> |
| C5 | Mở màn Thống kê | <ĐẠT/KHÔNG ĐẠT> |

## 4. Lỗi phát hiện và cách sửa

Lần chạy `connectedDebugAndroidTest` đầu tiên (chạy ngay sau unit test, emulator đang chịu tải) có 1/25 test lỗi: `TransactionListPerformanceTest.transactionList_5000Rows_sortedListUnder100ms`, trung vị 179.5 ms so với mục tiêu < 100 ms. Test lọc theo danh mục + khoảng ngày (cũng gọi `getAll()` trên 5.000 dòng) vẫn đạt trong cùng lượt chạy. Không sửa code, không đổi ngưỡng. Chạy lại bộ 25 test khi máy yên: 25/25 đạt.

<Các lỗi khác ở luồng release (nếu có): Không có>

Lưu ý: bản release ký tạm bằng khoá debug chỉ để chạy thử; cấu hình ký chính thức làm ở Ngày 57.