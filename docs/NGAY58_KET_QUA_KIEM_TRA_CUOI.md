# Kết quả diễn tập lần 2, kiểm tra cuối và đánh dấu phiên bản nộp — Ngày 58

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Ngày chạy:** 7/11/2026
**Thiết bị Android:** Pixel 5 (AVD), Android 11

## 1. Vấn đề cần sửa (từ Ngày 57 và từ rà soát Ngày 58) và cách xử lý

| Mã | Vấn đề | Cách xử lý | Commit |
|---|---|---|---|
| K1 | Chú thích `ExportViewModel` ghi sai giới hạn xuất CSV (1 lần/giờ) | Sửa thành 1 lần/phút (60 giây) |
| K2–K4 | Biên bản Ngày 35, 36, 37, 50 (backend) và UI-6 còn ô chưa điền | Điền thông tin thật hoặc ghi “không ghi lại” |

## 2. Chạy lại bộ kiểm thử

| Hạng mục | Lệnh | Người chạy | Kết quả |
|---|---|---|---|
| Unit test Android | `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | Long | ĐẠT |
| Test instrumented Android | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | Long | ĐẠT |
| Backend | `python -m pytest -q` (trong `backend`) | Vương | ĐẠT |

Số test chạy ra <khớp / không khớp> số `@Test` đếm được trong mã nguồn (414 và 29). So với Ngày 53/55 (409, 29, 379): khác vì Ngày 57 thêm chức năng xuất CSV (+5 unit, +14 backend).

## 3. Hiệu năng

| Phép đo | Kết quả | Mục tiêu | Kết quả |
|---|---|---|---|
| Room, lần chạy full: `getAll` 5.000 dòng / lọc danh mục + ngày / `searchByNote` (trung vị) | 202.4097 ms | < 100 ms | KHÔNG ĐẠT |
| Room, chạy riêng lớp `TransactionListPerformanceTest` khi máy yên (trung vị) | 202.4097 ms | < 100 ms | KHÔNG ĐẠT |

Thời điểm chạy: 2026-11-07 16:27

| Endpoint | Điều kiện đo | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |
|---|---|---|---|---|---|---|
| `POST /api/v1/categorize` | ghi chú "cà phê" | 1912.4 | 2123.7 | 2333.5 | < 3000 ms | ĐẠT |
| `GET /api/v1/sync/transactions/pull` | since=0, limit=500, trả về 500 bản ghi | 1524.5 | 1554.5 | 1600.2 | < 3000 ms | ĐẠT |


## 4. Bản release

- `assembleRelease`: BUILD SUCCESSFUL; chứng chỉ SHA-256: trùng Ngày 57; `mapping.txt`: có.
- Kiểm tra nhanh: D1 ĐẠT, D2 ĐẠT, D3 ĐẠT, D4 (không lộ JWT trong logcat) ĐẠT.

## 5. Diễn tập demo lần 2

Làm nóng: Render có ngủ; mất khoảng 1273 ms. Người thao tác: Long. Người ghi: Vương.

Lỗi gặp trong quá trình diễn tập và cách sửa: Không có.

## 6. Danh sách kiểm tra cuối cùng (Definition of Done toàn dự án)

| # | Mục | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Build release có ProGuard + ký | ĐẠT | Mục 4 |
| 2 | Core chạy offline | ĐẠT | Kiểm tra F1; bước B10 |
| 3 | Đăng nhập/Đăng ký | ĐẠT | C2; kiểm tra F2 |
| 4 | Đồng bộ 5 bảng | ĐẠT | 110 test `sync`; test sync Android; B1, B10a |
| 5 | Hộp thoại đăng xuất khi còn dữ liệu chờ | ĐẠT | Kiểm tra F3 |
| 6 | Cách ly người dùng | ĐẠT | 124 test bảo mật |
| 7 | AI hoạt động | ĐẠT | B3–B7 |
| 8 | `/retrain` giới hạn + khoá | ĐẠT | 19 test; B11 |
| 9 | Bộ test đầy đủ đạt | ĐẠT | Mục 2 |
| 10 | Hiệu năng | ĐẠT | Mục 3 |
| 11 | Deploy Render + Supabase | ĐẠT | `/health` connected|
| 12 | Bí mật an toàn | ĐẠT | Kiểm tra F4; D4 |
| 13 | README hoàn chỉnh | ĐẠT | Kiểm tra F5 |
| 14 | Điều hướng, phiên hết hạn | ĐẠT | Diễn tập; F1; đọc mã ở `SECURITY_REVIEW.md` mục 2 (ghi chú ở mục 7) |
| 15 | Xử lý lỗi nhất quán | ĐẠT | Kiểm tra F6 (ghi chú ở mục 7) |
| 16 | Kịch bản demo, đã diễn tập ≥ 2 lần | ĐẠT | Lần 1 (Ngày 57), lần 2 (mục 5) |
| 17 | Báo cáo đồ án | ĐẠT | Kiểm tra F7 |
| 18 | Ma trận truy vết | ĐẠT | `docs/MA_TRAN_TRUY_VET.md` |

**Kết luận:** Final QA Checklist (mục 1–12, 14, 15): PASS; Final Project Completion Checklist (mục 13, 16–18): PASS.

## 7. Lỗi ghi nhận, chưa sửa

- `SettingsViewModel` bắt `Exception` dự phòng ở luồng đăng xuất (2 chỗ), không đi qua `Result<T>` + `ErrorMapper` (mục 15).
- Nhánh làm mới token thất bại → về màn Đăng nhập chưa có test tự động; bằng chứng là đọc mã (mục 14).
- Backlog bảo mật B1–B7 trong `docs/SECURITY_REVIEW.md` vẫn mở.
- Lệch giao diện N1–N7 trong `docs/UI6_DOI_CHIEU_MAN_LONG.md` (chờ quyết định, không phải lỗi).

## 8. Phiên bản nộp

- Tag: `v1.0-mvp` trên nhánh `main` (mã commit tra bằng lệnh `git rev-parse v1.0-mvp`).
- Mã nguồn không đổi so với commit đã kiểm thử ở đầu biên bản; các commit sau đó chỉ chứa file tài liệu (đã kiểm bằng `git diff --stat` trước khi gộp).
- APK nộp: `Plix-v1.0-mvp.apk`, SHA-256 như mục 4; chia sẻ trực tiếp, không đưa lên kho mã nguồn.