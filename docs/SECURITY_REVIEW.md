# Security Review — Ngày 52 (Sprint 5)

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Ngày thực hiện:** 01/11/2026
**Phạm vi:** toàn bộ mã nguồn backend và Android tính đến hết Sprint UI. Đối chiếu với Master Plan Mục 5.5 (SEC-01 đến SEC-09) và Mục 17.
**Cách làm:** đọc mã nguồn, tìm kiếm bằng `git grep`, và dựa vào các bộ test bảo mật đã có (không viết test mới trong Ngày 52). Số dòng theo mã nguồn lúc viết tài liệu.

## 1. Tổng hợp theo yêu cầu bảo mật

| Mã | Yêu cầu | Kết quả | Bằng chứng |
|---|---|---|---|
| SEC-01 | Không tự viết mã hoá mật khẩu hay hệ thống Auth | Đạt | Đăng ký, đăng nhập, làm mới token đều gọi Supabase Auth qua `AuthApiService`. Tìm `hashlib`, `bcrypt`, `passlib`, `hmac`, `jwt.encode` trong `backend/app` (trừ `tests`) và `android/app/src/main`: không có kết quả. |
| SEC-02 | Backend chỉ verify JWT bằng thư viện chuẩn | Đạt | `backend/app/core/security.py:19-21`: `PyJWT` (`jwt.PyJWKClient`, `jwt.decode`), thuật toán `ES256`, khoá công khai lấy từ JWKS của Supabase, kiểm tra `audience="authenticated"`; hạn dùng (`exp`) do PyJWT kiểm tra, token hết hạn trả 401. |
| SEC-03 | Mọi API dữ liệu bắt buộc JWT | Đạt | 8 file router dùng `Depends(verify_jwt)` (`sync.py` có 2 route, đăng ký cho 5 bảng thành 10 route), thêm `GET /api/v1/whoami` (`main.py:65`). Chỉ `GET /api/v1/health` công khai (dùng để làm nóng server). Test có sẵn: `test_security_jwt_all_endpoints.py`, `test_security_jwt_http.py`, `test_sync_expired_token.py`. |
| SEC-04 | `user_id` không bao giờ lấy từ client | Đạt | Không model request nào có trường `user_id` (`backend/app/models` chỉ có `category.py:5`, là model trả về); `user_id` đi từ `verify_jwt` xuống service. Test có sẵn: `test_security_user_isolation.py` (có ca gửi `user_id` giả), `test_retrain_user_isolation.py`. |
| SEC-05 | Mã hoá Room bằng SQLCipher (Could Have) | Không áp dụng | Không có SQLCipher trong cấu hình Gradle. Ghi ở mục 7 (Known Limitations). |
| SEC-06 | Dữ liệu gửi lên backend tối thiểu | Đạt (kèm ghi chú B4) | `/anomaly` chỉ nhận `category_id` và `amount`; `/categorize` chỉ `note`; `/parse-nl` chỉ `text` và `allow_gemini`; `/forecast` không có tham số (`AiApiService.java`); thống kê do server tự tính từ dữ liệu đã đồng bộ. |
| SEC-07 | Khoá Gemini chỉ ở backend, không nhúng APK | Đạt | `GEMINI_API_KEY` chỉ đọc từ biến môi trường (`core/config.py:21`), gửi bằng header `x-goog-api-key` (`gemini_fallback.py:111`). Tìm `service_role`, `SERVICE_KEY`, `sb_secret`, mẫu khoá `AIza...` trong `android`, `backend`, `docs`: không có kết quả. `backend/.gitignore` loại `.env`. Android chỉ có `SUPABASE_ANON_KEY` (khoá công khai theo thiết kế Supabase). |
| SEC-08 | Không log mật khẩu, token, khoá Gemini, dữ liệu tài chính không cần thiết | Đạt (kèm ghi chú) | Backend: chỉ `gemini_fallback.py` có `logger.warning` (dòng 104, 117, 120, 125), nội dung không chứa khoá, câu người dùng hay token. Android: Timber chỉ bật ở bản debug (`PlixApplication.java:42`); không có `HttpLoggingInterceptor`, `printStackTrace`, `System.out`; không có lời gọi `Timber` nào chứa token hoặc mật khẩu. Có 4 lời gọi `Log.e` không gắn với debug (`RecurringTransactionWorker.java:54`, `EncryptedTokenStore.java:84,88`, `GlobalExceptionHandler.java:22`), chỉ ghi thông báo cố định và exception; chấp nhận được. |
| SEC-09 | Công khai việc Gemini có thể dùng dữ liệu, có công tắc tắt | Đạt | `fragment_settings.xml`: công tắc `switchGeminiFallback` (dòng 247) và dòng thông báo (dòng 242). Backend mặc định `allow_gemini = False` (`models/parse_nl.py:10`) nên không gọi Gemini nếu app không bật. README chưa có trong repo; yêu cầu ghi "UI/README" nên phần UI đã đáp ứng. |

