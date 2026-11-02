# Báo cáo kiểm thử — Ngày 55

**Người thực hiện:** Nguyễn Phi Long
**Ngày chạy:** 02/11/2026
**Nhánh / commit đã kiểm thử:** `sprint7-long` / `ee8d8b1`
**Thiết bị Android:** Emulator Pixel 5 (AVD), Android 11
**Nguồn đối chiếu:** Master Plan Mục 19 (Testing Strategy) và các biên bản kiểm thử đã có trong repo (đường dẫn ghi ở từng mục).

Danh sách từng test case: `docs/PHU_LUC_DANH_SACH_TEST.md`. Quy ước kết quả: ĐẠT / KHÔNG ĐẠT.

## 1. Phạm vi và cách kiểm thử

| Loại kiểm thử | Công cụ / cách chạy | Mục Master Plan |
|---|---|---|
| Unit test Android | JUnit 4 trên JVM; `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | 19.1 |
| Test instrumented Android | Room thật, WorkManager, kho token mã hoá trên emulator; `gradlew.bat connectedDebugAndroidTest` (trong `android`) | 19.2, 19.8 |
| Test backend | `pytest` với dữ liệu giả lập, không kết nối Postgres hay Render thật; `python -m pytest -q` (trong `backend`) | 19.1, 19.2, 19.3, 19.5, 19.6 |
| Script đồng bộ trên Postgres thật | `backend/scripts/simulate_two_devices.py`, `sync_functional_check.py`, `sync_edge_check.py`, chạy trên local và Render | 19.5 |
| Kiểm thử thủ công trên emulator | Các buổi demo và kiểm thử luồng chính (Mục 6, Mục 7) | 19.4, 19.7 |
| Benchmark hiệu năng | Room: `TransactionListPerformanceTest`; Backend: `backend/scripts/benchmark_render_latency.py` | 19.8 |

Không có load test và stress test (ngoài phạm vi đồ án theo Master Plan Mục 19.8).

## 2. Kết quả chạy toàn bộ bộ test tự động

| Hạng mục | Lệnh | Số test | Số lỗi | Kết quả |
|---|---|---|---|---|
| Unit test Android | `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | 409 | 0 | ĐẠT |
| Test instrumented Android | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | 29 | 2 | KHÔNG ĐẠT (2 test hiệu năng Room, xem Mục 4) |
| Backend | `python -m pytest -q` (trong `backend`) | 379 | 0 | ĐẠT |

Tổng cộng 817 test tự động.

Số test chạy ra khớp số test đếm được trong mã nguồn: 409 hàm `@Test` (unit), 29 hàm `@Test` (instrumented), 379 test pytest thu thập được (backend có 252 hàm `test_`, một số hàm được chạy nhiều lần với các tham số khác nhau).

Ghi chú về 2 lỗi instrumented: đó là `transactionList_5000Rows_sortedListUnder100ms` (trung vị 156.2 ms) và `transactionList_filterByCategoryAndDateRange_under100ms` (trung vị 161.7 ms), cùng vượt mục tiêu dưới 100 ms ở lần chạy full. Không có test nào khác lỗi. Sau đó chạy riêng lớp `TransactionListPerformanceTest` khi máy yên thì cả 3 phép đo đều đạt (xem Mục 4). Không sửa mã và không đổi ngưỡng.

Diễn biến kết quả hồi quy qua các mốc:

| Mốc | Unit test Android | Test instrumented | Backend | Nguồn |
|---|---|---|---|---|
| Ngày 51 (20/10/2026) | 288 test, 0 lỗi | 25 test, 0 lỗi ở lần chạy lại (lần đầu 1 lỗi hiệu năng) | 379 test, 0 lỗi | `backend/docs/ngay51-ket-qua-regression-va-ban-release.md` |
| Ngày UI-7 (31/10/2026) | 409 test, 0 lỗi | 29 test, 0 lỗi | 379 test, 0 lỗi | `docs/UI7_KET_QUA_HOI_QUY.md` |
| Ngày 53 (01/11/2026) | 409 test, 0 lỗi | 29 test, 2 lỗi hiệu năng ở cả 2 lần chạy | 379 test, 0 lỗi | `docs/NGAY53_KET_QUA_KIEM_THU_CUOI.md` |
| Ngày 55 (02/11/2026) | 409 test, 0 lỗi | 29 test, 2 lỗi hiệu năng ở lần chạy full (chạy riêng lớp hiệu năng khi máy yên: 3/3 đạt) | 379 test, 0 lỗi | Mục 2 và Mục 4 của báo cáo này |

Số test Android khác Ngày 51 (288 và 25) vì Sprint UI đã xoá 2 file test mẫu và thêm test mới.

## 3. Test case theo nhóm chức năng

Cột "Kết quả" lấy từ lần chạy ở Mục 2. Tên từng test nằm trong `docs/PHU_LUC_DANH_SACH_TEST.md`.

### 3.1. Unit test Android — 409 test

