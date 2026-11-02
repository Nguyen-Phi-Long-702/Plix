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