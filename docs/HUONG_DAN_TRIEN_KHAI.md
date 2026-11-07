# Hướng dẫn triển khai — Plix

**Người thực hiện:** Nguyễn Phi Long
**Nguồn đối chiếu:** `backend/` (`app/core/config.py`, `app/core/security.py`, `app/main.py`, `sql/`, `app/services/ai/train_seed_model.py`), `android/app/build.gradle.kts`, `android/app/src/main/java/com/longvuong/plix/data/local/AppDatabase.java`.

Tài liệu này hướng dẫn đưa Plix chạy trên môi trường thật: Supabase (Auth và Postgres), Backend trên Render (gói miễn phí) và ứng dụng Android trên điện thoại. Cách dựng môi trường phát triển trên máy cá nhân nằm ở `HUONG_DAN_CAI_DAT.md`. Kiến trúc tổng thể xem `SO_DO_KIEN_TRUC.md`.

## 1. Tổng quan

Thứ tự triển khai: **Supabase → Render → Android**.

| Thành phần | Chạy ở đâu | Cấu hình nằm ở đâu |
|---|---|---|
| Auth và Postgres | Supabase | Dashboard Supabase |
| Backend FastAPI + AI | Render (Web Service, gói Free) | Mục **Environment** của dịch vụ trên Render |
| Ứng dụng Android | Điện thoại | `android/app/build.gradle.kts` (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `BACKEND_BASE_URL`) |

Quy tắc giữ bí mật:

- Mật khẩu cơ sở dữ liệu và `GEMINI_API_KEY` chỉ đặt trong mục Environment của Render (hoặc `backend/.env` khi chạy trên máy), **không commit lên kho mã nguồn**.
- Android chỉ chứa khoá công khai (publishable/anon) của Supabase. Không đưa khoá bí mật (secret/service role) vào ứng dụng.

## 2. Supabase

### 2.1. Tạo project

1. Vào https://supabase.com, đăng nhập, chọn **New project**.
2. Đặt tên project, đặt **Database password** (nên chỉ gồm chữ và số để khỏi phải mã hoá ký tự đặc biệt khi đưa vào chuỗi kết nối), chọn vùng máy chủ, dùng gói Free.
3. Lưu mật khẩu ở nơi an toàn, chờ project tạo xong.

### 2.2. Lấy 3 giá trị cấu hình

| Giá trị | Lấy ở đâu | Dùng cho |
|---|---|---|
| **Project URL** (dạng `https://<project-ref>.supabase.co`) | Project Settings → mục API / Data API | Backend: `SUPABASE_URL` (**không** có dấu `/` ở cuối). Android: `SUPABASE_URL` (**có** dấu `/` ở cuối) |
| **Publishable key** (khoá công khai, còn gọi anon key) | Project Settings → mục API Keys | Android: `SUPABASE_ANON_KEY` |
| **Chuỗi kết nối Session pooler** | Nút **Connect** ở đầu trang project → chọn **Session pooler** → copy URI (cổng 5432) | Backend: `DATABASE_URL`. Thay `[YOUR-PASSWORD]` bằng mật khẩu ở bước 2.1 |

Chuỗi kết nối có dạng như mẫu trong `backend/.env.example`:

```
postgresql://postgres.<project-ref>:<mat-khau>@<pooler-host>:5432/postgres
```

Tên các mục trên giao diện Supabase có thể khác đôi chút tuỳ phiên bản giao diện.

### 2.3. Cấu hình Auth

Vào **Authentication** → phần cấu hình nhà cung cấp **Email**:

- Email phải được bật.
- Tắt **Confirm email**, rồi lưu. Lý do: khi đăng ký, ứng dụng lưu luôn `access_token` và `refresh_token` nhận được từ Supabase (`AuthManager`); nếu bật xác nhận email, Supabase không trả token lúc đăng ký và người dùng không vào được app.

### 2.4. Kiểm tra khoá ký JWT

Backend chỉ chấp nhận JWT ký bằng thuật toán **ES256**, lấy khoá công khai từ địa chỉ JWKS của Supabase (`app/core/security.py`). Mở trong trình duyệt:

