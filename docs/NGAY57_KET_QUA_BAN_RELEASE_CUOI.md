# Kết quả build và kiểm thử bản release cuối — Ngày 57

**Người thực hiện:** Nguyễn Phi Long
**Ngày chạy:** 04/11/2026
**Nhánh / commit đã build:** `sprint7-long` / `0c568a8`
**Thiết bị:** Pixel 5, Android 11 (emulator)

## 1. Cấu hình ký

- Bản release ký bằng khoá riêng của nhóm; đường dẫn khoá và mật khẩu đọc từ `android/keystore.properties`. File này và `*.jks`, `*.keystore` nằm trong `android/.gitignore`, không có trong kho mã nguồn.
- `apksigner verify`: `Verifies` (`Verified using v2 scheme (APK Signature Scheme v2): true`); chủ thể chứng chỉ: `C=VN, CN=Long Vuong` (SHA-256: `028a50fa3cffe7fa3e6d08a730032ff0fdf511239c9f15aa21e263d689d58c7e`).
- `isMinifyEnabled = true`, `mapping.txt`: có (`app/build/outputs/mapping/release/mapping.txt`).
- Quy tắc R8 phải bổ sung thêm: không có.

## 2. Biểu tượng và màn hình khởi động

- Icon launcher: logo Plix (icon thích ứng: nền `primary`, logo `logo_plix_icon.png`); đã xoá icon mẫu Android Studio.
- Splash: SplashScreen API (`core-splashscreen:1.2.0`), nền `primary`, icon dùng lại lớp foreground của icon launcher.

## 3. Kiểm thử bản release

| Bước | Nội dung | Kết quả |
|---|---|---|
| D1 | Icon launcher là logo Plix | Đạt (hiển thị đúng logo Plix trên màn hình chính) |
| D2 | Splash nền `primary` + logo khi mở lại app | Đạt (hiển thị đúng nền `primary` và logo khi khởi động) |
| D3 | `run-as` báo not debuggable | Đạt (`run-as: package not debuggable`) |
| R1 | Đăng nhập | Đạt (đăng nhập thành công, vào màn hình chính bình thường) |
| R2 | Thêm giao dịch (45.000, "cà phê test R8") | Đạt (lưu thành công và hiển thị đúng trên danh sách) |
| R3 | Đồng bộ (biểu tượng dừng quay, dòng có trên bảng `transactions`) | Đạt (đồng bộ hoàn tất, dữ liệu đã lưu trên bảng `transactions`) |
| R4 | Gợi ý phân loại ("cà phê") | Đạt (hiển thị đúng gợi ý danh mục tương ứng) |
| R5 | Mở màn Thống kê (4 chip khoảng thời gian) | Đạt (chuyển đổi mượt giữa 4 chip, số liệu hiển thị đúng) |
| R6 | Thêm giao dịch định kỳ (ngày 31 hằng tháng) | Đạt (tạo giao dịch định kỳ thành công, không xảy ra lỗi) |
| E2 | Xuất CSV (xoá dòng này nếu không làm Phần 4) | Đạt (xuất file CSV thành công, đúng định dạng dữ liệu) |

Kiểm tra log: `adb logcat -d | findstr /i "eyJ"` → Không có kết quả trả về (không rò rỉ JWT token trên logcat).

Ghi chú: bản debug cũ đã được gỡ trước khi cài bản release (khác chữ ký).

## 4. Lỗi phát hiện và cách sửa

Không phát hiện lỗi.

## 5. Lỗi ghi nhận, chưa sửa

Không có.