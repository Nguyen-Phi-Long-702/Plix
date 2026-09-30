# Demo nội bộ luồng categorize → correction → retrain — Ngày 27

**Người thực hiện:** Nguyễn Phi Long (chạy script demo)
**Ngày giờ demo thực tế:** 29/09/2026, 07:26 pm
**Cách demo:** gọi API thật qua `POST /api/v1/categorize`, `POST /api/v1/correction`,
`POST /api/v1/retrain` (backend local qua uvicorn, kết nối Postgres Supabase thật),
dùng tài khoản test mới, chưa từng retrain.
**Script:** `backend/app/services/ai/demo_categorize_correction_retrain.py`

## Kết quả từng bước

| Bước | Endpoint | Kết quả thật |
|---|---|---|
| 0 | `GET /whoami`, `GET /categories` + tạo giao dịch tạm | `user_id` lấy từ JWT: `c5226000-d68b-4929-b913-cf997183144e`; dùng 2 category thật: Ăn uống / Di chuyển; note demo: `do xang xe may` |
| 1 | `POST /categorize` | `category_id=sys_di_chuyen` (Di chuyển), `confidence=0.7469` |
| 2 | `POST /correction` | `201 Created`, `{'id': '73bf3c1f-54e4-49a3-9cd4-9bf7d63a6e80'}` |
| 3 | 2× `POST /retrain` đồng thời | `[200, 409]` — request 1: `200 {'trained_at': '2026-09-29T12:26:44.359277Z', 'training_sample_count': 1}`; request 2: `409 {'error_code': 'CONFLICT', 'message': 'Đang huấn luyện mô hình, vui lòng thử lại sau'}` |
| 4 | `POST /retrain` gọi ngay sau | `429 {'error_code': 'TOO_MANY_REQUESTS', 'message': 'Vui lòng đợi thêm khoảng 60 phút trước khi huấn luyện lại'}` |
| cleanup | xoá giao dịch + correction tạm | Đã dọn sạch, không để lại rác trong Postgres thật |

## Kết luận

- Cơ chế **giới hạn tần suất** (`429`, thời gian chờ khoảng 60 phút theo thông báo server trả về): **đạt**.
- Cơ chế **khoá chống chạy đồng thời** (`is_training`, `409`): **đạt** — trong 2 request đồng thời chỉ 1 request được huấn luyện (200), request còn lại bị từ chối (409).
- Luồng `categorize → correction → retrain` hoạt động đúng end-to-end với API thật.

## Ghi chú

- Tài khoản test mới chưa có model riêng nên `/categorize` dùng model seed mặc định (cold-start), đúng thiết kế Ngày 24.
- Dữ liệu demo `training_sample_count = 1` vì tài khoản test chỉ có đúng 1 correction; đây là dữ liệu minh hoạ cơ chế, không dùng để đánh giá độ chính xác (độ chính xác đã đo ở Ngày 25/26).
- Ở bước 2, category gửi lên làm `corrected_category_id` là Di chuyển — trùng với gợi ý ở bước 1. Bước này chỉ nhằm chứng minh endpoint ghi nhận correction thành công (201), không nhằm mô phỏng một lần sửa sai thật.