```
<Project URL>/auth/v1/.well-known/jwks.json
```

Kết quả phải có khoá chứa `"alg":"ES256"`. Nếu không có, project đang dùng khoá ký cũ; vào phần **JWT Keys** (hoặc JWT Signing Keys) trong Project Settings để chuyển sang khoá ký bất đối xứng ES256 (ECC P-256), rồi mở lại địa chỉ trên để kiểm tra.

### 2.5. Tạo bảng

Vào **SQL Editor** → **New query**. Với từng file dưới đây, **theo đúng thứ tự**: mở file trong thư mục `backend/sql/`, copy toàn bộ nội dung, dán vào SQL Editor, bấm **Run** và chờ báo thành công.

1. `01_core_sync_tables.sql` — 5 bảng đồng bộ: `transactions`, `categories`, `budgets`, `goals`, `corrections`
2. `02_ai_tables.sql` — 2 bảng AI: `ai_model_params`, `anomaly_stats`
3. `03_rls.sql` — bật Row Level Security và tạo policy cho cả 7 bảng

Mỗi file chỉ chạy **1 lần** (các lệnh `CREATE TABLE` không có `IF NOT EXISTS`, chạy lại sẽ báo lỗi bảng đã tồn tại). Xong, vào **Table Editor** phải thấy đủ 7 bảng.

### 2.6. Nạp 12 danh mục hệ thống

Backend tra danh mục hệ thống (dòng có `user_id` rỗng) ở bảng `categories` khi phân loại giao dịch, phát hiện bất thường, phân tích câu nhập nhanh và trả danh sách danh mục. Các file SQL ở mục 2.5 không nạp sẵn chúng, nên cần chạy thêm 1 lần trong **SQL Editor**. 12 dòng này trùng với 12 danh mục nạp sẵn trong `AppDatabase.java` của app (cùng `id`, tên, loại):

```sql
INSERT INTO categories (id, user_id, name, type, updated_at, is_deleted) VALUES
  ('sys_an_uong',       NULL, 'Ăn uống',                      'expense', 0, false),
  ('sys_di_chuyen',     NULL, 'Di chuyển',                    'expense', 0, false),
  ('sys_nhu_yeu_pham',  NULL, 'Nhu yếu phẩm',                 'expense', 0, false),
  ('sys_giai_tri',      NULL, 'Giải trí',                     'expense', 0, false),
  ('sys_hoa_don',       NULL, 'Hoá đơn (điện/nước/internet)', 'expense', 0, false),
  ('sys_suc_khoe',      NULL, 'Sức khoẻ',                     'expense', 0, false),
  ('sys_giao_duc',      NULL, 'Giáo dục',                     'expense', 0, false),
  ('sys_nha_o',         NULL, 'Nhà ở/Thuê nhà',               'expense', 0, false),
  ('sys_mua_sam',       NULL, 'Mua sắm',                      'expense', 0, false),
  ('sys_khac_chi',      NULL, 'Khác',                         'expense', 0, false),
  ('sys_luong',         NULL, 'Lương',                        'income',  0, false),
  ('sys_thu_nhap_khac', NULL, 'Thu nhập khác',                'income',  0, false)
ON CONFLICT (id) DO NOTHING;
```

Kiểm tra: chạy `SELECT count(*) FROM categories WHERE user_id IS NULL;` — kết quả phải là **12**.

### 2.7. Huấn luyện model phân loại mặc định

Người dùng chưa có đủ 10 mẫu sửa gợi ý thì `/categorize` dùng model mặc định lưu ở bảng `ai_model_params` với `user_id = '__seed__'`. Model này huấn luyện từ `backend/data/seed_categorization.csv` (600 dòng). Làm 1 lần, **trên máy bạn**, sau khi đã có môi trường ảo và file `backend/.env` trỏ tới project Supabase này (`HUONG_DAN_CAI_DAT.md`, mục 3.2 và 3.3). Trong thư mục `backend`, với môi trường ảo đang bật:

```
python -m app.services.ai.train_seed_model
```

Kết quả đúng: dòng cuối in `KET QUA REGRESSION: PASS`. Kiểm tra: bảng `ai_model_params` có 1 dòng `user_id = __seed__`.

