# Kết quả Sync Test Suite Phần 1 (Functional) — Ngày 35

**Người thực hiện:** Nguyễn Đại Vương, Nguyễn Phi Long
**Ngày giờ chạy:** <ngày giờ thực tế>
**Backend đã kiểm tra:** local (uvicorn + Postgres Supabase) và Render (https://plix-7jfp.onrender.com)

## 1. Test tự động phía backend (pytest, pool giả)

| File | Kết quả |
|---|---|
| test_sync_service.py | <N> passed |
| test_sync_endpoint.py | <N> passed |
| test_sync_all_tables.py | <N> passed |
| test_sync_tombstone_duplicate.py | <N> passed |
| test_sync_categories_write.py | <N> passed |

## 2. Script `scripts/sync_functional_check.py` (Postgres thật) — kết quả local / Render

| Case | transactions | categories | budgets | goals | corrections |
|---|---|---|---|---|---|
| F1 Đẩy bản ghi mới | <local> / <Render> | <local> / <Render> | <local> / <Render> | <local> / <Render> | <local> / <Render> |
| F2 Kéo về đúng dữ liệu | | | | | |
| F3 Bản sửa ghi đè bản cũ | | | | | |
| F4 Tombstone vẫn được kéo về | | | | | |
| F5 Không hồi sinh bản đã xoá | | | | | |
| F6 Gửi trùng yêu cầu | | | | | |
| F7 Gửi lại cả lô sau mất mạng | | | | | |

(Mỗi ô ghi ĐẠT hoặc KHÔNG ĐẠT theo dạng `local / Render`; các dòng F2–F7 điền tương tự dòng F1.)

Tổng: local <x>/35, Render <y>/35.

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

<Liệt kê: case, nguyên nhân, cách sửa, kết quả chạy lại. Nếu không có lỗi: "Không phát hiện lỗi.">

## 6. Kết luận

<Số case đạt / tổng số. Đạt 100% case functional thì ghi rõ.>