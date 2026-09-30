# Sync Payload Schema — ĐÃ CHỐT (Ngày 27, Exit Sprint 2)

Hợp đồng dữ liệu cho `POST /api/v1/sync/{table}/push` và `GET /api/v1/sync/{table}/pull`, áp dụng cho 5 bảng: `transactions`, `categories`, `budgets`, `goals`, `corrections`.

- Nguồn: Master Plan Mục 15.1 (payload), 14.2 (Room), 14.3 (Postgres); code nháp `backend/app/models/sync.py`; SQL `backend/sql/01_core_sync_tables.sql`.
- Xác nhận bởi: Nguyễn Phi Long, Nguyễn Đại Vương (Long approve pull request chứa file này).
- Từ sau Ngày 27, đổi tên trường hoặc kiểu dữ liệu là thay đổi phá vỡ tương thích và chỉ được làm khi cả hai đồng ý qua review pull request.

## 1. Quy ước chung

1. Toàn bộ trường dùng `snake_case`, trùng tên cột Room và Postgres.
2. `user_id` KHÔNG có trong body của `push`. Server luôn lấy `user_id` từ JWT (claim `sub`); nếu client vẫn gửi thì server bỏ qua.
3. `sync_status` chỉ tồn tại ở Room, không có ở Postgres và không nằm trong payload.
4. `updated_at` và mọi mốc thời gian (`occurred_at`, `deadline`, `created_at`) là số nguyên, epoch milliseconds. `is_deleted` là boolean. `updated_at` và `is_deleted` bắt buộc trong mọi bản ghi push và pull.
5. `id` là chuỗi duy nhất, không bắt buộc là UUID (category hệ thống dùng id cố định dạng `sys_*`).
6. Số tiền là số nguyên VNĐ.

## 2. Trường của từng bảng

Mọi bảng đều có: `id` (string, bắt buộc), `updated_at` (integer, bắt buộc), `is_deleted` (boolean, bắt buộc). Ngoài ra:

**`transactions`**

| Trường | Kiểu JSON | Bắt buộc | Ghi chú |
|---|---|---|---|
| `amount` | integer | có | VNĐ |
| `type` | string | không | `income` hoặc `expense`, mặc định `expense` |
| `category_id` | string hoặc null | không | mặc định null |
| `note` | string hoặc null | không | mặc định chuỗi rỗng |
| `payment_method` | string hoặc null | không | `cash`, `bank_transfer`, `e_wallet`, `credit_card`, `other`; mặc định null |
| `occurred_at` | integer | có | epoch ms |
| `is_recurring` | boolean | không | mặc định false |
| `recurrence_rule` | string hoặc null | không | chỉ dạng `MONTHLY:<ngày>` |
| `recurrence_parent_id` | string hoặc null | không | |

**`categories`**

| Trường | Kiểu JSON | Bắt buộc | Ghi chú |
|---|---|---|---|
| `name` | string | có | |
| `type` | string | không | `income` hoặc `expense`, mặc định `expense` |

**`budgets`**

| Trường | Kiểu JSON | Bắt buộc | Ghi chú |
|---|---|---|---|
| `period` | string | có | dạng `YYYY-MM` |
| `category_id` | string hoặc null | không | null là ngân sách tổng |
| `limit_amount` | integer | có | VNĐ |
| `threshold_percent` | integer | không | từ 50 đến 100, mặc định 80 |

**`goals`** (không có trường `status`)

| Trường | Kiểu JSON | Bắt buộc | Ghi chú |
|---|---|---|---|
| `name` | string | có | |
| `target_amount` | integer | có | VNĐ |
| `current_amount` | integer | không | mặc định 0 |
| `deadline` | integer | có | epoch ms |

**`corrections`**

| Trường | Kiểu JSON | Bắt buộc | Ghi chú |
|---|---|---|---|
| `transaction_id` | string | có | |
| `predicted_category_id` | string hoặc null | không | |
| `corrected_category_id` | string | có | |
| `created_at` | integer | có | epoch ms, không đổi sau khi tạo |

## 3. Push

`POST /api/v1/sync/{table}/push`

Request:

```json
{
  "records": [
    { "id": "uuid-string", "updated_at": 1735500000000, "is_deleted": false, "...trường riêng của bảng...": "..." }
  ]
}
```

Response:

```json
{
  "upserted_ids": ["uuid-1", "uuid-2"],
  "rejected": [ { "id": "uuid-3", "reason": "forbidden" } ]
}
```

- Server bỏ qua từng record không hợp lệ (đưa vào `rejected`), không làm hỏng cả batch; các record hợp lệ khác vẫn được upsert.
- `reason` chỉ là `forbidden` hoặc `invalid_data`. Không trả chi tiết hơn (ví dụ không phân biệt "không tồn tại" với "thuộc user khác").

## 4. Pull

`GET /api/v1/sync/{table}/pull?since=<epoch_ms>&limit=<số nguyên, mặc định 500>`

```json
{
  "records": [ { "id": "...", "updated_at": 0, "is_deleted": false, "...": "..." } ],
  "has_more": true
}
```

- `since` so sánh `>=`. Server luôn trả cả bản ghi `is_deleted = true` (tombstone).
- `has_more` do server tự tính: `true` nếu còn bản ghi có `updated_at` lớn hơn `updated_at` lớn nhất của batch này. Client dùng `has_more` để quyết định gọi lại, không tự suy ra từ `len(records) == limit`.
- Bản ghi pull có cùng tập trường như bản ghi push (không có `user_id`, không có `sync_status`).

## 5. Quy tắc riêng cho `categories`

- `pull` bảng `categories` chỉ trả category do chính user tạo (`user_id` bằng `sub` của JWT), kể cả tombstone; không trả category hệ thống. Category hệ thống đã được seed sẵn ở Room và ở Postgres với cùng id `sys_*` nên không đi qua sync. Vì vậy mọi bản ghi `categories` nhận từ `pull` thuộc user hiện tại và client ghi vào Room với `user_id` là user hiện tại.
- `push` một category hệ thống, hoặc category thuộc user khác, bị đưa vào `rejected` với `reason = forbidden`.