| Nhóm | Lớp test | Số test | Nội dung kiểm tra chính | Kết quả |
|---|---|---|---|---|
| Xác thực, phiên đăng nhập, đăng xuất | `AuthAuthenticatorTest`, `AuthManagerTest`, `AuthViewModelTest`, `LogoutUseCaseTest` | 17 | Làm mới token đúng 1 lần dù nhiều yêu cầu cùng gặp 401; đọc `sub` và email từ token; Đăng ký báo lỗi khi mật khẩu nhập lại không khớp; các nhánh đăng xuất (không còn dữ liệu chờ, đồng bộ lần cuối thành công, mất mạng, quá thời gian chờ, "Vẫn đăng xuất", huỷ) | ĐẠT |
| Xử lý lỗi | `ErrorMapperTest` | 18 | Chuyển ngoại lệ và mã lỗi HTTP (gồm 400, 401, 500, 503 và lỗi AI bận) thành loại lỗi và thông báo hiển thị | ĐẠT |
| Kiểm tra hợp lệ dữ liệu nhập | `FormValidatorTest` | 28 | Số tiền (0, âm, vượt giới hạn trên, đúng giới hạn), ghi chú tối đa 500 ký tự, ngày giao dịch quá xa, kỳ ngân sách, tên/số tiền hiện tại/hạn của mục tiêu | ĐẠT |
| Giao dịch (use case) | `AddTransactionUseCaseTest`, `UpdateTransactionUseCaseTest`, `DeleteTransactionUseCaseTest`, `FilterTransactionsUseCaseTest`, `FindSimilarTransactionUseCaseTest`, `AddSuggestedTransactionsUseCaseTest`, `CheckTransactionAnomalyUseCaseTest` | 27 | Thêm, sửa (đánh dấu chờ đồng bộ), xoá mềm; lọc theo danh mục và khoảng ngày, loại giao dịch đã xoá; tìm giao dịch tương tự; lưu các giao dịch đề xuất từ Nhập nhanh; xử lý kết quả phát hiện bất thường (kèm cờ còn giao dịch chờ đồng bộ) | ĐẠT |
| Danh mục | `CategoryUseCaseTest` | 14 | Chỉ sửa/xoá danh mục của chính người dùng; từ chối sửa/xoá danh mục hệ thống; từ chối tên rỗng, dài hơn 50 ký tự, trùng tên và loại với danh mục hệ thống | ĐẠT |
| Ngân sách | `AddBudgetUseCaseTest`, `BudgetThresholdCheckerTest`, `CalculateBudgetProgressUseCaseTest`, `CheckBudgetThresholdUseCaseTest`, `DeleteBudgetUseCaseTest`, `UpdateBudgetUseCaseTest` | 40 | Tiến độ (chỉ tính chi tiêu, loại giao dịch đã xoá, giao dịch chưa phân loại tính vào ngân sách tổng, hạn mức 0); các mốc ngưỡng cảnh báo; kiểm tra sau khi thêm, sửa giao dịch và sau khi kéo dữ liệu về; chặn 2 ngân sách tổng cùng tháng | ĐẠT |
| Mục tiêu tiết kiệm | `CalculateGoalRequiredMonthlyUseCaseTest`, `GoalStatusUseCaseTest`, `GoalUseCaseTest` | 14 | Số tiền cần tiết kiệm mỗi tháng (làm tròn lên, đã đạt, đã vượt, đến hạn chưa đạt); trạng thái đang thực hiện/đã đạt/đã hết hạn tính lại mỗi lần đọc; xoá mục tiêu của người khác bị từ chối | ĐẠT |
| Thống kê | `AggregateAnalyticsUseCaseTest`, `CalculateMonthToDateNetUseCaseTest` | 16 | Chi theo danh mục, xu hướng theo tháng, tổng thu/chi, tỷ lệ tiết kiệm (kể cả thu bằng 0), danh sách rỗng, loại giao dịch đã xoá; chuỗi số dư ròng lũy kế từ đầu tháng | ĐẠT |
| Giao dịch định kỳ | `RecurringTransactionWorkerTest` | 17 | Sinh đúng 1 giao dịch mỗi kỳ, chống trùng khi chạy 2 lần, bù các tháng thiếu, ngày 31 vào tháng có 30/28/29 ngày, sửa quy tắc không đổi giao dịch đã sinh, xoá mẫu thì dừng sinh nhưng giữ giao dịch đã sinh, chỉ chạy khi còn đăng nhập | ĐẠT |
| Đồng bộ | `SyncPullerTest`, `SyncPusherTest`, `SyncTombstoneTest`, `SyncLockTest`, `SyncTablesTest`, `DeletedCategoryNoticeTest` | 43 | Kéo theo con trỏ (lô 500, con trỏ không tiến khi lỗi, dừng khi 2 lần trả cùng tập bản ghi); đẩy theo lô 50 (kể cả trên 100 bản ghi, mất mạng giữa chừng); quy tắc xung đột (bản mới hơn thắng, trùng thời gian thì giữ bản đã có); xoá mềm giữ nguyên ở cả 5 bảng; thứ tự 5 bảng; thông báo giao dịch chuyển sang "Danh mục không xác định" | ĐẠT |
| ViewModel: Thêm/Sửa giao dịch, Trang chủ, Nhập nhanh, danh sách giao dịch | `AddEditTransactionViewModelTest`, `HomeViewModelTest`, `QuickAddNlViewModelTest`, `TransactionListViewModelTest`, `TransactionGrouperTest` | 106 | Form Thêm/Sửa (kiểm tra hợp lệ, gợi ý phân loại AI đủ trạng thái, banner bất thường, giao dịch định kỳ, cảnh báo giao dịch tương tự); Trang chủ (tổng quan tháng, giao dịch gần đây, ngân sách, trạng thái đang tải/rỗng); Nhập nhanh (phân tích, chọn/sửa từng dòng, lưu, AI bận, tắt hoặc ngoại tuyến); danh sách giao dịch nhóm theo ngày | ĐẠT |
| ViewModel: Thống kê và Dự báo | `AnalyticsViewModelTest`, `ForecastViewModelTest` | 33 | Khoảng thời gian, trạng thái đang tải/rỗng/có dữ liệu, gộp danh mục, cột thu/chi theo tháng; dự báo: đang tải/thành công/chưa đủ dữ liệu/lỗi, ẩn thẻ khi mất mạng | ĐẠT |
| Logic hiển thị dùng chung | `BudgetDisplayNameTest`, `BudgetProgressLevelTest`, `CategoryGrouperTest`, `AmountTextWatcherTest`, `CategoryIconMapperTest`, `PaymentMethodUiTest`, `GoalDisplayTextTest`, `CategorySummaryTextTest` | 36 | Tên ngân sách ("Danh mục không xác định" khi danh mục đã xoá), mức màu thanh tiến độ, nhóm danh mục, dấu phân cách nghìn khi nhập tiền, icon danh mục, 5 phương thức thanh toán, chữ hiển thị mục tiêu và tóm tắt danh mục | ĐẠT |
| **Tổng** | | **409** | | |

### 3.2. Test instrumented Android — 29 test

| Nhóm | Lớp test | Số test | Nội dung kiểm tra chính | Kết quả |
|---|---|---|---|---|
| Cơ sở dữ liệu Room | `AppDatabaseTest` | 18 | Nạp 12 danh mục mặc định; thêm và đọc lại giao dịch, ngân sách, mục tiêu, dữ liệu sửa gợi ý; ràng buộc danh mục không trùng tên và loại; xoá mềm ở giao dịch, danh mục, ngân sách, mục tiêu (không còn trong truy vấn đang dùng nhưng vẫn đọc được theo mã); bản ghi xoá mềm của cả 5 bảng vẫn nằm trong danh sách chờ đồng bộ; truy vấn giao dịch tương tự | ĐẠT |
| Hiệu năng Room | `TransactionListPerformanceTest` | 3 | 5.000 giao dịch giả lập, mục tiêu trung vị dưới 100 ms (xem Mục 4) | ĐẠT (3/3) ở lần chạy riêng lớp test khi máy yên; KHÔNG ĐẠT (2/3) ở lần chạy full của Mục 2 |
| Giao dịch định kỳ trên Room thật | `RecurringTransactionWorkerRoomTest` | 3 | Sinh đúng 1 giao dịch cho kỳ hiện tại; chạy 2 lần không trùng; mẫu đã xoá mềm bị loại khỏi truy vấn mẫu định kỳ | ĐẠT |
| Kéo dữ liệu trên Room thật | `SyncPullerRoomTest` | 2 | Lô hợp lệ lưu đủ và đẩy con trỏ cùng lúc; lô có bản ghi hỏng không ghi gì và giữ nguyên con trỏ | ĐẠT |
| Kho token mã hoá | `EncryptedTokenStoreTest` | 2 | Lưu và đọc lại token; xoá token | ĐẠT |
| Theo dõi kết nối mạng | `NetworkObserverTest` | 1 | Khởi tạo không lỗi và có `LiveData` trạng thái mạng | ĐẠT |
| **Tổng** | | **29** | | |

