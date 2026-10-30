# Đối chiếu chéo giao diện — các màn của Long (Ngày UI-6, 02/11/2026)

Người thực hiện: Nguyễn Đại Vương. Nguồn đối chiếu: `plix-ui-design.html`.

## Phạm vi
Đăng nhập, Đăng ký, Home, Thêm/Sửa giao dịch (+ 2 bảng chọn), Ngân sách (danh sách, form), Mục tiêu (danh sách, form), Thống kê, ActionBar và thanh điều hướng dưới.

## Lệch đã sửa
| Mã | Màn | Nội dung | Trạng thái |
|---|---|---|---|
| D1 | Ngân sách, Mục tiêu | Khung chờ đổi từ dạng dòng sang dạng thẻ (3 và 2 thẻ) | Đã sửa |
| D2 | Form Mục tiêu | Chữ gợi ý ô "Số tiền đã tiết kiệm hiện tại" theo thiết kế | Đã sửa |
| D3 | Ngân sách, Mục tiêu | Chữ thông báo sau khi lưu theo chế độ Thêm/Sửa | Đã sửa |
| D4 | Thống kê | Nhãn "Thu vs Chi (tháng)" | Đã sửa |

## Lệch ghi nhận, chưa sửa (chờ quyết)
| Mã | Màn | Nội dung | Lý do chưa sửa |
|---|---|---|---|
| N1 | Home | Thiết kế có cột "Còn lại NS tổng" | Kế hoạch ghi thu/chi/số dư ròng; cần dữ liệu mới |
| N2 | Home | Thiết kế có "Xem tất cả" ở mục Ngân sách | Cần thêm điều hướng trong nav_graph (file của Long) |
| N3 | Form Mục tiêu | Thiết kế hiện ô "đã tiết kiệm" cả khi Thêm; chế độ Sửa cho sửa mọi ô | Là logic có sẵn theo Master Plan Ngày 16 |
| N4 | Form giao dịch | Chữ banner giao dịch tương tự khác ví dụ thiết kế | Chữ do ViewModel tạo; đổi phải sửa ViewModel + test, ngoài UI-6 |
| N5 | Thống kê | Tiêu đề dự báo thiếu số tháng | Cần dữ liệu từ ViewModel |
| N6 | Home | Thiết kế viết "- 45.000đ" (có dấu cách sau dấu); app viết "-45.000đ" | Toàn app thống nhất kiểu viết liền (Home, Giao dịch, Nhập nhanh) và có test; cần quyết chung |
| N7 | Khung chờ | Không có hiệu ứng lấp lánh; độ dài vạch giả 50% thay vì 60% (Mục tiêu) | Không có trong kế hoạch (YAGNI) |

## Lệch phát hiện khi chạy app
| Màn | Trạng thái | Thiết kế | App | Xử lý |
|---|---|---|---|---|
| (điền) | | | | |

## Trạng thái không tái hiện được trên emulator

## Kiểm tra danh sách đủ Đang tải / Rỗng và imeAction
- Đủ Đang tải + Rỗng (Fragment và ViewModel đều có): Giao dịch, Ngân sách, Mục tiêu, Quản lý danh mục (không thể rỗng theo Master Plan 10.1), Thống kê, Home.
- `imeAction`: giao dịch (Số tiền, Ghi chú = Done), ngân sách (Hạn mức = Done), mục tiêu (Tên = Next, Số tiền mục tiêu = Done, Số tiền đã tiết kiệm = Done); đã thử bấm trên emulator.