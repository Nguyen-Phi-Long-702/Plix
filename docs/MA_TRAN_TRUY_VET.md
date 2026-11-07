# Ma trận truy vết yêu cầu — Ngày 58

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Nguồn yêu cầu:** Master Plan Mục 5 (5.1 đến 5.8) và NF-07 đến NF-10 (Mục 6). Tổng cộng 75 mã yêu cầu.

Mỗi dòng nối một mã yêu cầu với nơi cài đặt (mã nguồn) và nơi chứng minh (kiểm thử hoặc tài liệu). Tên lớp, tên file test và tên tài liệu đều có thật trong kho mã nguồn; tên test đầy đủ nằm ở `PHU_LUC_DANH_SACH_TEST.md`.

Chú thích ưu tiên: MH = Must Have; SH = Should Have; CH = Could Have; OOS = ngoài phạm vi; Tương lai = để sau MVP.

## Tổng hợp

| Trạng thái | Số yêu cầu |
|---|---|
| Đã làm | 65 |
| Đã ghi nhận | 2 |
| Không làm | 6 |
| Ngoài phạm vi | 2 |
| **Tổng** | **75** |

Không có yêu cầu nào thiếu trạng thái. Yêu cầu “Không làm” đều là Could Have hoặc ngoài MVP và đã ghi trong `README.md` (Giới hạn đã biết) hoặc trong danh sách hạng mục thời gian dự phòng của kế hoạch.

## Bảng truy vết

