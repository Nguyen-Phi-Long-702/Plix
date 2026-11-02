# Sơ đồ trình tự (Sequence Diagram)

Tài liệu mô tả 4 luồng chính của Plix theo đúng code hiện tại: Android Java (MVVM) và Backend FastAPI. Sơ đồ viết bằng Mermaid, GitHub tự hiển thị.

Quy ước chung: Fragment gọi ViewModel, ViewModel gọi UseCase, UseCase gọi Repository, Repository đọc ghi Room hoặc gọi API. Mũi tên liền là lời gọi, mũi tên đứt là kết quả trả về.

## 1. Đăng nhập

### 1.1 Đăng nhập bằng Supabase Auth

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant LF as LoginFragment
    participant VM as AuthViewModel
    participant AM as AuthManager
    participant SA as Supabase Auth
    participant TS as EncryptedTokenStore
    participant SR as SyncRepository

    U->>LF: Nhập email, mật khẩu, bấm Đăng nhập
    LF->>VM: login(email, password)
    VM-->>LF: UiState.Loading (khoá nút, hiện vòng xoay)
    VM->>AM: login(email, password, callback)
    AM->>SA: POST auth/v1/token?grant_type=password
    alt Đăng nhập thành công
        SA-->>AM: 200 access_token + refresh_token
        AM->>AM: saveSession: giữ token trong bộ nhớ
        AM->>TS: saveTokens(access, refresh)
        AM-->>VM: onSuccess
        VM->>SR: requestSync()
        Note right of SR: Lên lịch SyncWorker để kéo dữ liệu tài khoản về (xem mục 2.2)
        VM-->>LF: UiState.Success
        LF->>LF: Điều hướng sang mainGraph (màn Home)
    else Sai thông tin hoặc lỗi máy chủ
        SA-->>AM: Mã lỗi HTTP
        AM->>AM: ErrorMapper.mapAuthHttpCode
        AM-->>VM: onError(message)
        VM-->>LF: UiState.Error (hiện textError)
    else Mất mạng
        AM->>AM: ErrorMapper.mapThrowable
        AM-->>VM: onError(message)
        VM-->>LF: UiState.Error (hiện textError)
    end
```

Nguồn code: `LoginFragment`, `AuthViewModel`, `AuthManager`, `EncryptedTokenStore`, `ErrorMapper`, `SyncRepository`.

### 1.2 Dùng JWT khi gọi backend và tự làm mới token

```mermaid
sequenceDiagram
    autonumber
    participant C as OkHttp (AuthInterceptor, AuthAuthenticator)
    participant BE as Backend FastAPI
    participant AM as AuthManager
    participant SA as Supabase Auth
    participant MA as MainActivity

    C->>BE: Request kèm Authorization: Bearer access_token
    BE->>BE: verify_jwt: kiểm tra chữ ký ES256 bằng khoá công khai JWKS, audience authenticated, hạn dùng
    alt Token hợp lệ
        BE-->>C: 2xx (user_id lấy từ claim sub)
    else Token hết hạn hoặc không hợp lệ
        BE-->>C: 401
        C->>AM: refreshSync() (chỉ retry 1 lần)
        AM->>SA: POST auth/v1/token?grant_type=refresh_token
        alt Làm mới thành công
            SA-->>AM: 200 token mới
            AM->>AM: saveSession (lưu token mới)
            C->>BE: Gửi lại request với token mới
        else Làm mới thất bại
            AM->>AM: notifySessionExpired()
            AM-->>MA: sessionExpired
            MA->>MA: Điều hướng về authGraph (màn Đăng nhập)
        end
    end
    Note over C,MA: Nếu request gửi lại vẫn bị 401 thì cũng gọi notifySessionExpired()
