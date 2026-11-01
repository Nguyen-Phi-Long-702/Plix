# Kết quả hồi quy và kiểm thử bản release (R8) — Ngày UI-7 (Sprint UI)

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Ngày chạy:** 31/10/2026
**Thiết bị:** Pixel 5 (AVD), Android 11 (emulator)

## 1. Dọn tài nguyên không còn dùng

Tìm toàn thư mục `android` (java, res, manifest, test, androidTest, proguard) bằng `git grep`. Đã xoá 13 icon `drawable` không còn nơi tham chiếu: `ic_add`, `ic_ai_forecast`, `ic_ai_quick_add`, `ic_ai_retrain`, `ic_back`, `ic_close`, `ic_delete`, `ic_filter`, `ic_lock`, `ic_tab_analytics`, `ic_tab_budget_goal`, `ic_tab_home`, `ic_tab_settings`. Không có file `bg_*` nào cần xoá. `assembleDebug`: BUILD SUCCESSFUL.

## 2. Regression

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Unit test Android | `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | 409 test, 0 lỗi — ĐẠT |
| Test instrumented Android | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | 29 test, 0 lỗi — ĐẠT |
| Backend (do Long chạy) | `python -m pytest -q` (trong `backend`) | 379 test, 0 lỗi — ĐẠT |

Tổng số test chạy khớp số `@Test` đếm được trong mã nguồn (409 và 29). Số test khác Ngày 51 (288 và 25) vì Sprint UI đã xoá 2 file test mẫu và thêm test mới.

## 3. Bản release (R8)

- `assembleRelease`: BUILD SUCCESSFUL; có `mapping.txt` (R8 đã chạy).
- Ký tạm bằng khoá debug chỉ để chạy thử; không đưa vào mã nguồn (cấu hình ký chính thức làm ở Ngày 57).
- Quy tắc R8 phải bổ sung thêm: Không có

| Bước | Nội dung | Kết quả |
|---|---|---|
| R1 | Đăng nhập | ĐẠT |
| R2 | Thêm giao dịch (45.000, "cà phê test R8") | ĐẠT |
| R3 | Đồng bộ (biểu tượng dừng quay, dòng có trên bảng `transactions`) | ĐẠT |
| R4 | Gợi ý phân loại ("cà phê") | ĐẠT |
| R5 | Mở màn Thống kê (4 chip khoảng thời gian) | ĐẠT |
| R6 | Thêm giao dịch định kỳ (ngày 31 hằng tháng) | ĐẠT |

## 4. Lỗi phát hiện và cách sửa

Không phát hiện lỗi.

## 5. Lỗi ghi nhận, chưa sửa

Không có.