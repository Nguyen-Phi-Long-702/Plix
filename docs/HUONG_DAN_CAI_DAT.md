# Hướng dẫn cài đặt — Plix

**Người thực hiện:** Nguyễn Phi Long
**Nguồn đối chiếu:** mã nguồn trong `android/` và `backend/`.

Tài liệu này hướng dẫn dựng môi trường phát triển để chạy và kiểm thử Plix trên máy cá nhân (Windows). Việc đưa hệ thống lên Supabase, Render và cài bản APK nằm ở `HUONG_DAN_TRIEN_KHAI.md`. Kiến trúc tổng thể xem `SO_DO_KIEN_TRUC.md`.

## 1. Yêu cầu

| Thành phần | Yêu cầu | Ghi chú |
|---|---|---|
| Git | Bản mới | Lấy mã nguồn |
| Android Studio | Đã dùng khi phát triển: `Android Studio Panda 2` | Dự án đặt Gradle JDK là `jbr-21` (JDK đi kèm Android Studio), mã nguồn biên dịch ở mức Java 17 |
| Android SDK | Platform API 36 (mã nguồn khai báo `compileSdk` 36, `minorApiLevel` 1) | Cài trong **SDK Manager** của Android Studio; thiếu thì Android Studio gợi ý cài khi Gradle Sync |
| Thiết bị chạy app | Android 8.0 (API 26) trở lên | `minSdk = 26`; dùng máy ảo (AVD) hoặc điện thoại thật |
| Python | Đã dùng khi phát triển: `python 3.14.7` | Chạy Backend |
| Tài khoản Supabase | Gói miễn phí | Auth + Postgres |
| Kết nối Internet | Bắt buộc | Gradle tải thư viện lần đầu; app gọi Supabase và Backend |

Phiên bản công cụ build đã được khoá trong mã nguồn, không cần tự cài: Gradle 9.3.1 (qua `gradlew`), Android Gradle Plugin 9.1.1 (`android/gradle/libs.versions.toml`).

## 2. Lấy mã nguồn

```
git clone <đường-dẫn-kho-mã-nguồn>
cd <thư-mục-vừa-clone>
```

Cấu trúc thư mục:

| Thư mục | Nội dung |
|---|---|
| `android/` | Dự án Android Studio (Java, MVVM) |
| `backend/` | Backend FastAPI + AI; `sql/` chứa 3 file tạo bảng; `data/` chứa dữ liệu seed; `scripts/` chứa script đo và kiểm tra |
| `docs/` | Tài liệu dự án |

## 3. Cài đặt Backend

Tất cả lệnh trong mục này chạy trong thư mục `backend`.

### 3.1. Chuẩn bị Supabase

Backend cần 1 project Supabase đã tạo bảng, đã nạp danh mục hệ thống và đã huấn luyện model mặc định. Làm theo **mục 2 của `HUONG_DAN_TRIEN_KHAI.md`**: làm các mục 2.1 đến 2.6 (tạo project, lấy 3 giá trị cấu hình, chỉnh Auth, chạy SQL, nạp danh mục) rồi quay lại làm tiếp mục 3.2 và 3.3 dưới đây. Mục 2.7 (huấn luyện model mặc định) cần môi trường ảo và file `.env`, nên làm sau khi xong mục 3.3.

### 3.2. Tạo môi trường ảo và cài thư viện

```
cd backend
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

Thư mục `venv` đã nằm trong `backend/.gitignore`.

### 3.3. Tạo file `.env`

```
copy .env.example .env
```

Mở `backend/.env` và điền giá trị thật. File `.env` đã nằm trong `backend/.gitignore`, **không commit**.

| Biến | Bắt buộc | Ý nghĩa |
|---|---|---|
| `SUPABASE_URL` | Có | Project URL của Supabase, dạng `https://<project-ref>.supabase.co`, **không có dấu `/` ở cuối** (Backend tự nối thêm `/auth/v1/...` để lấy khoá công khai xác minh JWT) |
| `DATABASE_URL` | Có | Chuỗi kết nối Postgres (Session pooler) của Supabase, dạng như trong `.env.example` |
| `RETRAIN_COOLDOWN_SECONDS` | Không | Số giây tối thiểu giữa 2 lần gọi `/retrain` thành công của cùng 1 người dùng. Mặc định 3600 |
| `MAX_PULL_LIMIT` | Không | Số bản ghi tối đa mỗi lần gọi `GET /sync/{table}/pull`. Mặc định 500 |
| `GEMINI_API_KEY` | Không | Khoá Gemini từ Google AI Studio. Thiếu khoá thì nhánh dự phòng Gemini của `/parse-nl` báo "AI đang bận, vui lòng nhập tay"; phần còn lại của `/parse-nl` vẫn chạy |

