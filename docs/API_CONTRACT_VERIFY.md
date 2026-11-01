# Báo cáo verify API contract đồng bộ — Ngày 52

**Người thực hiện:** Nguyễn Phi Long, Nguyễn Đại Vương
**Ngày thực hiện:** 01/11/2026
**Nguồn đối chiếu:** `docs/SYNC_PAYLOAD_SCHEMA.md` (đã chốt Ngày 27, Master Plan Mục 15.1). Đây là báo cáo verify code với bản đã chốt, không phải tài liệu contract mới. Số dòng bên dưới theo mã nguồn lúc viết báo cáo.

## 1. Phạm vi và cách làm

Đối chiếu 5 bảng đồng bộ (`transactions`, `categories`, `budgets`, `goals`, `corrections`) giữa 5 nơi:

| Nơi | Vị trí |
|---|---|
| Bản đã chốt | `docs/SYNC_PAYLOAD_SCHEMA.md` |
| Pydantic (backend) | `backend/app/models/sync.py` |
| Postgres | `backend/sql/01_core_sync_tables.sql` |
| DTO (Android) | `android/app/src/main/java/com/longvuong/plix/data/remote/dto/*SyncRecordDto.java` |
| Room (Android) | `android/app/src/main/java/com/longvuong/plix/data/local/entity/*Entity.java` |

Tập trường được so bằng script tạm chạy trên mã nguồn (không đưa vào repo); các quy tắc còn lại được đọc trực tiếp ở đúng dòng ghi trong mục 3.

## 2. Tập trường và kiểu dữ liệu — KHỚP

Số trường tính cả `id`, `updated_at`, `is_deleted`. `user_id` và `sync_status` không nằm trong payload nên không tính (Postgres có `user_id`, Room có `user_id` và `sync_status`).

| Bảng | Số trường | Tài liệu | Pydantic | Postgres | DTO | Room |
|---|---|---|---|---|---|---|
| `transactions` | 12 | khớp | khớp | khớp | khớp | khớp |
| `categories` | 5 | khớp | khớp | khớp | khớp | khớp |
| `budgets` | 7 | khớp | khớp | khớp | khớp | khớp |
| `goals` | 7 | khớp | khớp | khớp | khớp | khớp |
| `corrections` | 7 | khớp | khớp | khớp | khớp | khớp |

Kiểu dữ liệu nhất quán giữa các nơi:

| Loại | Pydantic | Postgres | DTO / Room |
|---|---|---|---|
| Thời gian epoch ms (`updated_at`, `occurred_at`, `deadline`, `created_at`) | `int` | `BIGINT` | `long` |
| Số tiền VNĐ (`amount`, `limit_amount`, `target_amount`, `current_amount`) | `int` | `BIGINT` | `long` |
| `threshold_percent` | `int`, từ 50 đến 100, mặc định 80 | `INTEGER` | `int` |
| `is_deleted`, `is_recurring` | `bool` | `BOOLEAN` | `boolean` |
| `id` và các trường chuỗi | `str` | chuỗi | `String` |

Ngoài tập trường, script còn xác nhận: Android khai báo đủ 10 đường dẫn `api/v1/sync/<bảng>/push|pull` cho 5 bảng, và enum `SyncTable` của backend có đúng 5 bảng.

## 3. Quy tắc của schema đã chốt — KHỚP