### 3.3. Test backend — 379 test

| Nhóm | File test (`backend/app/tests`) | Số test | Nội dung kiểm tra chính | Kết quả |
|---|---|---|---|---|
| Bảo mật JWT và cách ly dữ liệu | `test_security` (3), `test_security_jwt_all_endpoints` (74), `test_security_jwt_http` (8), `test_security_user_isolation` (27), `test_sync_expired_token` (2) | 114 | Token hợp lệ, hết hạn, sai chữ ký; token giả (khoá khác, sửa payload, `alg=none`, ký HS256), thiếu `sub`, thiếu header Authorization trên cả 18 endpoint; `user_id` do client gửi không bao giờ tới database; mỗi endpoint chỉ đọc dữ liệu của chính người gọi; hết hạn giữa lúc đồng bộ rồi thử lại với token mới | ĐẠT |
| Đồng bộ | `test_sync_endpoint` (10), `test_sync_all_tables` (9), `test_sync_service` (25), `test_sync_tombstone_duplicate` (7), `test_sync_categories_write` (7) | 58 | Đẩy/kéo cho 5 bảng; bản mới hơn ghi đè; xoá mềm không hồi sinh; gửi trùng yêu cầu; ghi danh mục (danh mục hệ thống và của người khác bị từ chối, không làm hỏng cả lô) | ĐẠT |
| Phân loại AI (TF-IDF + Naive Bayes) | `test_tfidf` (11), `test_naive_bayes` (13), `test_categorize_service` (8), `test_seed_dataset` (3) | 35 | Giá trị TF-IDF và Naive Bayes (có làm mượt Laplace) khớp kết quả tính tay; dùng model riêng khi có từ 10 mẫu huấn luyện trở lên, ngược lại dùng model mặc định; danh mục dự đoán phải nằm trong danh sách hợp lệ; tập seed không trùng tập kiểm tra | ĐẠT |
| Sửa gợi ý và huấn luyện lại | `test_correction_endpoint` (3), `test_correction_service` (5), `test_retrain_endpoint` (3), `test_retrain_no_data` (2), `test_retrain_service` (2), `test_retrain_user_isolation` (2), `test_rate_limit` (4) | 21 | Lưu lần sửa đúng chủ giao dịch; giới hạn tần suất (chặn trong thời gian chờ, cho phép khi đúng bằng thời gian chờ); khoá chống chạy đồng thời; 400 khi chưa có dữ liệu; model của 2 người dùng độc lập | ĐẠT |
| Phát hiện bất thường | `test_anomaly_stats` (16), `test_anomaly_endpoint` (4) | 20 | Trung bình + độ lệch chuẩn (từ 10 mẫu), khoảng tứ phân vị (5 đến 9 mẫu), "chưa đủ dữ liệu" (dưới 5 mẫu); cả 2 chiều cao và thấp | ĐẠT |
| Dự báo | `test_forecast` (17), `test_forecast_endpoint` (4) | 21 | Làm mượt cấp số mũ hệ số 0,3; chuỗi thu − chi theo tuần (giờ Việt Nam); dưới 4 tuần trọn vẹn thì "chưa đủ dữ liệu"; chọn tháng; từ chối định dạng tháng sai | ĐẠT |
| Nhập nhanh và Gemini dự phòng | `test_nl_parser` (38), `test_parse_nl_endpoint` (10), `test_gemini_fallback` (28), `test_validation_service` (26) | 102 | Tách câu và nhận diện số tiền; khớp từ khoá với danh mục; chỉ gọi Gemini khi mọi phần đều dưới ngưỡng tin cậy và `allow_gemini = true`; lỗi gọi Gemini (mã 4xx/5xx, mất mạng, quá thời gian, phản hồi không phải JSON) coi là không khả dụng và trả `AI_BUSY`; kiểm tra schema và danh mục hợp lệ của kết quả Gemini | ĐẠT |
| Quyền ghi danh mục, giới hạn body, định dạng lỗi | `test_category_service` (5), `test_request_size_limit` (2), `test_validation_error_format` (1) | 8 | Danh mục của chính mình hợp lệ, danh mục hệ thống hoặc của người khác bị từ chối; body trong 1 MB được nhận, vượt 1 MB bị từ chối; lỗi trả về theo định dạng chung | ĐẠT |
| **Tổng** | | **379** | | |

## 4. Kiểm thử hiệu năng (Master Plan Mục 19.8)

Mục tiêu: Room truy vấn danh sách giao dịch với 5.000 bản ghi dưới 100 ms; Backend phản hồi dưới 3 giây sau khi làm nóng. Chỉ là benchmark cơ bản, không phải load test hay stress test.

### 4.1. Room — Ngày 50

Nguồn: `backend/docs/ngay50-ket-qua-hieu-nang-room.md`. Ngày chạy 19/10/2026, thiết bị Medium Phone (Android 16, emulator), bản debug, test `TransactionListPerformanceTest`.

Cách đo: tạo 5.000 giao dịch giả lập cố định (trải khoảng 21 tháng) trong file Room riêng; mỗi phép đo chạy 1 lần khởi động (không tính) rồi đo 5 lần, lấy trung vị so với 100 ms.

| Phép đo | Số dòng | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |
|---|---|---|---|---|---|---|
| `getAll`, sắp xếp mới nhất trước | 5000 | 53.7 | 59.9 | 73.2 | < 100 ms | ĐẠT |
| `getAll` + lọc danh mục + khoảng ngày | 73 | 60.2 | 71.6 | 75.8 | < 100 ms | ĐẠT |
| `searchByNote` + sắp xếp | 111 | 5.7 | 6.6 | 6.6 | < 100 ms | ĐẠT |

### 4.2. Room — các lần đo lại

Cột là trung vị (ms). Dấu "—" nghĩa là biên bản không ghi số, chỉ ghi test đạt.

| Lần chạy | Ngày | Thiết bị | `getAll` | `getAll` + lọc | `searchByNote` | Kết quả |
|---|---|---|---|---|---|---|
| Ngày 50 | 19/10/2026 | Medium Phone, Android 16 | 59.9 | 71.6 | 6.6 | ĐẠT |
| Ngày 51, lần đầu | 20/10/2026 | Medium Phone, Android 16 | 179.5 | — | — | KHÔNG ĐẠT (1/25 test; máy đang chịu tải sau unit test) |
| Ngày 51, chạy lại khi máy yên | 20/10/2026 | Medium Phone, Android 16 | — | — | — | ĐẠT (25/25) |
| Ngày UI-7 | 31/10/2026 | Pixel 5, Android 11 | — | — | — | ĐẠT (29/29) |
| Ngày 53, lần 1 | 01/11/2026 | Pixel 5, Android 11 | 263.6 | 261.3 | — | KHÔNG ĐẠT (2/29) |
| Ngày 53, chạy lại | 01/11/2026 | Pixel 5, Android 11 | 206.9 | 119.0 | — | KHÔNG ĐẠT (2/29) |
| Ngày 55, lần chạy full (Mục 2) | 02/11/2026 | Pixel 5, Android 11 | 156.2 | 161.7 | 31.5 | KHÔNG ĐẠT (2/3 phép đo; 2/29 test) |
| Ngày 55, chạy riêng lớp test khi máy yên | 02/11/2026 | Pixel 5, Android 11 | 82.9 | 62.3 | 9.2 | ĐẠT (3/3) |