Thiếu `SUPABASE_URL` hoặc `DATABASE_URL`, Backend dừng ngay khi khởi động với thông báo "Thiếu biến môi trường ... — kiểm tra file .env".

### 3.4. Chạy Backend

```
uvicorn app.main:app --reload
```

Backend chạy tại `http://127.0.0.1:8000`. Mở trình duyệt vào `http://127.0.0.1:8000/api/v1/health`, kết quả đúng:

```
{"status":"OK","database":"connected"}
```

Nếu `database` là `disconnected`: kiểm tra lại `DATABASE_URL` (đúng mật khẩu, đúng chuỗi Session pooler).

### 3.5. Chạy bộ kiểm thử Backend

```
python -m pytest -q
```

Cần có `backend/.env` (hoặc đã đặt biến môi trường), vì `app/core/config.py` đọc biến khi nạp. Kết quả ở lần chạy cuối của Sprint 6 (Ngày 53): 379 test, 0 lỗi (số này thay đổi nếu sau đó có thêm test).

## 4. Cài đặt Android

### 4.1. Mở dự án

Trong Android Studio chọn **File → Open** và mở thư mục **`android`** (không phải thư mục gốc của repo). Chờ Gradle Sync chạy xong. Lần đầu Gradle tự tải Gradle 9.3.1, JDK 21 (nếu máy chưa có) và toàn bộ thư viện nên cần Internet và mất vài phút. Nếu Android Studio báo thiếu Android SDK Platform, bấm theo gợi ý để cài.

File `android/local.properties` (đường dẫn Android SDK) do Android Studio tự tạo, đã nằm trong `.gitignore`.

### 4.2. Cấu hình địa chỉ Backend và Supabase

3 giá trị nằm trong `android/app/build.gradle.kts`:

| Hằng số `BuildConfig` | Ý nghĩa | Quy tắc |
|---|---|---|
| `SUPABASE_URL` | Project URL của Supabase | **Có** dấu `/` ở cuối |
| `SUPABASE_ANON_KEY` | Khoá công khai (publishable/anon) của Supabase | Khoá này công khai theo thiết kế của Supabase; KHÔNG dán khoá bí mật (secret/service role) vào đây |
| `BACKEND_BASE_URL` | Địa chỉ Backend trên Render | Có ở cả build `debug` và `release`; bắt đầu bằng `https://` và **có** dấu `/` ở cuối |

Chạy bằng Backend/Supabase đã có sẵn của nhóm thì không cần sửa gì. Chỉ sửa khi dùng Supabase hoặc Render của riêng bạn (xem `HUONG_DAN_TRIEN_KHAI.md`, mục 4.1). Ứng dụng chỉ gọi Backend qua HTTPS, nên không trỏ được vào Backend chạy trên máy ở `http://127.0.0.1:8000`.

### 4.3. Chạy ứng dụng

1. Chọn thiết bị: máy ảo (**Device Manager → Create Device**, chọn hệ điều hành Android 8.0 trở lên) hoặc điện thoại thật (bật **Tuỳ chọn nhà phát triển → Gỡ lỗi USB**, cắm cáp).
2. Chọn cấu hình chạy **app**, bấm **Run**.
3. Trên màn hình đăng ký tạo tài khoản, đăng nhập, thêm 1 giao dịch.

Lần gọi Backend đầu tiên trên Render có thể chậm hơn nhiều so với các lần sau (xem `HUONG_DAN_TRIEN_KHAI.md`, mục 3.6).

### 4.4. Chạy bộ kiểm thử Android

Mở terminal trong thư mục `android`:

```
gradlew.bat testDebugUnitTest --rerun
gradlew.bat connectedDebugAndroidTest
```

- Lệnh 1 là unit test, chạy trên máy tính, không cần thiết bị. Kết quả ở Ngày 53: 409 test, 0 lỗi.
- Lệnh 2 là test instrumented, **cần 1 máy ảo hoặc điện thoại đang chạy**. Có 29 test ở Ngày 53.
- Hai test hiệu năng `TransactionListPerformanceTest` (ngưỡng dưới 100 ms) phụ thuộc tốc độ thiết bị và tải máy ảo; kết quả các lần chạy ghi trong `BAO_CAO_KIEM_THU.md` và `NGAY53_KET_QUA_KIEM_THU_CUOI.md`.