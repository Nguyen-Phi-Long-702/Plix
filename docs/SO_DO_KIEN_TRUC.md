# Sơ đồ kiến trúc hệ thống — Plix

**Người thực hiện:** Nguyễn Phi Long
**Nguồn đối chiếu:** mã nguồn trong `android/` và `backend/`.

```mermaid
flowchart TB
    subgraph APP["Ứng dụng Android (Java, MVVM, offline-first)"]
        VIEW["View<br/>Fragment + XML layout<br/>Navigation Component, thanh điều hướng dưới 4 tab"]
        VM["ViewModel (14 lớp)<br/>LiveData + UiState"]
        UC["UseCase (domain)<br/>nghiệp vụ: giao dịch, danh mục, ngân sách,<br/>mục tiêu, thống kê, đăng xuất"]
        REPO["Repository (interface + Impl)<br/>Transaction, Category, Budget,<br/>Goal, Ai, Sync, Logout"]
        LOCAL[("Room - AppDatabase<br/>5 DAO, 5 bảng<br/>nguồn dữ liệu chính khi chạy")]
        REMOTE["Retrofit ApiService<br/>Sync, Ai, Health"]
        AUTH["AuthManager<br/>+ EncryptedTokenStore<br/>AuthInterceptor, AuthAuthenticator"]
        WORK["WorkManager<br/>SyncWorker (15 phút + debounce 3 giây)<br/>SyncPusher, SyncPuller<br/>RecurringTransactionWorker (15 phút)"]
        CORE["Core: Hilt (DataModule), AppExecutors,<br/>NetworkObserver, Result, ErrorMapper,<br/>GlobalExceptionHandler"]

        VIEW -->|"observe, gọi hàm"| VM
        VM --> UC
        VM --> REPO
        UC --> REPO
        VM --> AUTH
        REPO --> LOCAL
        REPO --> REMOTE
        REPO -.->|"requestSync"| WORK
        WORK --> LOCAL
        WORK --> REMOTE
    end

    subgraph BE["Backend FastAPI (Render free)"]
        MW["MaxBodySizeMiddleware<br/>giới hạn 1 MB"]
        JWT["verify_jwt (mọi route trừ /health)<br/>ES256, lấy user_id từ claim sub"]
        ROUTERS["Routers (/api/v1)<br/>/sync/{table}/push, pull<br/>/categorize, /correction, /retrain<br/>/anomaly, /forecast, /parse-nl<br/>/categories (GET), /whoami, /health"]
        SERVICES["Services<br/>sync_service, category_service, validation_service<br/>services/ai: tfidf, naive_bayes, categorize_service,<br/>correction_service, retrain_service, model_store,<br/>anomaly_stats, forecast, nl_parser, gemini_fallback"]
        POOL["asyncpg connection pool"]

        MW --> JWT
        JWT --> ROUTERS
        ROUTERS --> SERVICES
        SERVICES --> POOL
    end

    subgraph SB["Supabase (free tier)"]
        SBAUTH["Auth<br/>tài khoản, mật khẩu băm, JWT"]
        PG[("Postgres<br/>5 bảng đồng bộ: transactions, categories,<br/>budgets, goals, corrections<br/>ai_model_params<br/>anomaly_stats (chưa dùng ở MVP)")]
    end

    GEMINI["Gemini API<br/>(nhánh dự phòng của /parse-nl)"]

    AUTH -->|"HTTPS REST: auth/v1/signup, token"| SBAUTH
    REMOTE ==>|"HTTPS + JWT Bearer<br/>/api/v1/..."| MW
    JWT -.->|"lấy khoá công khai JWKS"| SBAUTH
    POOL --> PG
    SERVICES -.->|"httpx"| GEMINI
```

## Chú giải

- **MVVM trong ứng dụng:** View chỉ hiển thị và bắt sự kiện; ViewModel giữ trạng thái màn hình; UseCase chứa nghiệp vụ (không dùng `Context`, `View`, `LiveData`); Repository quyết định đọc/ghi Room hay gọi Retrofit. Tầng `presentation` và `domain` không gọi trực tiếp DAO hay ApiService.
- **Ghi dữ liệu (offline-first):** mọi thay đổi ghi vào Room trước với `sync_status = pending`; `SyncWorker` đẩy lên máy chủ sau.
- **Đồng bộ:** `SyncWorker` đẩy lần lượt 5 bảng theo thứ tự `categories → transactions → budgets → goals → corrections` (mỗi lô 50 bản ghi), rồi kéo về theo cùng thứ tự (mỗi lô tối đa 500 bản ghi).
- **Xác thực:** ứng dụng đăng nhập qua Supabase Auth (REST) để nhận JWT; mọi request tới backend gắn JWT; backend xác minh chữ ký bằng khoá công khai JWKS của Supabase và lấy `user_id` từ claim `sub`, không tin `user_id` do client gửi.
- **AI:** chạy ở backend (phân loại, sửa gợi ý, huấn luyện lại, bất thường, dự báo, phân tích câu nhập nhanh). Gemini chỉ là nhánh dự phòng của `/parse-nl`.
- **Cơ sở dữ liệu cloud:** chỉ backend truy cập Postgres (qua `asyncpg`); ứng dụng Android chỉ gọi Supabase cho phần Auth.