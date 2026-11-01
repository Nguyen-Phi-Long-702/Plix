# Kết quả kiểm thử hiệu năng backend trên Render — Ngày 50

**Người thực hiện:** Nguyễn Đại Vương
**Ngày giờ chạy:** ... (thời điểm script in ra)
**Backend đã đo:** https://plix-7jfp.onrender.com (Render)
**Script:** `backend/scripts/benchmark_render_latency.py`

## 1. Cách đo

- Đăng nhập Supabase bằng tài khoản thử nghiệm để lấy JWT thật.
- Làm nóng: gọi `GET /api/v1/health` trước (lần này mất ... ms), không tính vào kết quả, để loại thời gian khởi động của Render.
- Mỗi endpoint: 1 lần gọi khởi động (không tính) rồi đo 5 lần liên tiếp; thời gian đo từ máy chạy script tới Render và nhận đủ phản hồi (gồm cả độ trễ mạng).
- Mục tiêu: **dưới 3 giây**; ĐẠT khi cả 5 lần đo đều dưới 3 giây (kèm nhỏ nhất, trung vị, lớn nhất).
- Đây là benchmark cơ bản, không phải load test / stress test.

## 2. Kết quả
| Endpoint | Điều kiện đo | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |
|---|---|---|---|---|---|---|
| `POST /api/v1/categorize` | ghi chú "cà phê" | 2060.4 | 2099.2 | 2793.2 | < 3000 ms | ĐẠT |
| `GET /api/v1/sync/transactions/pull` | since=0, limit=500, trả về 500 bản ghi | 1669.1 | 1702.3 | 2384.0 | < 3000 ms | ĐẠT |

## 3. Đánh giá
Cả 2 endpoint đều phản hồi dưới 3 giây sau khi làm nóng; lần đo chậm nhất là 2793.2 ms.
