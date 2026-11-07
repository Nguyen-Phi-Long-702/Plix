# Kịch bản demo Plix v1

**Tài khoản:** tài khoản demo riêng của nhóm (email/mật khẩu giữ riêng, không ghi vào kho mã nguồn).
**Thiết bị:** điện thoại hoặc emulator đã cài APK release, Wi-Fi bật.
**Backend:** `https://plix-7jfp.onrender.com` (Render gói miễn phí, sau khoảng 15 phút không dùng thì lần gọi đầu có thể mất 30–60 giây).

## 1. Chuẩn bị trước giờ demo

| # | Việc | Dấu hiệu đạt |
|---|---|---|
| C1 | Làm nóng backend: mở `https://plix-7jfp.onrender.com/api/v1/health` trên trình duyệt, đợi khoảng 1 phút | Trả `"database":"connected"` |
| C2 | Đăng nhập tài khoản demo trên app, chờ biểu tượng đồng bộ ngừng quay | Trang chủ có dữ liệu |
| C3 | Thống kê → thẻ **Dự báo NET cuối tháng** | Hiện số tiền và biểu đồ |
| C4 | Cài đặt → **Quản lý danh mục** | Có **Thú cưng** và **Quà tặng**, không còn **Du lịch** |
| C5 | Tab **Ngân sách** (cả hai tab **Ngân sách** và **Mục tiêu**) | Chưa có ngân sách tổng tháng này, chưa có mục tiêu **Mua laptop mới** |
| C6 | Xem lần cuối bấm **Cải thiện gợi ý AI** (backend chỉ cho huấn luyện lại 1 lần/giờ cho mỗi tài khoản) | Đã quá 1 giờ kể từ lần bấm cuối, hoặc chưa bấm lần nào |

## 2. Các bước demo

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| B1 | **Trang chủ** | Thấy **Thu nhập**, **Chi tiêu**, **Ngân sách tháng này**, **Giao dịch gần đây**; dữ liệu có sẵn nhờ đồng bộ từ máy chủ |
| B2 | Bấm **Xem tất cả** → ô **Tìm theo ghi chú...** gõ `cà phê` | Danh sách lọc còn các giao dịch có ghi chú chứa "cà phê" |
| B3 | Bấm **Thêm** → **Thêm giao dịch** → Số tiền `45000`, Ghi chú `trà sữa` | Hiện gợi ý danh mục **Ăn uống**; bấm **Dùng gợi ý** → **Lưu giao dịch** → lưu xong, màn hình đóng (45.000đ nằm trong mức thông thường nên không có cảnh báo) |
| B4 | **Thêm giao dịch** → Số tiền `5000`, Ghi chú `cà phê`, Danh mục **Ăn uống** → **Lưu giao dịch** | Banner **Chi tiêu thấp bất thường**, giải thích có trung bình khoảng 67.000–73.000đ, kèm câu "Chỉ để bạn lưu ý, không phải lỗi." |
| B5 | **Thêm giao dịch** → Số tiền `2500000`, Ghi chú `ăn tối nhà hàng`, Danh mục **Ăn uống** → **Lưu giao dịch** | Banner **Chi tiêu cao bất thường**, giải thích có trung bình khoảng 67.000–73.000đ và giao dịch này 2.500.000đ |
| B6 | Bấm **Thêm** → **Nhập nhanh (AI)** → gõ `phở 50k, grab 35k` → **Phân tích bằng AI** | **Kết quả nhận diện** có 2 dòng: 50.000đ **Ăn uống** và 35.000đ **Di chuyển** → bấm **Lưu giao dịch** |
| B7 | Tab **Thống kê** | Thấy **Chi tiêu theo danh mục**, **Thu vs Chi (tháng)**, **Xu hướng thu chi theo tháng**; kéo xuống thẻ **Dự báo NET cuối tháng** có số tiền, biểu đồ và dòng giải thích đây là số ước tính, không phải số dư ngân hàng |
| B8 | **Cài đặt → Quản lý danh mục** | Thấy danh mục hệ thống và danh mục của người dùng |
| B8a | Menu **Thêm danh mục** → Tên `Du lịch`, Loại **Chi tiêu** → **Lưu** | Báo **Đã lưu danh mục**, **Du lịch** xuất hiện trong danh sách |
| B8b | Bấm biểu tượng **Sửa danh mục** ở **Quà tặng** → đổi tên thành `Quà biếu` → **Lưu** | Tên đổi thành **Quà biếu** |
| B8c | Bấm biểu tượng **Xoá danh mục** ở **Thú cưng** → **Xoá** | Danh mục biến mất; quay lại danh sách giao dịch, các giao dịch cũ của **Thú cưng** hiển thị **Danh mục không xác định** |
| B9 | Tab **Ngân sách** → nút **Thêm ngân sách** → Phạm vi **Ngân sách tổng**, Hạn mức `5000000` → **Lưu ngân sách** | Ngân sách xuất hiện kèm thanh tiến độ |
| B9a | Chuyển sang tab **Mục tiêu** → nút **Thêm mục tiêu** → Tên `Mua laptop mới`, Số tiền mục tiêu `20000000`, Số tiền đã tiết kiệm hiện tại `5000000`, Hạn hoàn thành chọn một ngày trong tương lai → **Lưu mục tiêu** | Mục tiêu xuất hiện trong danh sách; Plix tự tính số tiền cần tiết kiệm mỗi tháng |
| B10 | Bật **chế độ máy bay** → **Thêm giao dịch** → Số tiền `40000`, Ghi chú `cơm trưa`, Danh mục **Ăn uống** → **Lưu giao dịch** | Lưu được ngay không cần mạng; nút **Nhập nhanh (AI)** và thẻ dự báo bị ẩn |
| B10a | Tắt chế độ máy bay | Biểu tượng đồng bộ quay rồi ngừng; nút **Nhập nhanh (AI)** và thẻ dự báo hiện lại |
| B11 | **Cài đặt → Cải thiện gợi ý AI** | Báo **Đã cập nhật mô hình gợi ý AI**. Nếu vừa huấn luyện trong vòng 1 giờ thì báo "Vui lòng đợi thêm khoảng N phút trước khi huấn luyện lại" |
| B12 | *(Chỉ khi đã làm xuất CSV)* **Cài đặt → Xuất dữ liệu ra CSV** | Hiện bảng chia sẻ **Chia sẻ file CSV**; chọn một ứng dụng nhận file. Mở file thấy 6 cột tiếng Việt đúng dấu: Ngày giờ, Loại, Danh mục, Số tiền (VND), Ghi chú, Phương thức thanh toán. Bấm lần 2 trong vòng 60 giây → báo "Vui lòng đợi thêm N giây trước khi xuất dữ liệu lại" |
| B13 | **Cài đặt → Giới thiệu** | Thấy thông tin Plix, phiên bản và nhóm thực hiện |