Số nhỏ nhất và lớn nhất của 2 lần chạy Ngày 55 (ms): lần chạy full `getAll` 152.8 – 196.8, lọc 150.7 – 171.1, `searchByNote` 21.8 – 49.2; lần chạy riêng `getAll` 57.1 – 94.5, lọc 59.4 – 64.8, `searchByNote` 8.0 – 10.2. Nguồn: Logcat tag `PlixPerf` ngày 02/11/2026.

### 4.3. Đánh giá Room

Ở lần đo riêng Ngày 55 (chạy lớp `TransactionListPerformanceTest` khi máy yên) cả 3 phép đo đều dưới 100 ms (trung vị lớn nhất 82.9 ms, lần đo đơn lớn nhất 94.5 ms). Tuy nhiên lần chạy full ngay trước đó cùng ngày vẫn có 2 phép đo vượt ngưỡng (156.2 ms và 161.7 ms). Kết quả dao động giữa các lần chạy và giữa các thiết bị: Ngày 53 có 2 phép đo vượt ngưỡng ở cả hai lần chạy trên Pixel 5. Nguyên nhân chưa được xác định; ở Ngày 51, lần vượt ngưỡng xảy ra khi emulator đang chịu tải và đạt khi chạy lại lúc máy yên. Báo cáo ghi cả lần đạt và lần không đạt; không đổi ngưỡng và không sửa mã để đạt số liệu. Số đo chỉ gồm thời gian truy vấn và dựng danh sách trên emulator, bản debug, chưa gồm thời gian vẽ giao diện, nên kết quả trên máy thật có thể khác.

### 4.4. Backend trên Render — Ngày 50

Nguồn: `backend/docs/ngay50-ket-qua-hieu-nang-backend.md`, script `backend/scripts/benchmark_render_latency.py`, backend https://plix-7jfp.onrender.com.

Cách đo: đăng nhập Supabase lấy JWT thật; gọi `GET /api/v1/health` để làm nóng (không tính); mỗi endpoint gọi 1 lần khởi động rồi đo 5 lần liên tiếp, thời gian tính từ máy chạy script tới khi nhận đủ phản hồi (gồm độ trễ mạng). ĐẠT khi cả 5 lần đo dưới 3 giây.

| Endpoint | Điều kiện đo | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |
|---|---|---|---|---|---|---|
| `POST /api/v1/categorize` | ghi chú "cà phê" | 2060.4 | 2099.2 | 2793.2 | < 3000 ms | ĐẠT |
| `GET /api/v1/sync/transactions/pull` | since=0, limit=500, trả 500 bản ghi | 1669.1 | 1702.3 | 2384.0 | < 3000 ms | ĐẠT |

Lần đo chậm nhất là 2793.2 ms, dưới 3 giây. Backend chỉ đo 1 lần ở Ngày 50 (không chạy lại ở Ngày 55).

### 4.5. Huấn luyện lại mô hình — Ngày 26

Nguồn: `backend/docs/ngay26-xu-ly-van-de-phat-sinh.md` mục 3. Đo bằng `python -m app.services.ai.benchmark_retrain` với 5.000 giao dịch giả lập của 1 người dùng, chạy từ máy cá nhân tới Supabase thật (không đo trên Render): `retrain_user_model` mất 1.037 giây; ước tính 1 lần gọi `/retrain` khoảng 1.470 giây (chưa gồm xác thực JWT); mốc tham chiếu dưới 3 giây: ĐẠT.

## 5. Trường hợp biên đã kiểm thử ở các Sprint trước

Kết quả "ĐẠT" của các dòng có test tự động nghĩa là test đó nằm trong lần chạy ở Mục 2 với 0 lỗi.

### 5.1. Trường hợp biên của Ngày 49

| Mã | Trường hợp biên | Cách kiểm tra và bằng chứng | Kết quả |
|---|---|---|---|
| E1 | Bản build không chứa khoá bí mật của máy chủ (chỉ chứa khoá công khai) | Giá trị đưa vào bản build Android (`BuildConfig` trong `android/app/build.gradle.kts`) chỉ gồm `SUPABASE_URL`, `SUPABASE_ANON_KEY` (khoá công khai) và `BACKEND_BASE_URL`. Tìm `service_role`, `SERVICE_KEY`, `sb_secret`, `AIza` bằng `git grep -n -i -E "service_role\|SERVICE_KEY\|sb_secret\|AIza" -- android backend`: không có kết quả (chạy lại Ngày 55 trên commit `ee8d8b1`, cùng kết quả). Ngày 52 đã tìm cùng mẫu trong `android`, `backend`, `docs` với cùng kết quả (`docs/SECURITY_REVIEW.md`, SEC-07) | ĐẠT |
| E2 | Log không chứa dữ liệu nhạy cảm | Rà soát mã nguồn (SEC-08 trong `docs/SECURITY_REVIEW.md`): Timber chỉ bật ở bản debug; không có `HttpLoggingInterceptor`, `printStackTrace`, `System.out`; backend chỉ ghi `logger.warning` không chứa khoá, câu người dùng hay token; 4 lời gọi `Log.e` chỉ ghi thông báo cố định và exception. Đây là rà soát mã, không phải test tự động | ĐẠT |
| E3 | Worker giao dịch định kỳ chạy sau khi đã đăng xuất | `RecurringTransactionWorkerTest`: `isEligibleToRun_noSession_returnsFalse`, `isEligibleToRun_hasAccessTokenInMemory_returnsTrue`, `isEligibleToRun_afterRealLogout_returnsFalse`. Worker kiểm tra `AuthManager.isLoggedIn()` và tự huỷ công việc nếu không còn phiên | ĐẠT |
| E4 | Hơn 100 bản ghi chờ đồng bộ cùng lúc | `SyncPusherTest`: `push_120PendingRecords_sendsBatchesOf50And50And20_eachIdExactlyOnce`, `push_exactly100PendingRecords_sendsTwoFullBatchesAndNoEmptyBatch`, `push_120PendingNetworkLostOnSecondBatch_retryOnlySendsTheRemaining70` | ĐẠT |
| E5 | Ngân sách của danh mục vừa bị xoá | `BudgetDisplayNameTest.of_categoryDeleted_returnsUnknownCategoryName` (hiển thị "Danh mục không xác định"); phía giao dịch: `DeletedCategoryNoticeTest` (7 test) | ĐẠT |

