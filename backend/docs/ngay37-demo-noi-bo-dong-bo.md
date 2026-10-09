# Demo nội bộ luồng đồng bộ dữ liệu và Exit Sprint — Ngày 37

**Người thực hiện:** Nguyễn Đại Vương, Nguyễn Phi Long
**Ngày giờ demo thực tế:** <ngày giờ thực tế>
**Backend:** Render (https://plix-7jfp.onrender.com), Postgres Supabase
**Android:** nhánh `sprint4-android-long`, thiết bị A: <emulator/thiết bị A>, thiết bị B: <emulator/thiết bị B>
**Tài khoản:** tài khoản demo riêng (chưa từng chạy script kiểm thử)

## 1. Regression phía backend

| Hạng mục | Kết quả |
|---|---|
| `python -m pytest` trong `backend` | 127 passed, 0 failed |
| `simulate_two_devices.py` (Render) | 5/5 kịch bản đạt |
| `sync_functional_check.py` (Render) | 35/35 case đạt |
| `sync_edge_check.py` (Render, không chạy G5) | 25/25 case đạt |

## 2. Demo từng bước

| Bước | Nội dung | Kết quả |
|---|---|---|
| A1 | Đăng nhập khi có mạng rồi bật chế độ máy bay | ĐẠT |
| A2 | Ngoại tuyến: thêm 2 giao dịch T1, T2, hiện ngay | ĐẠT |
| A3 | Ngoại tuyến: thêm danh mục C1, ngân sách B1 (theo C1), mục tiêu G1, hiện ngay | ĐẠT |
| A4 | Ngoại tuyến: sửa T1 (50.000 thành 70.000), xoá T2 | ĐẠT |
| A5 | Server chưa có dòng demo37 nào khi còn ngoại tuyến | ĐẠT |
| B1 | Có mạng lại, biểu tượng đồng bộ về bình thường | ĐẠT |
| B2 | Server có T1 (70.000), T2 là tombstone, C1, B1, G1, mỗi bản ghi đúng 1 dòng | ĐẠT |
| B3 | Bảng corrections: không tạo được bằng giao diện; script F1–F7 và G1–G4 cột corrections | ĐẠT |
| C1 | Thiết bị B đăng nhập: thấy T1, C1, B1, G1, không thấy T2, không nhân đôi | ĐẠT |
| C2 | Thiết bị B thêm T3, sửa G1 (500.000) và lên server | ĐẠT |
| C3 | Thiết bị A nhận T3 và bản sửa G1 sau lượt đồng bộ kế tiếp | ĐẠT |
| D1 | Ngoại tuyến còn dữ liệu chưa đồng bộ: Đăng xuất hiện đúng hộp thoại xác nhận | ĐẠT |
| D2 | Chọn "Huỷ, để tôi thử lại sau": không đăng xuất, dữ liệu còn | ĐẠT |
| D3 | Có mạng, đồng bộ xong: Đăng xuất không hiện hộp thoại | ĐẠT |
| D4 | Đăng nhập lại: dữ liệu đủ, T2 không hiện lại | ĐẠT |
| D5 | Chọn "Vẫn đăng xuất (mất thay đổi chưa đồng bộ)" | ĐẠT |
| D6 | Đăng nhập lại: T6 mất (đúng giới hạn đã ghi), dữ liệu đã đồng bộ vẫn đủ | ĐẠT |

## 3. Exit Criteria Sprint (Master Plan Ngày 37)

- Bảng đối chiếu 20 dòng Mục 18.6 (mục 5 của `ngay36-ket-qua-test-phuc-tap.md`): 20/20.
- Demo đầy đủ ngoại tuyến → đồng bộ → nhiều thiết bị → đăng xuất/đăng nhập: 17/17.

## 4. Lỗi phát hiện và cách sửa


## 5. Kết luận
