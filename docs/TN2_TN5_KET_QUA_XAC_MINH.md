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

## CXM-13. Xếp lượt kế tiếp bằng REPLACE từ bên trong chính Worker đang chạy

**Ngày chạy:** 27/11/2026
**Nguồn:** developer.android.com, trang "Managing work" (`/develop/background-work/background-tasks/persistent/how-to/manage-work`), các phần "Conflict resolution policy" và "Stop a running Worker".

- `ExistingWorkPolicy.REPLACE`: thay công việc hiện có bằng công việc mới và huỷ công việc hiện có.
- Khi enqueue `WorkRequest` mới với `REPLACE` cho unique work, `WorkRequest` cũ lập tức bị coi là đã huỷ; Worker đang chạy bị dừng và `Result` của nó bị WorkManager bỏ qua.
- Trang không có câu nào nêu trường hợp gọi `enqueueUniqueWork(REPLACE)` từ bên trong chính Worker cùng tên, nên không xác nhận được lượt mới có được giữ lại hay không.
- Kết luận theo tài liệu: Không xác nhận được.
- Cách xử lý đã chọn (theo DP_04 DP-17-L1 việc 5 và DP_02 mục 3.1): dùng hai tên xen kẽ `reminder_daily` và `reminder_daily_alt`; lượt kế tiếp luôn xếp ở tên khác với lượt đang chạy, chọn theo ngày chẵn/lẻ của lần nhắc kế tiếp.
- Kiểm chứng thực tế (thiết bị: Pixel 4 AVD Android 10 `emulator-5554`, bản build debug nhánh `dp/long`): đặt giờ nhắc cách thời điểm mở app 2 phút (khởi động lúc 13:51, giờ đích 13:53), thông báo "Nhắc ghi chép" ("Hôm nay bạn đã ghi chi tiêu chưa?", icon `ic_stat_reminder`, chạm mở `MainActivity`) hiện lúc 13:53: Có.
- Trạng thái WorkManager (`androidx.work.workdb` bảng `WorkSpec`/`WorkName`) sau khi thông báo hiện (13:53:01.788): lượt vừa chạy (`reminder_daily_alt`) Succeeded (`state = 2`); lượt mới cho ngày kế tiếp (`reminder_daily`, `initial_delay = 86398279 ms`) Enqueued (`state = 0`): Có.
- Đăng xuất: lượt đang chờ (`reminder_daily`) chuyển sang Cancelled (`state = 5`): Có.
- Mở rộng ở DP-18-L1: nhắc định kỳ và mục tiêu (`reminder_due`) cũng dùng hai tên xen kẽ `reminder_due` và `reminder_due_alt` theo ngày chẵn/lẻ của lần nhắc kế tiếp, và `ReminderSchedulerImpl.applyForCurrentUser` không huỷ trước khi xếp, để lời gọi trong `PlixApplication.onCreate` không huỷ lượt đang tới giờ chạy khi WorkManager khởi động lại tiến trình. Khi đổi giờ nhắc, `ReminderSettingsViewModel.setDailyTime` gọi `cancelDaily()` trước `applyForCurrentUser()`.


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


## Kết quả test thủ công TN-5 (DP-13-L1)

**Ngày chạy:** 26/11/2026
**Thiết bị:** Pixel 4 (AVD `emulator-5554`, Android 10); máy thứ hai do thành viên tạo dữ liệu trên tài khoản A
**Bản build:** debug, nhánh dp/long