## 2. Xác thực và phiên đăng nhập (Mục 17.1)

| Nội dung | Kết quả | Chi tiết |
|---|---|---|
| Lưu token mã hoá | Đạt | `EncryptedTokenStore` dùng `EncryptedSharedPreferences` (khoá `AES256_SIV`, giá trị `AES256_GCM`, dòng 73-78). Khởi tạo lỗi thì xoá file cũ và tạo lại (dòng 84-89); vẫn lỗi thì `available = false` và không lưu token (dòng 40-42), không hạ xuống lưu không mã hoá. |
| Gắn token và làm mới token | Đạt | `AuthInterceptor` chỉ gắn header `Authorization`. `AuthAuthenticator` làm mới trong khối `synchronized (REFRESH_LOCK)`, thử lại tối đa 1 lần (`MAX_RESPONSE_COUNT = 2`); làm mới thất bại thì `notifySessionExpired()` và `MainActivity` đưa về màn Đăng nhập. |
| Đăng xuất | Đạt | `LogoutRepositoryImpl.clearLocalSession()` (dòng 129-131) huỷ SyncWorker đang chạy và giữ khoá trước khi xoá dữ liệu cục bộ, rồi `AuthManager.logout()` xoá token trong RAM và token đã mã hoá. |
| Mở lại app khi đã có phiên đăng nhập (AUTH-08) | Lỗi đã sửa | Xem mục 6, dòng S1. |

## 3. Cách ly dữ liệu người dùng

Mọi câu SQL trong mã chạy thật đều có điều kiện `user_id` lấy từ JWT, trừ các ngoại lệ liệt kê ở cuối bảng. Số dòng là nơi câu SQL bắt đầu (trong `backend/app`).

| File | Dòng | Điều kiện cách ly |
|---|---|---|
| `services/sync_service.py` | 65 (upsert) | `WHERE {name}.user_id = EXCLUDED.user_id`: không bao giờ ghi đè bản ghi của user khác |
| `services/sync_service.py` | 75, 82 (pull, has_more) | `WHERE user_id = $1` |
| `services/ai/anomaly_stats.py` | 86, 100 | lọc theo `user_id` (câu tra tên danh mục ở dòng 100 cũng có điều kiện chủ sở hữu) |
| `services/ai/categorize_service.py` | 28 | lọc theo `user_id` |
| `services/ai/correction_service.py` | 32 (insert) | ghi `user_id` của JWT |
| `services/ai/forecast.py` | 114 | `WHERE user_id = $1` |
| `services/ai/model_store.py` | 51, 76 | theo `user_id` |
| `services/ai/nl_parser.py` | 116 | lọc theo `user_id` |
| `services/ai/retrain_service.py` | 19, 40, 51, 63, 76 | theo `user_id` |
| `routers/categories.py` | 14 | danh mục hệ thống và của chính user |
| `core/rate_limit.py` | 14 | `WHERE user_id = $1` |