### 5.2. Trường hợp biên của các Sprint khác

| Mã | Trường hợp biên | Bằng chứng | Kết quả |
|---|---|---|---|
| E6 | Ngân sách tại các mốc ngưỡng: 0%, 79%, từ 79% lên 80%, đã ở ngưỡng thì không báo lại, từ 79% lên 100%, đã vượt 100%, hạn mức bằng 0 | `BudgetThresholdCheckerTest` (7 test), `CalculateBudgetProgressUseCaseTest.execute_limitAmountZero_doesNotDivideByZero` | ĐẠT |
| E7 | Sửa giao dịch làm vượt ngưỡng; kéo dữ liệu về làm vượt ngưỡng; nhiều giao dịch kéo về cùng ngân sách chỉ báo 1 lần | `CheckBudgetThresholdUseCaseTest.checkAfterUpdate_editAmountPushesOverThreshold_notifies`, `checkAfterPull_severalPulledTransactionsSameBudget_notifiesOnlyOnce`, `checkAfterPull_alreadyAboveThresholdBeforePull_doesNotNotifyAgain` | ĐẠT |
| E8 | Chặn 2 ngân sách tổng cùng tháng; trùng danh mục cùng kỳ | `AddBudgetUseCaseTest.execute_secondOverallBudgetSameMonth_rejectsWithCorrectMessage`, `execute_secondCategoryBudgetSamePeriodAndCategory_rejectsWithCorrectMessage` | ĐẠT |
| E9 | Giao dịch định kỳ: ngày 31 vào tháng 30 ngày, tháng 2 thường và tháng 2 nhuận; bù 3 tháng bị bỏ lỡ; chạy 2 lần cùng kỳ không trùng | `RecurringTransactionWorkerTest`: `generateMissingInstances_day31RuleInto30DayMonth_clampsToDay30`, `..._day31RuleIntoNonLeapFebruary_clampsToDay28`, `..._day31RuleIntoLeapFebruary_clampsToDay29`, `..._threeConsecutiveMonthsMissed_catchesUpExactlyThreeInstances`, `..._calledTwiceSamePeriod_doesNotCreateDuplicate` | ĐẠT |
| E10 | Sửa quy tắc định kỳ không đổi giao dịch đã sinh; xoá mẫu không xoá giao dịch đã sinh; xoá giao dịch đã sinh không sinh lại | `RecurringTransactionWorkerTest`: `editingRecurrenceRuleAfterInstanceGenerated_doesNotChangePastInstance`, `softDeletingTemplate_stopsGeneratingNewInstances_butKeepsAlreadyGeneratedOnes`, `deletingGeneratedInstance_doesNotRegenerateOnNextWorkerRun`; trên Room thật: `RecurringTransactionWorkerRoomTest` (3 test) | ĐẠT |
| E11 | Mục tiêu: đã đạt, đã vượt, đến hạn chưa đạt, chia không hết (làm tròn lên); đã đạt nhưng giảm số tiền thì quay lại đang thực hiện | `CalculateGoalRequiredMonthlyUseCaseTest` (6 test), `GoalStatusUseCaseTest.execute_currentReducedBelowTargetAfterAchieved_returnsActiveAgain` | ĐẠT |
| E12 | Thống kê: danh sách rỗng hoặc `null`; thu bằng 0 không chia cho 0; loại giao dịch đã xoá | `AggregateAnalyticsUseCaseTest.execute_emptyList_returnsZeroedSummaryWithoutCrash`, `execute_nullList_...`, `execute_savingsRate_zeroIncome_doesNotDivideByZero`, `execute_excludesDeletedTransactions` | ĐẠT |
| E13 | Kiểm tra dữ liệu nhập tại biên: số tiền 0, âm, vượt giới hạn, đúng giới hạn; ghi chú đúng 500 ký tự và vượt 500; ngày quá xa | `FormValidatorTest` (28 test) | ĐẠT |
| E14 | Danh mục hệ thống không được sửa/xoá; trùng tên và loại với danh mục hệ thống; tên rỗng; tên dài hơn 50 ký tự; ghi danh mục của người khác qua đồng bộ | `CategoryUseCaseTest` (14 test), `test_category_service` (5 test), `test_sync_categories_write` (7 test); Room: `AppDatabaseTest.category_exactDuplicateNameAndType_throwsConstraintException` | ĐẠT |
| E15 | Kéo dữ liệu: hơn 500 bản ghi (lặp theo con trỏ), đúng 500 bản ghi, 501 bản ghi cùng thời gian cập nhật (dừng khi 2 lần trả cùng tập), lỗi giữa chừng giữ con trỏ lô trước, lô có bản ghi hỏng không ghi gì | `SyncPullerTest` (23 test), `SyncPullerRoomTest` (2 test) | ĐẠT |
| E16 | Xung đột: `updated_at` khác nhau thì bản mới thắng ở cả hai thứ tự đến; trùng `updated_at` thì giữ bản đã lưu; bản xoá không hồi sinh; gửi trùng yêu cầu không tạo bản ghi thứ hai | `SyncPullerTest.upsertBatch_differentUpdatedAt_newerWinsInEitherArrivalOrder`, `upsertBatch_sameUpdatedAt_firstStoredVersionWinsAndLaterOneNeverOverwrites`; backend `test_sync_tombstone_duplicate` (7 test) | ĐẠT |
| E17 | Token hết hạn giữa lúc đồng bộ; nhiều yêu cầu cùng gặp 401 chỉ làm mới token 1 lần | `AuthAuthenticatorTest.authenticate_manyConcurrent401_refreshesExactlyOnce`, `authenticate_expiredToken_refreshesOnceAndRetriesWithNewToken`; backend `test_sync_expired_token` (2 test) | ĐẠT |
| E18 | Đăng xuất: không còn dữ liệu chờ, còn dữ liệu chờ khi mất mạng, đồng bộ lần cuối quá thời gian chờ, thành công một phần, "Vẫn đăng xuất", huỷ | `LogoutUseCaseTest` (7 test) | ĐẠT |
| E19 | Token giả mạo trên cả 18 endpoint: hết hạn, ký bằng khoá khác, sai định dạng, thiếu header; token `alg=none`, ký HS256, sửa payload, thiếu `sub` | `test_security_jwt_all_endpoints` (74 test), `test_security_jwt_http` (8 test), `test_security` (3 test) | ĐẠT |
| E20 | Cách ly người dùng: `user_id` do client gửi không tới database ở cả 18 endpoint; sửa gợi ý trên giao dịch của người khác bị từ chối | `test_security_user_isolation` (27 test), `test_correction_endpoint.test_correction_is_rejected_when_transaction_belongs_to_another_user` | ĐẠT |
| E21 | Body yêu cầu vượt 1 MB; thiếu trường bắt buộc | `test_request_size_limit` (2 test), `test_validation_error_format` (1 test) | ĐẠT |
| E22 | `/retrain`: bị chặn trong thời gian chờ (429), cho phép khi đúng bằng thời gian chờ, 2 yêu cầu đồng thời chỉ 1 thành công (409), chưa có dữ liệu huấn luyện (400) | `test_retrain_endpoint` (3), `test_rate_limit` (4), `test_retrain_service` (2), `test_retrain_no_data` (2); demo thật Ngày 27: 2 yêu cầu đồng thời ra `[200, 409]`, gọi ngay sau ra 429 (`backend/docs/ngay27-demo-noi-bo-sprint3.md`) | ĐẠT |
| E23 | "Chưa đủ dữ liệu": phát hiện bất thường dưới 5 mẫu; dự báo dưới 4 tuần trọn vẹn | `test_anomaly_stats.test_fewer_than_five_samples_is_insufficient_data_for_high_amount`, `test_anomaly_endpoint.test_anomaly_returns_insufficient_data_for_new_category`, `test_forecast.test_three_complete_weeks_is_insufficient_data`, `test_four_complete_weeks_is_enough` | ĐẠT |
| E24 | Gemini lỗi hoặc hết hạn mức thì trả `AI_BUSY`; công tắc tắt hoặc thiếu trường `allow_gemini` thì không gọi Gemini | `test_gemini_fallback` (28 test), `test_parse_nl_endpoint.test_parse_nl_returns_ai_busy_when_gemini_is_unavailable`, `test_parse_nl_does_not_call_gemini_when_allow_gemini_is_false`, `test_parse_nl_does_not_call_gemini_when_allow_gemini_is_omitted`; Android: `ErrorMapperTest` | ĐẠT |
| E25 | Giao dịch tương tự: cùng số tiền, danh mục, cùng ngày; bỏ qua giao dịch đã xoá; ranh giới nửa đêm | `FindSimilarTransactionUseCaseTest` (6 test), `AppDatabaseTest.transaction_findSimilar_*` (5 test) | ĐẠT |