## 3. Render (Backend)

### 3.1. Điều kiện

- Mã nguồn đã nằm trên GitHub, có thư mục `backend` ở nhánh dùng để triển khai (bản nộp: nhánh `main`).
- Có tài khoản Render đã liên kết với GitHub.
- Supabase đã xong mục 2.

### 3.2. Tạo Web Service

Trên https://dashboard.render.com chọn **New +** → **Web Service** → chọn kho mã nguồn của dự án, rồi điền:

| Mục | Giá trị |
|---|---|
| Name | Tên dịch vụ tuỳ ý |
| Language | `Python 3` |
| Branch | Nhánh dùng để triển khai (bản nộp: `main`) |
| Root Directory | `backend` |
| Build Command | `pip install -r requirements.txt` |
| Start Command | `uvicorn app.main:app --host 0.0.0.0 --port $PORT` |
| Instance Type | `Free` |

`Root Directory` là `backend` vì `requirements.txt` và thư mục `app/` nằm trong `backend/`, và mã nguồn import theo dạng `app.core...`.

### 3.3. Biến môi trường (mục Environment)

| Biến | Bắt buộc | Giá trị |
|---|---|---|
| `SUPABASE_URL` | Có | Project URL ở mục 2.2, **không** có `/` ở cuối |
| `DATABASE_URL` | Có | Chuỗi Session pooler ở mục 2.2, đã thay mật khẩu |
| `GEMINI_API_KEY` | Không | Khoá lấy từ Google AI Studio. Chỉ đặt ở đây, không đưa vào mã nguồn hay ứng dụng |
| `RETRAIN_COOLDOWN_SECONDS` | Không | Bỏ trống thì dùng mặc định 3600 |
| `MAX_PULL_LIMIT` | Không | Bỏ trống thì dùng mặc định 500 |
| `EXPORT_COOLDOWN_SECONDS` | Không | Bỏ trống thì dùng mặc định 60 |

Không cần đặt `SUPABASE_SERVICE_KEY` hay `SUPABASE_JWT_SECRET`: Backend xác minh JWT bằng khoá công khai lấy từ JWKS và không đọc 2 biến này (`app/core/config.py`).

### 3.4. Triển khai

Bấm **Create Web Service** và theo dõi mục **Logs**. Triển khai thành công khi log hiện dòng `Application startup complete`. Nếu log báo "Thiếu biến môi trường ...", bổ sung biến còn thiếu ở mục 3.3.

### 3.5. Kiểm tra

Mở địa chỉ công khai Render hiển thị ở đầu trang dịch vụ, nối thêm `/api/v1/health`:

```
https://<địa-chỉ-Render>/api/v1/health
```

Kết quả đúng: `{"status":"OK","database":"connected"}`.

### 3.6. Làm nóng máy chủ trước khi dùng

Gói Free của Render đưa dịch vụ vào trạng thái ngủ khi lâu không có yêu cầu; theo ghi nhận trong kế hoạch dự án, yêu cầu đầu tiên sau đó có thể mất khoảng 30–60 giây. Vì vậy, trước mỗi buổi demo hoặc nộp bài: mở địa chỉ ở mục 3.5 trong trình duyệt, chờ có kết quả, rồi mới mở ứng dụng. Khi đồng bộ, ứng dụng cũng tự gọi `/api/v1/health` để làm nóng nếu lần gọi gần nhất đã lâu (`SyncWorker`).

### 3.7. Cập nhật Backend

Render tự triển khai lại khi có commit mới ở nhánh đã chọn (nếu mục **Auto-Deploy** trong Settings của dịch vụ đang bật).

## 4. Android

### 4.1. Trỏ ứng dụng tới hệ thống của bạn

Bỏ qua mục này nếu dùng Supabase và Render sẵn có của nhóm. Nếu dùng Supabase và Render riêng, sửa trong `android/app/build.gradle.kts`:

| Hằng số | Sửa thành | Vị trí trong file |
|---|---|---|
| `SUPABASE_URL` | Project URL ở mục 2.2, **có** `/` ở cuối | `defaultConfig` |
| `SUPABASE_ANON_KEY` | Publishable key ở mục 2.2 | `defaultConfig` |
| `BACKEND_BASE_URL` | Địa chỉ Render ở mục 3.5, bắt đầu `https://`, **có** `/` ở cuối | Khối `debug` **và** khối `release` (sửa cả hai) |