Ngoại lệ (đã xem từng chỗ):

- `routers/health.py:6` là `SELECT 1`, không đọc dữ liệu người dùng.
- `sync_service.py:93` và `services/ai/correction_service.py:23` tra chủ sở hữu theo `id`, sau đó so với `user_id` của JWT: khác thì `rejected` (`sync_service.py:200-205`) hoặc từ chối (`correction_service.py`).
- `_manual_test()` trong `retrain_service.py` (từ dòng 89), `demo_categorize_correction_retrain.py`, `benchmark_retrain.py` là script chạy tay, không router nào gọi.

`backend/sql/03_rls.sql` bật RLS và tạo policy chủ sở hữu cho 5 bảng đồng bộ. Việc script đã được chạy trên Supabase không kiểm tra được từ mã nguồn trong ngày này.

## 4. SQL injection

- Android: Room dùng DAO có tham số; `execSQL` duy nhất (`AppDatabase.java:61`) dùng tham số `?` với dữ liệu seed viết cứng.
- Backend: giá trị luôn đi qua tham số `$1..$n` của asyncpg. Có 4 chuỗi f-string trong `sync_service.py` (dòng 65, 75, 82, 93) để ghép tên bảng vào SQL; tên bảng lấy từ enum `SyncTable` và danh sách cột viết cứng (`_build_spec`), không bao giờ từ dữ liệu client. Đây là ngoại lệ so với câu "không ghép chuỗi SQL" ở Mục 17 nhưng không có đường chèn SQL.

## 5. Bảo vệ khác

| Hạng mục | Kết quả | Chi tiết |
|---|---|---|
| Chỉ dùng HTTPS | Đạt | Cả hai `BACKEND_BASE_URL` và `SUPABASE_URL` là `https://`; không có `http://` trong mã Java và `build.gradle.kts`; `network_security_config.xml` rỗng nên dùng mặc định cấm cleartext (`targetSdk = 36`). |
| Thành phần xuất ra ngoài | Đạt | Chỉ `MainActivity` có `exported="true"` (màn hình khởi chạy); `InitializationProvider` là `exported="false"`. |
| Giới hạn tần suất `/retrain` | Đạt | `RETRAIN_COOLDOWN_SECONDS` mặc định 3600 (`core/config.py:16`), `core/rate_limit.py`; test `test_rate_limit.py`. |
| Giới hạn kích thước body | Đạt (có giới hạn) | 1 MB (`core/middleware.py:7`); chỉ kiểm tra header `Content-Length` (xem mục 7). |
| Thu gọn mã bản release | Đạt | `isMinifyEnabled = true` cho `release` (`build.gradle.kts:36`); đã chạy thử ở Ngày 51 và Ngày UI-7. |
| Sao lưu tự động của hệ thống | Lỗi đã sửa | Xem mục 6, dòng S2. |
| Tách lớp MVVM | Đạt | Không Fragment hay Activity nào dùng trực tiếp DAO, `AppDatabase`, Retrofit, `RepositoryImpl` hoặc `ApiService`. |

## 6. Vấn đề tìm được khi rà soát chéo và cách xử lý

Đã sửa trong Ngày 52:

| Mã | Mức | Vấn đề | Cách xử lý |
|---|---|---|---|
| S1 | Cao | AUTH-08 chưa đạt. Token được lưu mã hoá nhưng `AuthManager` chỉ giữ token trong RAM và không nạp lại từ `EncryptedTokenStore` khi tiến trình mới khởi động; `nav_graph.xml` luôn bắt đầu ở `loginFragment`. Tắt hẳn app rồi mở lại phải đăng nhập lại (cần mạng), trái yêu cầu "tắt mạng, mở lại app đã đăng nhập trước đó vẫn vào được". `SyncWorker` chạy nền sau khi tiến trình bị huỷ cũng bỏ qua lượt đồng bộ vì không có `userId` trong RAM (`SyncWorker.java:74-76`). | `AuthManager` nạp token đã lưu vào RAM khi khởi tạo; `MainActivity` mở thẳng `mainGraph` khi `isLoggedIn()` (chỉ khi `savedInstanceState == null`). Đã kiểm tra trên emulator: đăng nhập, Force stop, mở lại vào thẳng Trang chủ; bật chế độ máy bay vẫn vào được; đăng xuất rồi mở lại về màn Đăng nhập. |
| S2 | Trung bình | `android:allowBackup="true"` cùng hai file quy tắc sao lưu còn là bản mẫu mặc định (sao lưu mọi thứ): cơ sở dữ liệu Room chứa dữ liệu tài chính dạng không mã hoá có thể bị sao lưu lên tài khoản Google. Dữ liệu đã được đồng bộ lên cloud nên không cần sao lưu hệ thống. | Đặt `android:allowBackup="false"` trong `AndroidManifest.xml`. |

Đưa vào backlog (không sửa trong Ngày 52):

| Mã | Mức | Vấn đề | Quyết định |
|---|---|---|---|
| B1 | Thấp | Record sai kiểu hoặc sai giá trị làm cả batch trả 422, trong khi schema mục 3 viết server bỏ qua từng record không hợp lệ và `invalid_data` chưa bao giờ được trả (xem `docs/API_CONTRACT_VERIFY.md`, mục 5). | Long và Vương thống nhất: đổi server cho khớp câu chữ của schema, hoặc ghi chú rõ hành vi trong schema. |
| B2 | Thấp | `GET /api/v1/whoami` (`main.py:65`) không thuộc danh sách endpoint ở Mục 15. | Giữ lại: test (`security_helpers.py`, `test_security_*`) và script (`eval_categorize.py`, `demo_categorize_correction_retrain.py`) đang dùng; chỉ trả `user_id` của chính token. |
| B3 | Thấp | `FastAPI(title="Plix API", lifespan=lifespan)` (`main.py:41`) không tắt `/docs` và `/openapi.json` mặc định. | Chấp nhận cho phạm vi đồ án; chưa kiểm tra trên máy chủ Render. |
| B4 | Thấp | Dòng thông báo SEC-09 ghi "Chỉ gửi câu chữ", nhưng prompt gửi Gemini còn kèm danh sách tên danh mục của người dùng (`build_prompt`, `gemini_fallback.py:64`). | Bổ sung vào dòng thông báo khi viết README ở Sprint 6. |
| B5 | Thấp | Bản ghi bị server `rejected` được Android giữ `pending` và đẩy lại mỗi lượt đồng bộ (`SyncPusher.java:65-66`). | Kế hoạch chưa có yêu cầu xử lý riêng; để sau. |
| B6 | Thấp | Thư viện `logging-interceptor` (`build.gradle.kts:79`) không được dùng trong mã. | Xoá khi dọn dependency. |
| B7 | Thấp | Chuyển dữ liệu giữa hai máy (Android 12 trở lên) do `data_extraction_rules.xml` quyết định; file này vẫn là bản mẫu mặc định và chưa kiểm tra ảnh hưởng của `allowBackup="false"` lên việc này. | Kiểm tra khi có thời gian dự phòng. |
| B8 | Thấp | `backend/docs/ngay51-ket-qua-regression-va-ban-release.md` mục 3 còn 5 ô `<ĐẠT/KHÔNG ĐẠT>` và dòng 37 chưa điền. | Đã xử lý ở Ngày 53: Long điền kết quả thật. |

## 7. Known Limitations (bảo mật)

- Chưa mã hoá cơ sở dữ liệu Room bằng SQLCipher (SEC-05 là Could Have): dữ liệu tài chính cục bộ không mã hoá khi mất máy và máy không có khoá màn hình.
- Chưa hỗ trợ xoá tài khoản (Post-MVP theo Mục 17).
- Giới hạn kích thước body chỉ kiểm tra header `Content-Length`, không đếm byte thật của body dạng chunked (ghi trong `core/middleware.py`).