## 6. Kiểm thử đồng bộ dữ liệu

### 6.1. Script trên Postgres thật (Ngày 34 – 36)

| Script | Nội dung | Kết quả | Nguồn |
|---|---|---|---|
| `simulate_two_devices.py` | 5 kịch bản 2 thiết bị: bản mới hơn thắng (2 thứ tự đến), 2 thiết bị gửi đồng thời, trùng `updated_at` (2 thứ tự đến) | 5/5 đạt (Render) | `backend/docs/ngay36-ket-qua-test-phuc-tap.md` mục 2 |
| `sync_functional_check.py` | F1 đẩy mới; F2 kéo về đúng; F3 bản sửa ghi đè; F4 tombstone vẫn kéo về; F5 không hồi sinh bản đã xoá; F6 gửi trùng yêu cầu; F7 gửi lại cả lô sau mất mạng; mỗi case chạy cho 5 bảng | local 35/35, Render 35/35 | `backend/docs/ngay35-ket-qua-test-functional.md` mục 2 |
| `sync_edge_check.py` | G1 cài lại app và đăng nhập (since=0); G2a, G2b thiết bị thứ 2; G3 kéo lỗi 401 rồi kéo lại; G4 kéo một phần rồi lỗi; mỗi case chạy cho 5 bảng. G5a, G5b (chỉ bảng giao dịch, chỉ Render): xin `limit=100000` khi có 501 bản ghi, server chỉ trả 500 và `has_more = true`, kéo tiếp đủ 501 | local 25/25, Render 27/27 (gồm G5a, G5b); chạy lại Ngày 37 trên Render (không chạy G5): 25/25 | `backend/docs/ngay36-ket-qua-test-phuc-tap.md` mục 2, `backend/docs/ngay37-demo-noi-bo-dong-bo.md` mục 1 |

### 6.2. Đối chiếu tình huống bắt buộc của Master Plan Mục 18.6

Theo biên bản Ngày 37 (`backend/docs/ngay37-demo-noi-bo-dong-bo.md` mục 3): bảng đối chiếu 20 dòng (18 tình huống của Mục 18.6 cộng 2 tình huống bổ sung theo Day 36: con trỏ không tiến khi có lỗi, phản hồi quá lớn) đạt 20/20. Bằng chứng từng dòng (test tự động, script, kiểm thử thủ công) nằm ở `backend/docs/ngay36-ket-qua-test-phuc-tap.md` mục 5. Các test tự động trong bảng đó nằm trong lần chạy ở Mục 2 của báo cáo này.

### 6.3. Kiểm thử thủ công trên emulator (app → Render → Supabase)

| Buổi | Nội dung | Số bước | Kết quả | Nguồn |
|---|---|---|---|---|
| Ngày 37 | Ngoại tuyến thêm/sửa/xoá giao dịch, danh mục, ngân sách, mục tiêu (A1–A5); có mạng lại và đồng bộ (B1–B3); thiết bị thứ 2 đăng nhập, sửa rồi thiết bị 1 nhận (C1–C3); đăng xuất có và không có dữ liệu chờ, "Huỷ", "Vẫn đăng xuất", đăng nhập lại (D1–D6) | 17 | 17/17 ĐẠT | `backend/docs/ngay37-demo-noi-bo-dong-bo.md` mục 2 |
| Ngày 46 | Phát hiện bất thường cao/thấp/bình thường/chưa đủ dữ liệu (A1–A4); dự báo (F1, F2); Nhập nhanh (P1–P5); Gemini bật/tắt/AI bận (G1–G3); máy bay (O1, O2); smoke test gợi ý danh mục và đồng bộ (S1) | 17 | 17/17 ĐẠT (G3 kiểm bằng test tự động `ErrorMapperTest` và `test_parse_nl_returns_ai_busy_when_gemini_is_unavailable`) | `backend/docs/ngay46-demo-noi-bo-sprint4.md` mục 2 |

Kiểm thử ngoại tuyến (Master Plan 19.4) có bằng chứng ở Ngày 37 (A1–A5), Ngày 46 (O1, O2) và các test Room ở Mục 3.2.

## 7. Hồi quy Sprint UI và bản release (R8)

### 7.1. Hồi quy Ngày UI-7

Nguồn: `docs/UI7_KET_QUA_HOI_QUY.md`. Thiết bị Pixel 5 (AVD), Android 11.

| Hạng mục | Kết quả |
|---|---|
| Dọn tài nguyên không còn dùng | Xoá 13 icon `drawable` không còn nơi tham chiếu (tìm toàn thư mục `android`); `assembleDebug` BUILD SUCCESSFUL |
| Unit test Android | 409 test, 0 lỗi — ĐẠT |
| Test instrumented Android | 29 test, 0 lỗi — ĐẠT |
| Backend | 379 test, 0 lỗi — ĐẠT |

