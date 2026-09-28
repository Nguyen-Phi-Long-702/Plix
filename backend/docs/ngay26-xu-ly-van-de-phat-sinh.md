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