| Quy tắc trong `SYNC_PAYLOAD_SCHEMA.md` | Nơi kiểm tra | Kết quả |
|---|---|---|
| Mọi trường `snake_case`, trùng tên cột Room và Postgres (mục 1.1) | Mục 2 ở trên (tên trường giống hệt ở 5 nơi) | Khớp |
| `user_id` không có trong body push; server lấy từ JWT; client gửi thì bỏ qua (mục 1.2) | `models/sync.py` không có trường `user_id`; `routers/sync.py:27-29` truyền `user_id` từ `Depends(verify_jwt)` vào `push_records` | Khớp |
| `sync_status` không nằm trong payload (mục 1.3) | Không có trong Pydantic, Postgres, DTO | Khớp |
| `updated_at`, `is_deleted` bắt buộc ở push và pull (mục 1.4) | `models/sync.py:16-19` (`SyncRecordBase`: không có giá trị mặc định) | Khớp |
| Category hệ thống dùng id `sys_*` (mục 1.5) | `AppDatabase.java` seed các id `sys_*` ở Room | Khớp |
| `type` của giao dịch và danh mục chỉ `income` hoặc `expense`, mặc định `expense` | `models/sync.py:24`, `models/sync.py:38` | Khớp |
| `payment_method` thuộc 5 giá trị `cash`, `bank_transfer`, `e_wallet`, `credit_card`, `other` | `models/sync.py:28`; Android `PaymentMethodUi.java` (khối `static`) có đúng 5 mã này | Khớp |
| `threshold_percent` từ 50 đến 100, mặc định 80 | `models/sync.py:45` (`Field(default=80, ge=50, le=100)`) | Khớp |
| Push trả `upserted_ids` và `rejected` (mục 3) | `models/sync.py:74-76`; Android `SyncPushResponseDto.java:8,11` | Khớp |
| Mỗi phần tử `rejected` có `id` và `reason` | `models/sync.py:69-71`; Android `SyncPushResponseDto.java:15,18` | Khớp |
| `reason` chỉ là `forbidden` hoặc `invalid_data` | `models/sync.py:71` (`Literal["forbidden", "invalid_data"]`); server hiện chỉ trả `forbidden` (`sync_service.py:187,205`) | Khớp (xem mục 5) |
| Record thuộc user khác bị đưa vào `rejected`, không làm hỏng cả batch | `sync_service.py:200-205`: upsert không ghi được thì tra chủ sở hữu, khác `user_id` thì `rejected` | Khớp |
| Push category hệ thống hoặc của user khác bị `rejected` với `forbidden` (mục 5) | `_can_write_category` (`sync_service.py:143-157`) và `sync_service.py:184-188` | Khớp |
| Pull trả `records` và `has_more` (mục 4) | `models/sync.py:79-81`; Android `SyncPullResponseDto.java:9,12` | Khớp |
| `since` so sánh `>=` | `sync_service.py:78` (`updated_at >= $2`) | Khớp |
| Pull luôn trả cả bản ghi `is_deleted = true` (tombstone) | `pull_sql` (`sync_service.py:75-81`) không lọc `is_deleted` | Khớp |
| `limit` mặc định 500, có trần | `routers/sync.py:35,39` (mặc định 500, `min(limit, MAX_PULL_LIMIT)`); `core/config.py:18` (`MAX_PULL_LIMIT` mặc định 500); Android `SyncPuller.java:21` (`PULL_LIMIT = 500`) | Khớp |
| `has_more` do server tính: còn bản ghi có `updated_at` lớn hơn `updated_at` lớn nhất của batch | `has_more_sql` (`sync_service.py:82-87`, điều kiện `updated_at > $2`) và `sync_service.py:230` | Khớp |
| Client dùng `has_more`, không suy ra từ `len(records) == limit` | Android `SyncPuller.java:91` (`if (!body.hasMore)`) | Khớp |
| Pull `categories` chỉ trả category do chính user tạo, không trả category hệ thống (mục 5) | `pull_sql` lọc `WHERE user_id = $1`; category hệ thống có `user_id` rỗng (NULL) nên không khớp | Khớp |

## 4. Test tự động có sẵn liên quan đến sync

Chạy `python -m pytest -q -k sync` (trong `backend`): 110 passed, 0 failed. Các file chính: `test_sync_endpoint.py`, `test_sync_all_tables.py`, `test_sync_service.py`, `test_sync_tombstone_duplicate.py`, `test_sync_categories_write.py`, `test_sync_expired_token.py`. Đây là test đã có từ các Sprint trước, không viết thêm trong Ngày 52.

## 5. Điểm hành vi cần Long và Vương thống nhất (không phải sai lệch tên trường, kiểu hay cấu trúc)

`SYNC_PAYLOAD_SCHEMA.md` mục 3 viết: "Server bỏ qua từng record không hợp lệ (đưa vào `rejected`), không làm hỏng cả batch". Code hiện chỉ đưa vào `rejected` các record bị `forbidden` (thuộc user khác, hoặc category hệ thống). Một record sai kiểu hoặc sai giá trị (ví dụ `type` ngoài `income`/`expense`, `threshold_percent` ngoài 50 đến 100) không đi tới `rejected`: model Pydantic của từng bảng được dùng làm kiểu body (`routers/sync.py:17-27`) nên FastAPI trả 422 cho cả batch. Test hiện có khẳng định hành vi 422 này (`test_sync_endpoint.py:76,170,180`, `test_sync_all_tables.py:150`). Vì vậy giá trị `invalid_data` được khai báo ở `models/sync.py:71` nhưng server chưa bao giờ trả.

Kết luận: 0 sai lệch về tên trường, kiểu dữ liệu, `snake_case`, cấu trúc `rejected` và `has_more`. Hành vi 422 ở trên được ghi vào backlog (mục B1 của `docs/SECURITY_REVIEW.md`) để hai thành viên quyết định giữa việc đổi server cho khớp câu chữ của schema hoặc ghi chú rõ trong schema; không tự ý đổi trong báo cáo này.