### 7.2. Bản release đã thu gọn mã (R8)

Cấu hình: `isMinifyEnabled = true` cho `release`, dùng `proguard-android-optimize.txt` và `proguard-rules.pro`; `assembleRelease` BUILD SUCCESSFUL, có `mapping.txt`; quy tắc phải bổ sung thêm: không có. Bản release ký tạm bằng khoá debug chỉ để chạy thử.

| Bước kiểm tra | Ngày 51 | Ngày UI-7 |
|---|---|---|
| Đăng nhập | C1 ĐẠT | R1 ĐẠT |
| Thêm giao dịch (45.000, "cà phê test R8") | C2 ĐẠT | R2 ĐẠT |
| Đồng bộ (biểu tượng dừng quay, có dòng trên bảng `transactions`) | C3 ĐẠT | R3 ĐẠT |
| Gợi ý phân loại ("cà phê") | C4 ĐẠT | R4 ĐẠT |
| Mở màn Thống kê | C5 ĐẠT | R5 ĐẠT (4 chip khoảng thời gian) |
| Thêm giao dịch định kỳ (ngày 31 hằng tháng) | — | R6 ĐẠT |

Nguồn: `backend/docs/ngay51-ket-qua-regression-va-ban-release.md` (mục 2 cấu hình, mục 3 luồng kiểm tra) và `docs/UI7_KET_QUA_HOI_QUY.md` mục 3.

## 8. Đối chiếu Master Plan Mục 19 (Testing Strategy)

| Mục | Yêu cầu | Bằng chứng | Kết quả |
|---|---|---|---|
| 19.1 | Ngân sách: các mốc 0%, 79%, 80%, 100%, trên 100%, hạn mức 0; loại giao dịch đã xoá | `BudgetThresholdCheckerTest`, `CalculateBudgetProgressUseCaseTest`, `CheckBudgetThresholdUseCaseTest` (Mục 5, E6–E7) | ĐẠT |
| 19.1 | Mục tiêu: đạt/vượt/hết hạn, trạng thái tính lại mỗi lần đọc | `CalculateGoalRequiredMonthlyUseCaseTest`, `GoalStatusUseCaseTest` (E11) | ĐẠT |
| 19.1 | Danh mục: không sửa/xoá danh mục hệ thống, chỉ chủ sở hữu được sửa/xoá | `CategoryUseCaseTest`, `test_category_service` (E14) | ĐẠT |
| 19.1 | Giao dịch: thêm, sửa, xoá mềm, lọc loại giao dịch đã xoá | `AddTransactionUseCaseTest`, `UpdateTransactionUseCaseTest`, `DeleteTransactionUseCaseTest`, `FilterTransactionsUseCaseTest`, `AppDatabaseTest` | ĐẠT |
| 19.1 | Số tiền là số nguyên, cộng dồn không sai số làm tròn | Số tiền là `long` ở Room và DTO, `BIGINT` ở Postgres, `int` ở Pydantic (`docs/API_CONTRACT_VERIFY.md` mục 2); `AggregateAnalyticsUseCaseTest.execute_totalIncomeAndExpense_summedAcrossWholeList` | ĐẠT |
| 19.1 | Giao dịch định kỳ: cắt ngày cuối tháng, chống trùng, bù kỳ thiếu | `RecurringTransactionWorkerTest` (E9, E10) | ĐẠT |
| 19.1 | TF-IDF, Naive Bayes: công thức đúng, làm mượt Laplace | `test_tfidf` (11), `test_naive_bayes` (13) | ĐẠT |
| 19.1 | Phát hiện bất thường: 3 mức ngưỡng (dưới 5 mẫu: chưa đủ dữ liệu; 5–9 mẫu: khoảng tứ phân vị; từ 10 mẫu: trung bình + độ lệch chuẩn) × 2 chiều cao/thấp | `test_anomaly_stats` (16), `test_anomaly_endpoint` (4) | ĐẠT |
| 19.1 | Dự báo: công thức thu − chi đúng, cả thu và chi được tính | `test_forecast` (17), `test_forecast_endpoint` (4) | ĐẠT |
| 19.1 | Phân tích câu nhập nhanh: tách câu, nhận diện tiền | `test_nl_parser` (38) | ĐẠT |
| 19.1 | Thống kê: 4 loại thống kê trên tập biết trước, trường hợp rỗng | `AggregateAnalyticsUseCaseTest` (9) | ĐẠT |
| 19.1 | Kiểm tra hợp lệ dữ liệu nhập: từng quy tắc | `FormValidatorTest` (28) | ĐẠT |
| 19.2 | Room DAO với Repository (5 bảng, lọc `is_deleted`) | `AppDatabaseTest` (18) | ĐẠT |
| 19.2 | FastAPI với Postgres (upsert, tie-breaker, tombstone, 5 bảng) | `test_sync_*` (pool giả) và 3 script ở Mục 6.1 (Postgres thật) | ĐẠT |
| 19.2 | Luồng đăng nhập đầu cuối | `AuthAuthenticatorTest`, `AuthManagerTest`; Ngày 37 A1, D4; Ngày 51 C1; Ngày UI-7 R1 | ĐẠT |
| 19.2 | SyncWorker với `/sync/{table}` (5 bảng, con trỏ) | `SyncPusherTest`, `SyncPullerTest`, `SyncPullerRoomTest`; Mục 6 | ĐẠT |
| 19.2 | Quản lý danh mục đầu cuối cục bộ (tạo, sửa, xoá mềm, giao dịch tham chiếu hiển thị "Danh mục không xác định") và qua đồng bộ | `CategoryUseCaseTest`, `AppDatabaseTest` (xoá mềm danh mục), `DeletedCategoryNoticeTest`, `BudgetDisplayNameTest`, `test_sync_categories_write`; Ngày 37 A3, B2, C1 | ĐẠT |
| 19.2 | Cập nhật `current_amount` của mục tiêu | `CalculateGoalRequiredMonthlyUseCaseTest.execute_afterIncreasingCurrentAmount_requiredMonthlyDecreasesImmediately`; Ngày 37 C2 (sửa mục tiêu rồi lên server) | ĐẠT |
| 19.2 | `/retrain`: giới hạn tần suất và khoá đồng thời (2 yêu cầu đồng thời, 1 thành công, 1 nhận 409) | `test_retrain_endpoint.test_concurrent_retrain_requests_only_one_succeeds`; demo thật Ngày 27 (E22) | ĐẠT |
| 19.2 | `RecurringTransactionWorker` với Room | `RecurringTransactionWorkerRoomTest` (3) | ĐẠT |
| 19.3 | JWT không hợp lệ, hết hạn, giả mạo | E19 | ĐẠT |
| 19.3 | Cách ly dữ liệu người dùng qua mọi endpoint (gồm `GET /categories`) | E20 | ĐẠT |
| 19.3 | Bản build không chứa khoá dịch vụ (kiểm tra trên mã nguồn và cấu hình build) | E1 | ĐẠT |
| 19.3 | Quyền ghi danh mục: người dùng A không ghi/xoá được danh mục của B | `test_category_service`, `test_sync_categories_write` (E14) | ĐẠT |
| 19.4 | Hoạt động ngoại tuyến | Ngày 37 A1–A5, Ngày 46 O1–O2, `AppDatabaseTest` | ĐẠT |
| 19.5 | Đồng bộ 5 bảng: tombstone, tie-breaker, con trỏ, đăng xuất, đua luồng | Mục 6; E15–E18 | ĐẠT |
| 19.6 | AI trên dữ liệu cố định (phân loại, bất thường, dự báo, phân tích câu) | Test ở Mục 3.3. Đo độ chính xác phân loại trên tập 50 câu cố định: Ngày 25 đạt 38/50 (76,0%) với seed 500 dòng, Ngày 26 đạt 43/50 (86,0%) với seed 600 dòng, mốc nhóm đặt 80% (`backend/docs/ngay25-ket-qua-do-chinh-xac.md`, `ngay26-xu-ly-van-de-phat-sinh.md`); đánh giá chi tiết theo từng danh mục nằm ở tài liệu AI methodology | ĐẠT |
| 19.7 | Luồng chính và Quản lý danh mục trên giao diện | Thủ công: Ngày 37, Ngày 46, bản release Ngày 51 (C1–C5) và Ngày UI-7 (R1–R6). Chưa có test giao diện tự động (Espresso) | ĐẠT (thủ công) |
| 19.8 | Hiệu năng Room dưới 100 ms, Backend dưới 3 giây | Mục 4 | Room: ĐẠT ở lần đo riêng Ngày 55 (3/3), KHÔNG ĐẠT ở lần chạy full cùng ngày (2/3), xem Mục 4.2; Backend: ĐẠT |