```

Nguồn code: `AuthInterceptor`, `AuthAuthenticator`, `AuthManager`, `MainActivity`, `backend/app/core/security.py` (`verify_jwt`).

## 2. Thêm giao dịch và đồng bộ

### 2.1 Thêm giao dịch (lưu cục bộ trước)

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant AF as AddEditTransactionFragment
    participant VM as AddEditTransactionViewModel
    participant UC as AddTransactionUseCase
    participant TR as TransactionRepositoryImpl
    participant DB as Room (TransactionDao)
    participant SS as SyncScheduler (WorkManager)
    participant BT as CheckBudgetThresholdUseCase

    U->>AF: Nhập số tiền, danh mục, ngày, ghi chú, bấm Lưu
    AF->>VM: save()
    VM->>VM: Tạo TransactionEntity (id = UUID, userId lấy từ JWT, syncStatus = pending, updatedAt = now)
    VM-->>AF: saveState = Loading
    VM->>UC: execute(entity, callback)
    UC->>UC: FormValidator.validateTransaction(amount, note, occurredAt)
    alt Dữ liệu không hợp lệ
        UC-->>VM: Result.Error
        VM-->>AF: saveState = Error (hiện thông báo lỗi)
    else Dữ liệu hợp lệ
        UC->>TR: insert(entity, callback)
        TR->>DB: transactionDao.insert(entity) (luồng diskIO)
        TR->>SS: requestSync()
        Note right of SS: Work duy nhất (REPLACE), trễ 3 giây, cần có mạng
        TR-->>UC: Result.Success
        UC-->>VM: Result.Success
        VM-->>AF: saveState = Success
        AF-->>U: Snackbar "Đã lưu giao dịch"
        UC->>BT: checkAfterAdd(entity) (chỉ với giao dịch chi tiêu)
        Note right of BT: Kiểm tra ngưỡng ngân sách, thông báo cục bộ nếu vượt ngưỡng
    end
    Note over DB,SS: Giao dịch đã nằm trong Room với trạng thái pending nên hiện ngay trong danh sách, kể cả khi đang offline. Phần đồng bộ lên máy chủ xem mục 2.2
```

Nguồn code: `AddEditTransactionFragment`, `AddEditTransactionViewModel`, `AddTransactionUseCase`, `FormValidator`, `TransactionRepositoryImpl`, `SyncScheduler`, `CheckBudgetThresholdUseCase`. Bước kiểm tra chi tiêu bất thường (AI) sau khi lưu không thuộc luồng này nên không vẽ.

### 2.2 Đồng bộ lên máy chủ (đẩy rồi kéo)

```mermaid
sequenceDiagram
    autonumber
    participant SS as SyncScheduler (WorkManager)
    participant SW as SyncWorker
    participant DB as Room
    participant BE as Backend FastAPI (/api/v1/sync)
    participant PG as Postgres (Supabase)

    SS->>SW: doWork() sau 3 giây khi có mạng (hoặc định kỳ mỗi 15 phút)
    SW->>SW: syncLock.lock(), lấy userId từ token
    opt Lần gọi /health gần nhất đã quá 10 phút
        SW->>BE: GET /api/v1/health (warm-up)
    end
    Note over SW,PG: Bước 1 - ĐẨY cả 5 bảng theo thứ tự: categories, transactions, budgets, goals, corrections
    loop Mỗi bảng
        SW->>DB: getPendingSync(userId)
        loop Mỗi lô tối đa 50 bản ghi pending
            SW->>BE: POST /api/v1/sync/{bảng}/push {records}
            BE->>BE: verify_jwt, user_id lấy từ JWT
            loop Mỗi bản ghi trong lô
                BE->>PG: INSERT ... ON CONFLICT (id) DO UPDATE ... WHERE cùng user_id AND updated_at cũ < updated_at mới
            end
            BE-->>SW: 200 {upserted_ids, rejected}
            SW->>DB: markSynced(id, updatedAt) cho các id trong upserted_ids (chỉ khi updated_at chưa đổi)
        end
    end
    Note over SW,PG: Bước 2 - KÉO cả 5 bảng theo cùng thứ tự
    loop Mỗi bảng
        loop Đến khi has_more = false
            SW->>BE: GET /api/v1/sync/{bảng}/pull?since=con_trỏ&limit=500
            BE->>PG: SELECT ... WHERE user_id AND updated_at >= since ORDER BY updated_at, id LIMIT 500
            BE-->>SW: 200 {records, has_more}
            SW->>DB: Trong 1 transaction Room: thêm bản ghi mới, ghi đè nếu updated_at lớn hơn, lưu con trỏ = max(updated_at)
        end
    end
    SW->>SW: checkBudgetThresholdAfterPull và deletedCategoryNotice.checkAfterPull
    alt Đồng bộ xong
        SW->>SW: clearLastError(), Result.success()
    else Máy chủ trả 5xx, 429 hoặc 408, hoặc mất mạng
        SW->>SW: Lưu lastError, Result.retry() (WorkManager thử lại sau)
    else Máy chủ trả mã lỗi HTTP khác hoặc lỗi không xác định
        SW->>SW: Lưu lastError, Result.failure()
    end
```

