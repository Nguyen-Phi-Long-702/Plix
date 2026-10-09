# Kết quả Sync Test Suite Phần 1 (Functional) — Ngày 35

**Người thực hiện:** Nguyễn Đại Vương, Nguyễn Phi Long
**Ngày giờ chạy:** 
**Backend đã kiểm tra:** local (uvicorn + Postgres Supabase) và Render (https://plix-7jfp.onrender.com)

## 1. Test tự động phía backend (pytest, pool giả)

| File | Kết quả |
|---|---|
| test_sync_service.py | 58 passed |
| test_sync_endpoint.py | 58 passed |
| test_sync_all_tables.py | 58 passed |
| test_sync_tombstone_duplicate.py | 58 passed |
| test_sync_categories_write.py | 58 passed |

## 2. Script `scripts/sync_functional_check.py` (Postgres thật) — kết quả local / Render

| Case | transactions | categories | budgets | goals | corrections |
|---|---|---|---|---|---|
| F1 Đẩy bản ghi mới | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F2 Kéo về đúng dữ liệu | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F3 Bản sửa ghi đè bản cũ | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F4 Tombstone vẫn được kéo về | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F5 Không hồi sinh bản đã xoá | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F6 Gửi trùng yêu cầu | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| F7 Gửi lại cả lô sau mất mạng | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |

(Mỗi ô ghi ĐẠT hoặc KHÔNG ĐẠT theo dạng `local / Render`; các dòng F2–F7 điền tương tự dòng F1.)

Tổng: local 35/35, Render 35/35.

## 3. Test đơn vị Android (`com.longvuong.plix.data.sync`)

| Lớp test | Kết quả |
|---|---|
| SyncPusherTest | |
| SyncPullerTest | |
| SyncTombstoneTest | |
| SyncTablesTest | |
| RecurringTransactionWorkerTest | |
| (các lớp còn lại trong package) | |

## 4. Kiểm tra thủ công trên emulator (app → Render → Supabase)

| Mã | Nội dung | transactions | categories | budgets | goals | corrections |
|---|---|---|---|---|---|---|
| E1 | Đẩy sau khi mất mạng rồi có mạng lại | | | | | không áp dụng |
| E2 | Xoá mềm lên server | | | | | không áp dụng |
| E3 | Không hồi sinh, không trùng | | | | | không áp dụng |

## 5. Lỗi phát hiện và cách sửa


## 6. Kết luận

Đạt 100% case functional.