## 9. Lỗi phát hiện trong quá trình kiểm thử và cách xử lý

| Ngày | Lỗi / hiện tượng | Cách xử lý |
|---|---|---|
| 26 | `POST /retrain` khi người dùng chưa có giao dịch nào có danh mục trả 500 và người dùng bị chặn 429 trong 1 giờ dù chưa huấn luyện được | Sửa `retrain_service.py` (xoá hàng giữ chỗ, báo `NoTrainingDataError`) và router trả 400; thêm 2 test trong `test_retrain_no_data.py` (`backend/docs/ngay26-xu-ly-van-de-phat-sinh.md`) |
| 46 | Công tắc "Cho phép dùng Gemini dự phòng" chỉ lưu cục bộ, backend vẫn gọi Gemini khi người dùng đã tắt | App gửi `allow_gemini`, backend chỉ gọi Gemini khi `allow_gemini = true` (thiếu trường coi là `false`) (`backend/docs/ngay46-demo-noi-bo-sprint4.md`) |
| 51 | 1/25 test instrumented lỗi hiệu năng ở lần chạy đầu (179.5 ms) | Không sửa mã, không đổi ngưỡng; chạy lại khi máy yên: 25/25 đạt |
| 52 (S1) | Mở lại app sau khi tắt hẳn phải đăng nhập lại; `SyncWorker` chạy nền bỏ qua lượt đồng bộ vì không có `userId` trong RAM | `AuthManager` nạp token đã lưu khi khởi tạo; `MainActivity` mở thẳng `mainGraph` khi đã đăng nhập; kiểm tra lại trên emulator (`docs/SECURITY_REVIEW.md` mục 6) |
| 52 (S2) | `android:allowBackup="true"` cùng quy tắc sao lưu mẫu có thể đưa dữ liệu tài chính chưa mã hoá lên sao lưu hệ thống | Đặt `android:allowBackup="false"` trong `AndroidManifest.xml` |
| 53 | 2 test hiệu năng Room (`transactionList_5000Rows_sortedListUnder100ms`, `transactionList_filterByCategoryAndDateRange_under100ms`) vượt 100 ms ở cả 2 lần chạy | Không sửa mã, không đổi ngưỡng; xem Mục 4.2 và 4.3 |

## 10. Vấn đề còn tồn và giới hạn của việc kiểm thử

| Hạng mục | Tình trạng |
|---|---|
| Hiệu năng Room | Lần đo riêng Ngày 55: ĐẠT (3/3); lần chạy full cùng ngày: KHÔNG ĐẠT (2/3). Kết quả dao động giữa các lần chạy (Ngày 53 vượt ngưỡng), nguyên nhân chưa xác định; số liệu và đánh giá ở Mục 4 |
| Backlog bảo mật B1–B7 | Còn mở tính đến Ngày 53, chi tiết ở `docs/SECURITY_REVIEW.md` mục 6 |
| Lệch giao diện ghi nhận chưa sửa | N1–N7 trong `docs/UI6_DOI_CHIEU_MAN_LONG.md` (chờ quyết định, không phải test lỗi) |
| Test giao diện tự động | Chưa có (Espresso); giao diện kiểm thủ công |
| Kiểm tra khoá bí mật | Thực hiện trên mã nguồn và cấu hình build; chưa giải nén file APK để tìm chuỗi |
| Test backend | Chạy với dữ liệu giả lập; kết nối Postgres thật được kiểm bằng 3 script ở Mục 6.1 |
| Hiệu năng backend | Đo 1 lần ở Ngày 50, từ máy cá nhân tới Render (gồm độ trễ mạng); không phải load test |
| Phạm vi đo hiệu năng Room | Đo trên emulator, bản debug, chưa gồm thời gian vẽ giao diện; chưa đo trên máy thật |
| Thời gian huấn luyện lại | Đo từ máy cá nhân tới Supabase, chưa đo trực tiếp trên Render |

## 11. Kết luận

Ngày 02/11/2026 trên commit `ee8d8b1`, bộ test tự động gồm 817 test chạy như sau: unit test Android 409/409 đạt, test instrumented 27/29 đạt và Backend 379/379 đạt. Các trường hợp biên của Ngày 49 và của các Sprint trước (Mục 5) đều có test hoặc bằng chứng đạt. Đồng bộ dữ liệu đạt 35/35 case chức năng và 25/25 case phức tạp trên local và Render (Mục 6), hồi quy Sprint UI và bản release R8 đạt (Mục 7).

Hiệu năng Backend đạt (lần đo chậm nhất 2793.2 ms, dưới 3 giây). Hiệu năng Room ở lần đo riêng Ngày 55: ĐẠT, trung vị lớn nhất 82.9 ms, dưới 100 ms; tuy nhiên lần chạy full cùng ngày KHÔNG ĐẠT ở 2 phép đo (`getAll` 5.000 dòng 156.2 ms, lọc danh mục + khoảng ngày 161.7 ms), nên 2 test instrumented lỗi ở Mục 2. Kết quả Room dao động giữa các lần chạy (Ngày 53 vượt ngưỡng), số liệu và đánh giá ở Mục 4.