Nguồn code: `SyncScheduler`, `SyncWorker`, `SyncPusher`, `SyncPuller`, `SyncTables`, `backend/app/routers/sync.py`, `backend/app/services/sync_service.py`.

## 3. Phân loại, sửa gợi ý và huấn luyện lại

### 3.1 Gợi ý danh mục khi gõ ghi chú

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant AF as AddEditTransactionFragment
    participant VM as AddEditTransactionViewModel
    participant AR as AiRepositoryImpl
    participant BE as Backend FastAPI (/api/v1/categorize)
    participant PG as Postgres (Supabase)
    participant DB as Room (CategoryDao)

    U->>AF: Gõ ghi chú giao dịch
    AF->>VM: setNote(note)
    VM->>VM: scheduleCategorize: huỷ lượt chờ và request cũ
    alt Ghi chú rỗng
        VM-->>AF: UiState.Empty (ẩn thẻ gợi ý)
    else Đang mất mạng
        VM-->>AF: UiState.Empty (ẩn thẻ gợi ý, không gọi AI)
    else Có ghi chú và có mạng
        VM->>VM: Chờ 500 ms không gõ thêm (debounce)
        VM-->>AF: UiState.Loading
        VM->>AR: categorize(note)
        AR->>BE: POST /api/v1/categorize {note}
        BE->>BE: verify_jwt, user_id lấy từ JWT
        BE->>PG: load_model_params(user_id)
        opt Chưa có model riêng hoặc training_sample_count nhỏ hơn 10
            BE->>PG: load_model_params(seed) để dùng model seed
        end
        BE->>BE: NaiveBayesClassifier.predict(note) trả về tên danh mục và confidence
        BE->>PG: SELECT id FROM categories WHERE chưa xoá AND (hệ thống hoặc của user) AND name khớp
        alt Gọi thành công
            BE-->>AR: 200 {category_id, confidence}
            AR->>DB: categoryDao.getById(category_id)
            AR-->>VM: Result.Success(CategorySuggestion)
        else Lỗi máy chủ hoặc mất mạng
            BE-->>AR: Mã lỗi HTTP hoặc IOException
            AR-->>VM: Result.Error
        end
        alt Ghi chú đã đổi trong lúc chờ phản hồi
            VM->>VM: Bỏ kết quả cũ
        else Kết quả là lỗi
            VM-->>AF: UiState.Error "Không lấy được gợi ý, nhập tay"
        else confidence nhỏ hơn 0.4
            VM-->>AF: Success: nhãn "Độ tin cậy thấp" kèm phần trăm, không tự điền danh mục
        else confidence từ 0.4 trở lên
            VM-->>AF: Success: thẻ "Tên danh mục" kèm phần trăm
        end
    end
    opt Thẻ gợi ý có độ tin cậy đủ
        U->>AF: Bấm thẻ gợi ý
        AF->>VM: applySuggestedCategory() (điền danh mục nếu cùng loại thu/chi)
    end
