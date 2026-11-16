# Kết quả xác minh kỹ thuật TN-2, TN-3, TN-5

**Người thực hiện:** Nguyễn Phi Long
**Ngày chạy:** 13/11/2026
**Thiết bị:** Pixel 6 Pro (AVD), Android 12


## CXM-08. Mô hình ML Kit qua Google Play services: lần quét đầu khi offline

**Ngày chạy:** 18/11/2026
**Thiết bị:** Samsung A15, Android 16

Cách làm: gỡ app, cài lại bản debug, đăng nhập khi có mạng, bật chế độ máy bay, quét một ảnh hoá đơn bằng "Chọn từ thư viện"; sau đó bật mạng và quét lại cùng ảnh.

- Google Play services trên thiết bị: Có; app Play Store: Có.
- Trước lần quét này, thiết bị đã từng quét hoá đơn bằng ML Kit: Chưa.
- Lần quét đầu khi offline: Hiện form trống, không có điền gì cả.
- Thông báo hiện ra khi mô hình chưa sẵn sàng (nguyên văn): Số tiền chưa chắc chắn, vui lòng kiểm tra; nút "Nhập tay": Có.
- Quét lại cùng ảnh khi có mạng: Hiện form trống, không có điền gì cả.
- Kết luận theo quan sát: Không rõ.

## CXM-09. Chụp bằng Intent hệ thống có cần quyền CAMERA khi Manifest không khai báo

**Ngày chạy:** 18/11/2026
**Thiết bị:** Samsung A15; máy thật

- Manifest: có `uses-feature android.hardware.camera required=false`, không có `uses-permission CAMERA`.
- Bấm "Chụp ảnh": ứng dụng máy ảnh hệ thống có mở; không có hộp thoại xin quyền CAMERA.
- Chụp xong quay về Plix: Có chạy đọc ảnh và hiện ra form trống chưa điền gì cả.
- Kết luận theo quan sát: Không cần quyền CAMERA.

## CXM-10. Phiên bản thư viện giải quyết được

Lệnh: `gradlew.bat :app:dependencies --configuration debugRuntimeClasspath` (trong `android`). Cột "Giải quyết thành" là phiên bản cuối cùng trong cây phụ thuộc (số sau `->` nếu có).

| Thư viện | Khai báo | Giải quyết thành | Kết luận |
|---|---|---|---|
| `com.google.android.gms:play-services-mlkit-text-recognition` | 19.0.2 | 19.0.2 | Giải quyết được |
| `androidx.swiperefreshlayout:swiperefreshlayout` | 1.2.0 | 1.2.0 | Giải quyết được |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | 2.9.0 | Giải quyết được (Gradle tự nâng từ 2.8.7 lên 2.9.0 vì thư viện khác yêu cầu) |
| `androidx.activity:activity` | không khai báo riêng | 1.8.0 | Ghi nhận |
| `androidx.core:core` | không khai báo riêng | 1.16.0 | Ghi nhận |

- `ActivityResultContracts.PickVisualMedia` và `TakePicture` có trong `androidx.activity` 1.8.0: Có.
- Build debug (`assembleDebug`) sau khi thêm 3 thư viện: BUILD SUCCESSFUL.
- Mở app sau khi thêm 3 thư viện: mở được, không crash.
- Phiên bản thay thế: Không có.


## Kết quả test thủ công quét hoá đơn (DP-8-L2)

**Ngày chạy:** 18/11/2026
**Thiết bị:** Samsung A15

| Mã | Kết quả | Ghi chú |
|---|---|---|
| TN2-M01 | Đạt | camera hệ thống mở; không có hộp thoại xin quyền CAMERA |
| TN2-M02 | Đạt | không xin quyền đọc ảnh; ảnh gốc còn nguyên |
| TN2-M05 | Đạt | tình huống 1: rỗng; tình huống 2: rỗng; tình huống 3: rỗng |

### Lỗi tìm thấy (DP-8-L2)

- Không có lỗi mới.