# Kết quả kiểm thử hiệu năng cơ sở dữ liệu cục bộ (Room) — Ngày 50

**Người thực hiện:** Nguyễn Phi Long
**Ngày chạy:** 19/10/2026
**Thiết bị đo:** Medium Phone, Android 16.0
**Bản build:** debug (chưa bật ProGuard/R8)
**Test:** `TransactionListPerformanceTest` (androidTest, `com.longvuong.plix.data.local`)

## 1. Cách đo

- Tạo 5.000 giao dịch giả lập (cố định, không ngẫu nhiên) trong file cơ sở dữ liệu Room riêng, xoá sau khi test xong.
- Mỗi phép đo: 1 lần khởi động (không tính) rồi đo 5 lần; lấy **trung vị** so với mục tiêu **dưới 100 ms** (kèm nhỏ nhất, lớn nhất).
- Đây là benchmark cơ bản, không phải load test / stress test.

## 2. Kết quả

| Phép đo | Số dòng trả về | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |
|---|---|---|---|---|---|---|
| Danh sách đầy đủ, sắp xếp mới nhất trước (`getAll`) | 5000 | 53.7 | 59.9 | 73.2 | < 100 ms | ĐẠT |
| `getAll` + lọc danh mục + khoảng ngày (`FilterTransactionsUseCase`) | 73 | 60.2 | 71.6 | 75.8 | < 100 ms | ĐẠT |
| Tìm theo ghi chú + sắp xếp (`searchByNote`) | 111 | 5.7 | 6.6 | 6.6 | < 100 ms | ĐẠT |

## 3. Đánh giá

Cả 3 phép đo đều dưới 100 ms với 5.000 giao dịch trên thiết bị đo; lần đo chậm nhất là 75.8 ms. Số liệu đo trên emulator, bản debug, và chỉ gồm thời gian truy vấn và dựng danh sách (chưa gồm thời gian vẽ giao diện), nên kết quả trên máy thật có thể khác.