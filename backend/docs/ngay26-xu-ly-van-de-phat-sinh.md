# Xử lý vấn đề phát sinh — Ngày 26

**Người thực hiện:** Nguyễn Phi Long (cùng Nguyễn Đại Vương)
**Ngày giờ đo thực tế:** 28/09/2026 9:54pm

## 1. Độ chính xác phân loại sau khi bổ sung dữ liệu mẫu

- **Trước (Ngày 25):** seed 500 dòng → 38 / 50 = 76,0 %
- **Sau (Ngày 26):** seed 600 dòng (mỗi danh mục 60 dòng) → 43 / 50 = 86 %
- **Mốc mục tiêu (nhóm xác nhận):** từ 80 % trở lên → Đạt
- **Model đo:** model mặc định dùng chung (`__seed__`), huấn luyện lại ngày 26 trên `backend/data/seed_categorization.csv`
- **Tập kiểm tra:** giữ nguyên `backend/data/test_categorization_holdout.csv` (50 câu, 5 câu/danh mục)
- **Cách đo:** gọi thật `POST /api/v1/categorize` cho từng câu (backend local qua uvicorn, Postgres Supabase thật), script `backend/app/services/ai/evaluate_holdout_accuracy.py`
- **Kết quả từng dòng:** `backend/data/day26_holdout_results.csv` (Ngày 25: `backend/data/day25_holdout_results.csv`)

| Danh mục | Trước (Ngày 25) | Sau (Ngày 26) |
|---|---|---|
| Di chuyển | 4/5 | 4/5 |
| Ăn uống | 3/5 | 4/5 |
| Nhu yếu phẩm | 3/5 | 5/5 |
| Hoá đơn (điện/nước/internet) | 5/5 | 5/5 |
| Giải trí | 3/5 | 4/5 |
| Sức khoẻ | 4/5 | 5/5 |
| Giáo dục | 5/5 | 5/5 |
| Nhà ở/Thuê nhà | 3/5 | 3/5 |
| Mua sắm | 3/5 | 3/5 |
| Lương | 5/5 | 5/5 |


## 2. Lỗi retrain khi user chưa có giao dịch nào (phát hiện và sửa trong Ngày 26)

- **Hiện tượng:** gọi `POST /api/v1/retrain` khi user chưa có giao dịch nào có danh mục (chưa có giao dịch, hoặc giao dịch không có danh mục / danh mục đã xoá) → server trả 500; đồng thời user bị chặn 429 trong 1 giờ dù chưa huấn luyện được gì.
- **Nguyên nhân:** không có dữ liệu thì TF-IDF báo lỗi và không được bắt; hàng giữ chỗ `ai_model_params` do bước khoá tạo ra mang `trained_at` = thời điểm gọi nên kiểm tra cooldown coi như vừa retrain. `.env.example` quy định cooldown chỉ tính giữa 2 lần retrain **thành công**.
- **Cách sửa:** `retrain_service.py` — nếu không có dữ liệu thì xoá hàng giữ chỗ (chỉ hàng chưa có model) và raise `NoTrainingDataError`; `routers/retrain.py` trả **400** với thông báo tiếng Việt (body `{"error_code": "BAD_REQUEST", "message": "..."}`). User đã có model thì model và `trained_at` được giữ nguyên.
- **Kiểm chứng:** 2 test mới trong `test_retrain_no_data.py`; `python -m app.services.ai.retrain_service` có thêm Case 3 chạy trên Postgres thật.
- **Lưu ý cho Android:** `/retrain` có thể trả 400 kèm `message` như trên.


## 3. Thời gian huấn luyện lại mô hình (retrain)

- **Cách đo:** `python -m app.services.ai.benchmark_retrain` — 5000 giao dịch giả lập cho 1 user riêng, chạy từ máy cá nhân tới supabase thật.
- **Giới hạn:** không đo trực tiếp trên Render.
- **Ngày giờ đo:** 28/09/2026 11:30pm

| Hạng mục | Thời gian (giây) |
|---|---|
| 1 lần truy vấn tới Postgres (SELECT 1, trung vị 5 lần) | 0.122 |
| Chỉ tính toán fit TF-IDF + Naive Bayes (trong bộ nhớ) | 0.043 |
| `retrain_user_model` (khoá + đọc + fit + lưu) | 1.037 |
| `load_model_params` sau retrain (router `/retrain` gọi) | 0.311 |
| Ước tính 1 lần gọi `/retrain` (chưa gồm xác thực JWT) | 1.470 |

- **Mốc tham chiếu:** dưới 3 giây.
- **Kết luận:** Đạt, không cần tối ưu thêm; giữ nguyên `retrain_service.py`.