| Mã | Kết quả | Ghi chú |
|---|---|---|
| TN5-M01 | Đạt | Trang chủ: Snackbar 'Đã đồng bộ', `SyncWorker: Đồng bộ xong`; Giao dịch: Snackbar 'Đã đồng bộ', `SyncWorker: Đồng bộ xong`; Ngân sách: Snackbar 'Đã đồng bộ', `SyncWorker: Đồng bộ xong` và số 'Đã chi' cập nhật lên `2.840.499đ` sau khi đồng bộ 2 giao dịch mới (`Dp13 m01 lan 1`, `Dp13 m01 lan 2`); Mục tiêu: Snackbar 'Đã đồng bộ', `SyncWorker: Đồng bộ xong`; Thống kê: không có `swipeRefresh`, kéo xuống không kích hoạt đồng bộ và không hiện Snackbar (vòng quay hiển thị rất ngắn khi đồng bộ xong nhanh) |
| TN5-M02 | Đạt | Máy bay: Snackbar `'Không có mạng, dữ liệu sẽ đồng bộ khi có mạng'`, vòng quay tắt ngay không quay mãi; danh sách đã cuộn (Giao dịch): chỉ cuộn nội dung (`01/11/2026` di chuyển trong danh sách), không kích hoạt `SyncWorker` và không hiện Snackbar |
| TN5-M03 | Đạt | Mốc đầu `T0 = 15:17 26/11/2026`. Lần 1 (92 giây nền): có đồng bộ tự động (bằng chứng: hộp thoại hiện `'Đang đồng bộ dữ liệu lên máy chủ...'`, logcat `SyncWorker: Đồng bộ xong`, mốc đổi thành `15:19 26/11/2026`); lần 2 (20 giây nền ngay sau đó): không kích hoạt `SyncWorker` trong 10 giây, mốc giữ `15:19 26/11/2026`; xoay màn hình (sau hơn 70 giây ở foreground, xoay ngang/dọc 2 lần): không kích hoạt `SyncWorker` trong 10 giây, mốc giữ `15:19 26/11/2026` |
| TN5-M04 | Đạt | IDLE: `'Không có lỗi đồng bộ. Các thay đổi trên máy sẽ tự được gửi lên máy chủ khi có mạng.'` + `'Đồng bộ lần cuối: 15:11 26/11/2026'` (cùng các mốc `15:17`, `15:19`, `17:45`); SYNCING: `'Đang đồng bộ dữ liệu lên máy chủ...'` + `'Đồng bộ lần cuối: chưa có'` (lần đăng nhập đầu); ERROR (sau khi Suspend Render): `'Đồng bộ thất bại: Máy chủ phản hồi lỗi (mã 503). Dữ liệu vẫn an toàn trên máy - Plix sẽ tự thử lại khi có thể.'` + `'Đồng bộ lần cuối: 17:45 26/11/2026'` (không ghi đè mốc thành công); tài khoản B mới (khi server đang Suspend): `'Đồng bộ lần cuối: chưa có'` |
| TN5-M05 | Đạt | D1 (còn pending, có mạng): đăng xuất về màn Đăng nhập không hiện hộp thoại, đăng nhập lại A vẫn còn giao dịch `DP13 M05 D1`; D2 (offline): hộp thoại `'Còn dữ liệu chưa đồng bộ'` hiện `N=1` (`'Bạn có 1 thay đổi chưa đồng bộ lên cloud...'`), Back và chạm ngoài không đóng được, chọn Huỷ vẫn ở Cài đặt và còn `DP13 M05 D2`, chọn Vẫn đăng xuất về màn Đăng nhập và đăng nhập lại A thì `DP13 M05 D2` không còn; D3 (kéo làm mới rồi bấm Đăng xuất ngay): không văng hay treo, đồng bộ kịp và thoát về màn Đăng nhập; sau đăng xuất/đăng nhập lại A khi server đang Suspend: `'Đồng bộ lần cuối: chưa có'` |

### Lỗi tìm thấy (DP-13-L1)

- Không có lỗi mới. (Ghi chú hành vi đã biết theo DP_02 mục 6.7: ở lần kéo làm mới ngay sau khi Render vừa Resume trong lúc `SyncWorker` trước đó đang chờ retry sau lỗi 503, app hiện Snackbar `'Không thể đồng bộ lúc này, thử lại sau'`.)


## Kết quả tích hợp quét hoá đơn với form có prefill (DP-13-L2)

**Ngày chạy:** 26/11/2026
**Thiết bị:** Pixel 4 (AVD `emulator-5554`, Android 10)
**Bản build:** debug, nhánh dp/long đã gộp `dev` có `DP-7-V2`

| Mã | Kết quả | Ghi chú |
|---|---|---|
| TN2-M03 | Đạt | Offline (chờ `NetworkObserver` chuyển offline, `cardFabQuickAdd` ẩn, `cardFabScanReceipt` vẫn hiện), HD1 (`hd1.png` qua Chọn từ thư viện): số tiền `135.000`, ngày `08/11/2026`, ghi chú `HIGHLANDS COFFEE`; khối gợi ý danh mục: không có; chọn tay danh mục `Di chuyển` và lưu: được, hiện trong Giao dịch (đã xoá giao dịch thử sau khi kiểm tra). Lưu ý: nếu vừa bật máy bay <3 giây trước khi `NetworkObserver` nhận `onLost()`, form vẫn kịp gọi `/categorize` và hiện `'Không lấy được gợi ý, nhập tay'` |
| TN2-M04 | Đạt | Online, HD1 (`hd1.png` qua Chọn từ thư viện): form điền sẵn `135.000`, `08/11/2026`, `HIGHLANDS COFFEE`, loại Chi tiêu; gợi ý danh mục hiện sau ~500 ms là `'Độ tin cậy thấp · 20%'` (`sys_an_uong`) kèm `'Vui lòng kiểm tra lại danh mục.'`; Back không lưu thì danh sách không có giao dịch mới |
| TN2-M06 | Đạt | Kiểm tra trên Android Studio Network Inspector khi quét online HD1: số request `categorize`: 1 (`POST https://plix-7jfp.onrender.com/api/v1/categorize`, 27 bytes); thân request chỉ có `{"note":"HIGHLANDS COFFEE"}`: Có; request mang ảnh hoặc `parse-nl`: Không có (chỉ có 1 request telemetry nội bộ `datatransport/2.3.3` tới `firebaselogging.googleapis.com` của Google Play services/ML Kit) |
| TN2-M07 | Đạt | HD4 (`hd4.png` qua Chọn từ thư viện): số tiền `45.000`, ngày hôm nay `26/11/2026`, ghi chú `QUAN COM TAM`; Snackbar cảnh báo nguyên văn: `'Số tiền chưa chắc chắn, vui lòng kiểm tra'`; Back không lưu |
| TN2-M08 | Đạt | Chụp ảnh + `adb shell am kill com.longvuong.plix` (dừng PID 4161) rồi xác nhận ảnh: Plix khởi tạo lại tiến trình (PID 10133), không văng, quay về màn `Quét hoá đơn` (hiện `'Không có kết nối mạng'` và nút `'Nhập tay'`); `cache/receipts` sau `am force-stop` và mở lại app: rỗng |

### Lỗi tìm thấy (DP-13-L2)

- Không có lỗi mới.