```

Nguồn code: `AddEditTransactionViewModel` (`setNote`, `scheduleCategorize`, `requestCategorize`), `AiRepositoryImpl.categorize`, `backend/app/routers/categorize.py`, `backend/app/services/ai/categorize_service.py`.

### 3.2 Sửa gợi ý (correction)

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant AF as AddEditTransactionFragment
    participant VM as AddEditTransactionViewModel
    participant AR as AiRepositoryImpl
    participant BE as Backend FastAPI (/api/v1/correction)
    participant PG as Postgres (Supabase)

    U->>AF: Chọn danh mục khác với gợi ý của AI, bấm Lưu
    AF->>VM: save()
    VM->>VM: Chụp lại suggestedCategoryId (danh mục AI gợi ý) trước khi lưu
    VM->>VM: Lưu giao dịch vào Room (thêm mới như mục 2.1, hoặc cập nhật nếu đang sửa)
    Note over VM: Chỉ khi lưu cục bộ thành công mới chạy tiếp
    VM->>VM: reportCorrectionIfNeeded(transactionId, predicted, corrected)
    alt Không có gợi ý AI hoặc danh mục cuối trùng gợi ý
        VM->>VM: Không gửi correction
    else Danh mục cuối khác gợi ý
        VM->>AR: submitCorrection(transactionId, predicted, corrected)
        AR->>BE: POST /api/v1/correction {transaction_id, predicted_category_id, corrected_category_id}
        BE->>BE: verify_jwt, user_id lấy từ JWT
        BE->>PG: SELECT user_id FROM transactions WHERE id AND is_deleted = false
        alt Giao dịch đã có trên Postgres và thuộc về user
            BE->>PG: INSERT INTO corrections
            BE-->>AR: 201 {id}
            AR->>AR: Tăng số correction mới (SharedPreferences) cho huy hiệu ở Cài đặt
        else Giao dịch chưa có trên Postgres hoặc không thuộc user
            BE-->>AR: 403
            AR->>AR: ErrorMapper.mapHttpCode(403)
        end
        AR-->>VM: Result
        Note over VM: ViewModel bỏ qua kết quả (best-effort), không ảnh hưởng giao dịch đã lưu
    end
    Note over AR,PG: Giao dịch vừa tạo mới chỉ lên Postgres sau khi SyncWorker đẩy xong (mục 2.2), nên request correction gửi trước thời điểm đó rơi vào nhánh 403
```

Nguồn code: `AddEditTransactionViewModel` (`save`, `reportCorrectionIfNeeded`), `AiRepositoryImpl.submitCorrection`, `backend/app/routers/correction.py`, `backend/app/services/ai/correction_service.py`.

### 3.3 Huấn luyện lại mô hình

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant SF as SettingsFragment
    participant VM as SettingsViewModel
    participant AR as AiRepositoryImpl
    participant BE as Backend FastAPI (/api/v1/retrain)
    participant PG as Postgres (Supabase)

    U->>SF: Bấm dòng "Cải thiện gợi ý AI"
    SF->>VM: retrain()
    VM-->>SF: retrainState = Loading (khoá dòng, hiện ProgressBar)
    VM->>AR: retrain(callback)
    AR->>BE: POST /api/v1/retrain
    BE->>BE: verify_jwt, user_id lấy từ JWT
    BE->>PG: check_retrain_cooldown: đọc trained_at trong ai_model_params
    alt Chưa hết thời gian chờ (mặc định 3600 giây) kể từ lần huấn luyện trước
        BE-->>AR: 429 "Vui lòng đợi thêm khoảng N phút trước khi huấn luyện lại"
    else Hết thời gian chờ
        BE->>PG: Giành khoá: ai_model_params.is_training = true (chỉ khi đang false)
        alt Đang có lượt huấn luyện khác
            BE-->>AR: 409 "Đang huấn luyện mô hình, vui lòng thử lại sau"
        else Giành được khoá
            BE->>PG: SELECT note, tên danh mục FROM transactions JOIN categories (cả hai chưa xoá)
            alt Không có dòng nào
                BE->>PG: Xoá hàng ai_model_params tạm
                BE-->>AR: 400 "Chưa có giao dịch nào có danh mục trên máy chủ..."
            else Có dữ liệu
                BE->>BE: TfidfVectorizer.fit và NaiveBayesClassifier.fit
                BE->>PG: COUNT corrections chưa xoá, làm training_sample_count
                BE->>PG: save_model_params (lưu tham số model, trained_at)
                BE->>PG: load_model_params
                BE-->>AR: 200 {trained_at, training_sample_count}
            end
        end
    end
    alt Thành công
        AR->>AR: Đặt lại số correction mới về 0
        AR-->>VM: Result.Success
        VM-->>SF: retrainState = Success
        SF-->>U: Snackbar "Đã cập nhật mô hình gợi ý AI", ẩn huy hiệu số mới
    else Lỗi
        AR->>AR: ErrorMapper.mapHttpCode(code, body)
        AR-->>VM: Result.Error(message)
        VM-->>SF: retrainState = Error
        SF-->>U: Snackbar hiện message lỗi
    end
```

Nguồn code: `SettingsFragment`, `SettingsViewModel.retrain`, `AiRepositoryImpl.retrain`, `backend/app/routers/retrain.py`, `backend/app/core/rate_limit.py`, `backend/app/services/ai/retrain_service.py`.