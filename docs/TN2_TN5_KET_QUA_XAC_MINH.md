# Kết quả xác minh kỹ thuật TN-2, TN-3, TN-5

**Người thực hiện:** Nguyễn Phi Long
**Ngày chạy:** 13/11/2026
**Thiết bị:** Pixel 6 Pro (AVD), Android 12

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