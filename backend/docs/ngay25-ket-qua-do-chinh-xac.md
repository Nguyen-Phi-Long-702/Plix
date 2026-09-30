# Kết quả đo độ chính xác thực tế — Ngày 25

**Người thực hiện:** Nguyễn Phi Long & Nguyễn Đại Vương (cùng chạy, cùng ghi nhận)
**Ngày giờ đo thực tế:** 28/09/2026 10:30am
**Model dùng để đo:** model mặc định dùng chung (`__seed__`), huấn luyện Ngày 19 trên `backend/data/seed_categorization.csv` (500 dòng, 10 danh mục)
**Tập kiểm tra:** `backend/data/test_categorization_holdout.csv` — 50 câu mới, không trùng tập huấn luyện, 5 câu/danh mục × 10 danh mục.
**Cách đo:** gọi thật `POST /api/v1/categorize` (backend local qua uvicorn, kết nối Postgres Supabase thật) cho từng câu, đối chiếu qua `GET /api/v1/categories`.
**Script:** `backend/app/services/ai/evaluate_holdout_accuracy.py`
**Kết quả chi tiết từng dòng:** `backend/data/day25_holdout_results.csv`

## Kết quả tổng thể
- Số câu đúng: 38 / 50
- Tỷ lệ chính xác thực tế: 76.0 %

## Breakdown theo từng danh mục

| Danh mục | Đúng/Tổng | Tỷ lệ |
|---|---|---|
| Di chuyển | 4/5 | 80% |
| Ăn uống | 3/5 | 60% |
| Nhu yếu phẩm | 3/5 | 60% |
| Hoá đơn (điện/nước/internet) | 5/5 | 100% |
| Giải trí | 3/5 | 60% |
| Sức khoẻ | 4/5 | 80% |
| Giáo dục | 5/5 | 100% |
| Nhà ở/Thuê nhà | 3/5 | 60% |
| Mua sắm | 3/5 | 60% |
| Lương | 5/5 | 100% |

## Ghi chú
- Đây là số liệu THẬT đo được ngày 28/09/2026, không phải ước lượng - dùng trực tiếp cho phần đánh giá trong báo cáo sprint 7.