| Mã | Yêu cầu | Ưu tiên | Trạng thái | Mã nguồn | Kiểm thử / tài liệu |
|---|---|---|---|---|---|
| AUTH-01 | Đăng ký/đăng nhập email + mật khẩu qua Supabase Auth | MH | Đã làm | `AuthApiService`, `AuthManager`, `AuthViewModel`, `LoginFragment`, `RegisterFragment` | `AuthViewModelTest`, `AuthManagerTest`; `UI7_KET_QUA_HOI_QUY.md` (R1) |
| AUTH-02 | App nhận access token và refresh token | MH | Đã làm | `AuthManager`, `AuthResponseDto` | `AuthManagerTest` |
| AUTH-03 | Token lưu bằng EncryptedSharedPreferences | MH | Đã làm | `EncryptedTokenStore` | `EncryptedTokenStoreTest`; `SECURITY_REVIEW.md` mục 2 |
| AUTH-04 | Mọi request gắn `Authorization: Bearer` | MH | Đã làm | `AuthInterceptor` | `test_security_jwt_all_endpoints.py` (thiếu header → 401) |
| AUTH-05 | Backend verify chữ ký JWT, lấy `user_id` | MH | Đã làm | `security.py` (`verify_jwt`) | `test_security.py`, `test_security_jwt_http.py`; `SECURITY_REVIEW.md` (SEC-02) |
| AUTH-06 | `user_id` luôn lấy từ JWT, không tin client | MH | Đã làm | `routers/*.py` (`Depends(verify_jwt)`) | `test_security_user_isolation.py` |
| AUTH-07 | Đăng nhập lần đầu cần mạng, báo lỗi rõ, không crash | MH | Đã làm | `ErrorMapper`, `AuthManager` | `ErrorMapperTest` |
| AUTH-08 | Mở lại app đã đăng nhập khi mất mạng vẫn vào được | MH | Đã làm | `AuthManager`, `MainActivity` | `AuthManagerTest`; `SECURITY_REVIEW.md` (S1) |
| AUTH-09 | Token hết hạn tự làm mới | MH | Đã làm | `AuthAuthenticator` | `AuthAuthenticatorTest`, `test_sync_expired_token.py` |
| AUTH-10 | Mỗi bản ghi local có `user_id` | MH | Đã làm | `TransactionEntity`, `CategoryEntity`, `BudgetEntity`, `GoalEntity`, `CorrectionEntity` | `AppDatabaseTest` |
| AUTH-11 | Đăng xuất: thử đồng bộ lần cuối, xác nhận nếu còn dữ liệu chờ | MH | Đã làm | `LogoutUseCase`, `LogoutRepositoryImpl`, `LogoutConfirmationDialog` | `LogoutUseCaseTest`; `ngay37-demo-noi-bo-dong-bo.md` (D1–D6) |
| AUTH-12 | Đăng nhập lại (kể cả máy khác) kéo dữ liệu về | MH | Đã làm | `SyncPuller` | `SyncPullerTest`, `SyncPullerRoomTest`; `ngay36-ket-qua-test-phuc-tap.md` |
| AUTH-13 | Chỉ 1 phương thức đăng nhập (email/mật khẩu) | OOS | Ngoài phạm vi (đúng thiết kế) | — | — |
| AUTH-14 | Quên mật khẩu qua email | CH | Không làm (Could Have) | — | Nằm trong danh sách hạng mục thời gian dự phòng |
| FIN-01 | Ghi giao dịch thu/chi đủ trường | MH | Đã làm | `AddEditTransactionFragment`, `AddEditTransactionViewModel`, `AddTransactionUseCase`, `TransactionEntity` | `AddTransactionUseCaseTest`, `AddEditTransactionViewModelTest`, `FormValidatorTest` |
| FIN-02 | Giao dịch lặp lại hằng tháng | MH | Đã làm | `RecurringTransactionWorker` | `RecurringTransactionWorkerTest`, `RecurringTransactionWorkerRoomTest` |
| FIN-03 | Sửa / xoá (mềm) giao dịch | MH | Đã làm | `UpdateTransactionUseCase`, `DeleteTransactionUseCase` | `UpdateTransactionUseCaseTest`, `DeleteTransactionUseCaseTest` |
| FIN-04 | Tìm kiếm, lọc theo danh mục và khoảng thời gian | MH | Đã làm | `FilterTransactionsUseCase`, `TransactionListFragment`, `TransactionListViewModel` | `FilterTransactionsUseCaseTest`, `TransactionListViewModelTest` |
| FIN-05 | Ngân sách theo tháng: tổng + theo danh mục | MH | Đã làm | `AddBudgetUseCase`, `UpdateBudgetUseCase`, `DeleteBudgetUseCase`, `AddEditBudgetFragment` | `AddBudgetUseCaseTest`, `UpdateBudgetUseCaseTest`, `DeleteBudgetUseCaseTest` |
| FIN-06 | Thanh tiến độ ngân sách, cảnh báo khi vượt ngưỡng | MH | Đã làm | `CalculateBudgetProgressUseCase`, `BudgetThresholdChecker`, `BudgetProgressLevel`, `BudgetAdapter` | `CalculateBudgetProgressUseCaseTest`, `BudgetThresholdCheckerTest`, `BudgetProgressLevelTest` |
| FIN-07 | Mục tiêu tiết kiệm: (mục tiêu − hiện tại) / số tháng còn lại | MH | Đã làm | `CalculateGoalRequiredMonthlyUseCase`, `GoalStatusUseCase`, `GoalUseCase` | `CalculateGoalRequiredMonthlyUseCaseTest`, `GoalStatusUseCaseTest`, `GoalUseCaseTest` |
| FIN-08 | Thống kê: chi theo danh mục, xu hướng tháng, thu vs chi, tỷ lệ tiết kiệm | MH | Đã làm | `AggregateAnalyticsUseCase`, `AnalyticsFragment`, `AnalyticsViewModel` | `AggregateAnalyticsUseCaseTest`, `AnalyticsViewModelTest` |
| FIN-09 | Financial Core chạy 100% offline sau khi đăng nhập | MH | Đã làm | `TransactionRepositoryImpl`, `AppDatabase` | `AppDatabaseTest`; `ngay37-demo-noi-bo-dong-bo.md` (A1–A5); `ngay46-demo-noi-bo-sprint4.md` (O1, O2) |
| AI-01 | Gợi ý phân loại TF-IDF + Naive Bayes tự viết | MH | Đã làm | `tfidf.py`, `naive_bayes.py`, `categorize_service.py`, `categorize.py` | `test_tfidf.py`, `test_naive_bayes.py`, `test_categorize_service.py`; `AI_METHODOLOGY.md` |
| AI-02 | Học từ sửa gợi ý, huấn luyện lại theo `user_id` | MH | Đã làm | `correction_service.py`, `retrain_service.py`, `retrain.py` | `test_correction_service.py`, `test_retrain_service.py`, `test_retrain_endpoint.py` |
| AI-03 | Model lưu riêng theo `user_id` | MH | Đã làm | `model_store.py` | `test_retrain_user_isolation.py` |
| AI-04 | Phát hiện bất thường theo danh mục và người dùng, cả cao và thấp | MH | Đã làm | `anomaly_stats.py`, `anomaly.py`, `CheckTransactionAnomalyUseCase` | `test_anomaly_stats.py`, `test_anomaly_endpoint.py`, `CheckTransactionAnomalyUseCaseTest` |
| AI-05 | Dự báo net thu − chi cuối tháng (làm mượt cấp số mũ) | SH | Đã làm | `forecast.py`, `ForecastViewModel` | `test_forecast.py`, `test_forecast_endpoint.py`, `ForecastViewModelTest` |
| AI-06 | Nhập nhanh tiếng Việt (rule-based) | SH | Đã làm | `nl_parser.py`, `parse_nl.py`, `QuickAddNlViewModel`, `QuickAddNlFragment` | `test_nl_parser.py`, `test_parse_nl_endpoint.py`, `QuickAddNlViewModelTest` |
| AI-07 | Gemini dự phòng cho nhập nhanh | CH | Đã làm | `gemini_fallback.py` | `test_gemini_fallback.py` |
| AI-08 | OCR hoá đơn | CH | Không làm (Could Have) | — | Nằm trong danh sách hạng mục thời gian dự phòng |
| AI-09 | Mọi dự đoán là API call có JWT, không chạy model trên máy | MH | Đã làm | `AiApiService`, `AiRepositoryImpl` | `test_security_jwt_all_endpoints.py` |
| AI-10 | Mất mạng: ẩn gợi ý AI, nhập tay, core không ảnh hưởng | MH | Đã làm | `NetworkObserver`, `AddEditTransactionViewModel`, `QuickAddNlViewModel` | `QuickAddNlViewModelTest`, `ForecastViewModelTest`; `ngay46-demo-noi-bo-sprint4.md` (O1, O2) |
| AI-11 | Kết quả AI là đề xuất kèm confidence, người dùng xác nhận trước khi ghi | MH | Đã làm | `validation_service.py`, `SuggestedTransactionAdapter`, `AddSuggestedTransactionsUseCase` | `test_validation_service.py`, `AddSuggestedTransactionsUseCaseTest` |
| AI-12 | Phân biệt rõ thống kê / ML / LLM trong tài liệu | MH | Đã làm | — | `README.md` (bảng Tính năng AI), `AI_METHODOLOGY.md` |
| SYNC-01 | Room là nguồn sự thật local, ghi thẳng Room | MH | Đã làm | `TransactionRepositoryImpl`, `AppDatabase` | `AppDatabaseTest` |
| SYNC-02 | Mỗi bản ghi có `updated_at` và `sync_status` | MH | Đã làm | `SyncableEntity`, `SyncStatus` | `AppDatabaseTest`; `API_CONTRACT_VERIFY.md` mục 2 |
| SYNC-03 | `SyncWorker` đẩy bản ghi chờ đồng bộ lên server | MH | Đã làm | `SyncWorker`, `SyncPusher`, `SyncScheduler` | `SyncPusherTest` |
| SYNC-04 | Backend verify token, upsert theo `user_id` từ token | MH | Đã làm | `sync_service.py`, `sync.py` | `test_sync_endpoint.py`, `test_sync_all_tables.py`, `test_sync_service.py` |
| SYNC-05 | Máy mới/cài lại: kéo toàn bộ dữ liệu theo `user_id` | MH | Đã làm | `SyncPuller`, `sync.py` | `SyncPullerTest`, `SyncPullerRoomTest`; `ngay36-ket-qua-test-phuc-tap.md` |
| SYNC-06 | Xung đột: bản `updated_at` mới hơn thắng, trùng thì giữ bản đang lưu | MH | Đã làm | `sync_service.py`, `SyncPuller` | `SyncPullerTest`, `test_sync_tombstone_duplicate.py`; `ngay36-ket-qua-test-phuc-tap.md` |
| SYNC-07 | Đăng xuất xoá token và dữ liệu local, có xác nhận khi còn dữ liệu chờ | MH | Đã làm | `LogoutUseCase`, `LogoutRepositoryImpl` | `LogoutUseCaseTest`; `ngay37-demo-noi-bo-dong-bo.md` (D1–D6) |
| SYNC-08 | Thử lại khi lỗi mạng, xử lý gửi trùng | MH | Đã làm | `SyncWorker`, `SyncPusher`, `sync_service.py` | `SyncPusherTest`, `test_sync_tombstone_duplicate.py` |
| SYNC-09 | Đồng bộ đa thiết bị thời gian thực | CH | Không làm (Could Have) | — | `README.md` (Giới hạn đã biết, mục 9) |
| SEC-01 | Không tự viết mã hoá mật khẩu/hệ thống Auth | MH | Đã làm | `AuthApiService` | `SECURITY_REVIEW.md` (SEC-01) |
| SEC-02 | Backend verify JWT bằng thư viện chuẩn | MH | Đã làm | `security.py` | `SECURITY_REVIEW.md` (SEC-02), `test_security.py` |
| SEC-03 | Mọi API dữ liệu bắt buộc JWT | MH | Đã làm | `routers/*.py` | `test_security_jwt_all_endpoints.py`, `test_security_jwt_http.py` |
| SEC-04 | `user_id` không lấy từ client | MH | Đã làm | `routers/*.py`, `sync_service.py` | `test_security_user_isolation.py` |
| SEC-05 | Mã hoá Room bằng SQLCipher | CH | Không làm (Could Have) | — | `README.md` (Giới hạn đã biết, mục 7), `SECURITY_REVIEW.md` mục 7 |
| SEC-06 | Dữ liệu gửi lên backend tối thiểu | MH | Đã làm | `AiApiService` | `SECURITY_REVIEW.md` (SEC-06) |
| SEC-07 | Khoá Gemini chỉ ở backend | MH | Đã làm | `config.py`, `gemini_fallback.py` | `SECURITY_REVIEW.md` (SEC-07); `BAO_CAO_KIEM_THU.md` (E1) |
| SEC-08 | Không log mật khẩu, token, khoá, dữ liệu nhạy cảm | MH | Đã làm | `PlixApplication`, `gemini_fallback.py` | `SECURITY_REVIEW.md` (SEC-08); `NGAY57_KET_QUA_BAN_RELEASE_CUOI.md` (kiểm tra logcat) |
| SEC-09 | Công khai Gemini có thể dùng dữ liệu, có công tắc tắt | SH | Đã làm | `SettingsFragment`, `SettingsViewModel`, `parse_nl.py` | `SECURITY_REVIEW.md` (SEC-09), `test_parse_nl_endpoint.py`; `README.md` (Tính năng AI) |
| CLOUD-01 | Backend chạy trên Render (gói miễn phí) | MH | Đã làm | `main.py` | `HUONG_DAN_TRIEN_KHAI.md` mục 3; `benchmark_render_latency.py` |
| CLOUD-02 | Supabase Postgres + Auth (gói miễn phí) | MH | Đã làm | `01_core_sync_tables.sql`, `02_ai_tables.sql`, `03_rls.sql` | `HUONG_DAN_TRIEN_KHAI.md` mục 2 |
| CLOUD-03 | Cold start Render là giới hạn chấp nhận được, có bước làm nóng | MH | Đã ghi nhận | `SyncWorker` (gọi `/health` khi đã lâu) | `README.md` (Giới hạn đã biết, mục 1), `KICH_BAN_DEMO.md` (C1) |
| CLOUD-04 | Supabase có thể tạm ngưng khi không hoạt động lâu | MH | Đã ghi nhận | — | `README.md` (Giới hạn đã biết, mục 8) |
| CLOUD-05 | Không bịa số liệu giới hạn free-tier chưa xác nhận | MH | Đã làm | — | `README.md` (Giới hạn đã biết, mục 8) |
| CLOUD-06 | GitHub Actions CI/CD | CH | Không làm (Could Have) | — | Nằm trong danh sách hạng mục thời gian dự phòng |
| NF-01 | Không phình phạm vi (ngân hàng, chuyển tiền, 2FA, microservices…) | OOS | Ngoài phạm vi (đúng thiết kế) | — | — |
| NF-02 | Phân công vai trò động Thành viên 1/2 | MH (quy trình) | Đã làm | — | Kế hoạch công việc theo ngày của nhóm (tài liệu ngoài kho mã) |
| NF-03 | Có thời gian dự phòng cho sửa lỗi, tích hợp, demo | MH (quy trình) | Đã làm | — | Kế hoạch công việc theo ngày của nhóm (tài liệu ngoài kho mã) |
| NF-04 | Bộ tài liệu học thuật: sơ đồ, test, AI, giới hạn, demo | MH (học thuật) | Đã làm | — | `SO_DO_KIEN_TRUC.md`, `SO_DO_ERD.md`, `SEQUENCE_DIAGRAMS.md`, `USE_CASE_DIAGRAM.md`, `BAO_CAO_KIEM_THU.md`, `AI_METHODOLOGY.md`, `README.md`, `KICH_BAN_DEMO.md` |
| CATEGORY-01 | Người dùng tạo danh mục riêng | MH | Đã làm | `CategoryUseCase`, `CategoryManagementFragment`, `CategoryManagementViewModel` | `CategoryUseCaseTest`, `AppDatabaseTest` |
| CATEGORY-02 | Sửa tên danh mục do mình tạo (không sửa danh mục hệ thống) | MH | Đã làm | `CategoryUseCase` | `CategoryUseCaseTest`, `test_category_service.py` |
| CATEGORY-03 | Xoá mềm danh mục của mình; giao dịch tham chiếu thành “Danh mục không xác định” | MH | Đã làm | `CategoryUseCase`, `DeletedCategoryNotice`, `BudgetDisplayName` | `DeletedCategoryNoticeTest`, `BudgetDisplayNameTest`, `AppDatabaseTest` |
| CATEGORY-04 | Màn hình quản lý danh mục (hệ thống + của người dùng) | MH | Đã làm | `CategoryManagementFragment`, `CategoryAdapter`, `CategoryGrouper` | `CategoryGrouperTest`; `KICH_BAN_DEMO.md` (B8–B8c) |
| FIN-07b | Cập nhật thủ công số tiền đã tiết kiệm của mục tiêu | MH | Đã làm | `AddEditGoalFragment`, `AddEditGoalViewModel` | `CalculateGoalRequiredMonthlyUseCaseTest` |
| FIN-06b | Thông báo cục bộ khi ngân sách vượt ngưỡng | SH | Đã làm | `NotificationHelper`, `CheckBudgetThresholdUseCase`, `BudgetThresholdNotifier` | `CheckBudgetThresholdUseCaseTest`, `BudgetThresholdCheckerTest` |
| NF-05 | Xuất dữ liệu ra CSV | SH | Đã làm | `export.py`, `export_service.py`, `ExportRepositoryImpl`, `ExportViewModel`, `SettingsFragment` | `test_export.py`, `ExportViewModelTest`; `KICH_BAN_DEMO.md` (B12) |
| NF-06 | Xoá tài khoản | Tương lai | Không làm (ngoài MVP) | — | `README.md` (Giới hạn đã biết, mục 6) |
| NF-07 | minSdk = 26 | MH | Đã làm | `build.gradle.kts` (`minSdk = 26`) | `HUONG_DAN_TRIEN_KHAI.md` mục 4.3 |
| NF-08 | MVP chỉ có giao diện sáng | MH | Đã làm | `themes.xml` (`Theme.MaterialComponents.Light`, không có thư mục `values-night`) | Kế hoạch Sprint giao diện, Ngày UI-1 (tài liệu ngoài kho mã) |
| NF-09 | Trợ năng tối thiểu: `contentDescription`, vùng chạm ≥ 48dp | MH | Đã làm (rà soát bằng mắt, chưa có biên bản riêng) | `res/layout/*.xml` (`contentDescription` trên các nút chỉ có icon) | Kế hoạch Sprint giao diện, Ngày UI-6 (tài liệu ngoài kho mã) |
| NF-10 | Mọi endpoint có tiền tố `/api/v1/` | MH | Đã làm | `main.py` (`API_PREFIX`) | `test_security_jwt_all_endpoints.py` |