## 3. Khi có sự cố

| Tình huống | Cách xử lý |
|---|---|
| Lần gọi AI hoặc đồng bộ đầu tiên chậm | Backend Render đang khởi động lại; đợi tối đa 1 phút (đã làm nóng ở C1 thì hiếm gặp) |
| Banner nói danh mục chưa đủ giao dịch | Dữ liệu demo chưa kéo hết về máy → đăng xuất, đăng nhập lại, chờ đồng bộ xong |
| B6, B7 không dùng được AI/dự báo | Máy đang mất mạng (các tính năng này cần mạng); bật lại mạng |
| Dưới banner ở B5 có dòng "Kết quả có thể chưa tính giao dịch mới nhất đang chờ đồng bộ" | Bình thường: giao dịch ở B4 chưa kịp đồng bộ. Đợi vài giây giữa B4 và B5 thì dòng này không hiện |
| B11 báo phải đợi N phút | Đã huấn luyện trong vòng 1 giờ qua; có thể dùng chính thông báo này để minh hoạ giới hạn tần suất |

## 4. Dọn dẹp sau mỗi lần diễn tập hoặc demo

1. Xoá 6 giao dịch kịch bản đã tạo (biểu tượng **Xoá giao dịch** → **Xoá**): `trà sữa` 45.000, `cà phê` 5.000, `ăn tối nhà hàng` 2.500.000, `phở` 50.000, `grab` 35.000, `cơm trưa` 40.000.
2. Xoá danh mục **Du lịch**; xoá ngân sách tổng và mục tiêu **Mua laptop mới** (biểu tượng **Xoá ngân sách**, **Xoá mục tiêu**).
3. Trên máy tính, chạy lại script nạp dữ liệu (`python -m scripts.seed_demo_data` trong `backend`) để khôi phục danh mục **Thú cưng**, tên **Quà tặng** và làm mới ngày giờ dữ liệu.
4. Trên app: **Đăng xuất** rồi **Đăng nhập** lại tài khoản demo để kéo dữ liệu đã khôi phục.