Sau khi sửa, bấm **Sync Now** trong Android Studio.

### 4.2. Build bản debug

Mở terminal trong thư mục `android`:

```
gradlew.bat assembleDebug
```

Khi báo `BUILD SUCCESSFUL`, file APK nằm tại:

```
android\app\build\outputs\apk\debug\app-debug.apk
```

### 4.3. Cài lên điện thoại

Cách 1 — dùng `adb` (cáp USB, đã bật **Gỡ lỗi USB** trên điện thoại; `adb` nằm trong thư mục `platform-tools` của Android SDK):

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

(chạy trong thư mục `android`)

Cách 2 — chép file `app-debug.apk` sang điện thoại, mở file đó và cho phép cài ứng dụng từ nguồn này khi Android hỏi.

Điện thoại cần Android 8.0 (API 26) trở lên.

### 4.4. Bản release

Bản `release` đã bật thu gọn mã bằng R8 (`isMinifyEnabled = true`) và được ký bằng khoá riêng của nhóm. Khoá không nằm trong kho mã nguồn.

**Tạo khoá ký (làm 1 lần):**

1. Android Studio → **Build → Generate Signed App Bundle / APK… → APK → Create new…**, tạo file `.jks` ở thư mục **ngoài** repo (ví dụ `D:\Plix-secrets\plix-release.jks`), alias `plix`, hiệu lực 25 năm. Bấm **OK** rồi **Cancel** hộp thoại ký.
2. Sao lưu file `.jks` và mật khẩu ở nơi an toàn. Mất khoá thì không thể cập nhật ứng dụng cùng chữ ký.
3. Tạo file `android/keystore.properties` (đã nằm trong `.gitignore`, **không** commit):

```
storeFile=D:/Plix-secrets/plix-release.jks
storePassword=<mật-khẩu-kho-khoá>
keyAlias=plix
keyPassword=<mật-khẩu-khoá>
```

Dùng dấu `/` trong đường dẫn. Máy không có `keystore.properties` vẫn build được `release` nhưng ra file chưa ký (`app-release-unsigned.apk`), không cài được.

**Build:** trong thư mục `android`:

```
gradlew.bat assembleRelease
```

File APK đã ký nằm tại:

```
android\app\build\outputs\apk\release\app-release.apk
```

**Kiểm tra chữ ký** (công cụ `apksigner` nằm trong thư mục `build-tools` của Android SDK):

```
apksigner.bat verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

**Cài lên điện thoại:** nếu máy đang có bản `debug`, gỡ trước vì hai bản dùng chữ ký khác nhau:

```
adb uninstall com.longvuong.plix
adb install app\build\outputs\apk\release\app-release.apk
```

Hoặc chép file `app-release.apk` sang điện thoại và mở file đó.

## 5. Kiểm tra sau triển khai

Thực hiện lần lượt, mỗi bước phải đạt rồi mới sang bước sau:

| Bước | Thao tác | Kết quả đúng |
|---|---|---|
| 1 | Mở `https://<địa-chỉ-Render>/api/v1/health` | `{"status":"OK","database":"connected"}` |
| 2 | Mở app, đăng ký 1 tài khoản mới | Vào được màn hình chính (chứng tỏ đã tắt Confirm email) |
| 3 | Thêm 1 giao dịch (ví dụ 45.000, ghi chú "cà phê") | Giao dịch hiện trong danh sách |
| 4 | Chờ đồng bộ xong (biểu tượng đồng bộ ngừng quay), mở bảng `transactions` trong Table Editor của Supabase | Có dòng giao dịch vừa thêm |
| 5 | Ở màn thêm giao dịch, nhập ghi chú "cà phê" và chờ vài giây (lần đầu sau khi Render ngủ có thể lâu hơn) | Hiện thẻ gợi ý danh mục "Ăn uống" |
| 6 | Mở tab Thống kê | Màn hình hiển thị bình thường |