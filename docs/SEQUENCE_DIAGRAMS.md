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