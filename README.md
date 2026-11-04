# Plix

Ứng dụng Android quản lý chi tiêu cá nhân, ứng dụng trí tuệ nhân tạo (Đồ án 1).

- Sinh viên thực hiện: Nguyễn Phi Long (24521007), Nguyễn Đại Vương (24522050)
- Giảng viên hướng dẫn: Ths.Võ Tuấn Kiệt
- Kho mã nguồn: https://github.com/Nguyen-Phi-Long-702/Plix

## Tổng quan

Plix theo kiến trúc local-first: dữ liệu được ghi vào cơ sở dữ liệu trên máy (Room) trước, nên các chức năng tài chính cốt lõi dùng được khi không có mạng. Khi có mạng, ứng dụng đồng bộ hai chiều với Supabase Postgres thông qua backend FastAPI. Các tính năng AI chạy trên backend nên cần kết nối mạng.

## Chức năng

- Tài khoản: đăng ký, đăng nhập bằng Supabase Auth; token đăng nhập được lưu mã hoá trên máy.
- Giao dịch thu/chi: thêm, sửa, xoá mềm; lọc theo danh mục và khoảng thời gian; tìm theo ghi chú.
- Danh mục: 12 danh mục hệ thống và danh mục do người dùng tự tạo.
- Giao dịch định kỳ hằng tháng.
- Ngân sách có cảnh báo khi đạt ngưỡng (thông báo cục bộ) và mục tiêu tiết kiệm.
- Thống kê bằng biểu đồ.
- Đồng bộ hai chiều cho 5 bảng: giao dịch, danh mục, ngân sách, mục tiêu tiết kiệm, dữ liệu sửa gợi ý AI. Xung đột xử lý theo nguyên tắc ghi đè lần cuối cùng thắng (dựa trên thời gian cập nhật); dữ liệu bị xoá chỉ được đánh dấu xoá, không xoá vật lý.
- Tính năng AI: xem bảng bên dưới.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Android | Java, giao diện XML, MVVM, Dagger Hilt, Room, Retrofit, Jetpack Navigation, WorkManager, MPAndroidChart (minSdk 26, targetSdk 36) |
| Backend | Python, FastAPI, uvicorn, asyncpg, PyJWT, httpx |
| Đám mây | Supabase (Auth và Postgres), Render (chạy backend), Google Gemini (dự phòng cho nhập nhanh) |

## Tính năng AI

| Tính năng | Loại kỹ thuật |
|---|---|
| Gợi ý danh mục từ ghi chú và học lại từ các lần người dùng sửa gợi ý (AI-01, AI-02) | Machine learning cổ điển: TF-IDF và Naive Bayes do nhóm tự viết, không dùng thư viện ML |
| Phát hiện giao dịch bất thường (AI-04) | Thống kê thuần, không phải machine learning |
| Dự báo net thu − chi cuối tháng (AI-05) | Thống kê thuần (làm mượt theo cấp số mũ), không phải machine learning |
| Nhập nhanh bằng ngôn ngữ tự nhiên (AI-06) | Rule-based: biểu thức chính quy cho số tiền và từ điển từ khoá |
| Gemini dự phòng cho nhập nhanh (AI-07) | Mô hình ngôn ngữ lớn (LLM) của Google, gọi qua API |

- `confidence` của gợi ý danh mục là điểm tin cậy tương đối (normalized score) để so sánh giữa các danh mục, không phải xác suất đã hiệu chỉnh. Giao diện hiển thị dưới dạng phần trăm chỉ là cách định dạng, không có nghĩa "đúng chừng đó phần trăm".
- Gemini mặc định tắt; người dùng bật bằng công tắc "Cho phép dùng Gemini dự phòng" trong Cài đặt. Gói miễn phí của Google có thể dùng dữ liệu prompt để cải thiện sản phẩm. Khi bật, backend gửi câu người dùng nhập và tên các danh mục của họ cho Gemini, không gửi lịch sử giao dịch.
- Chi tiết công thức và kết quả đo độ chính xác: [docs/AI_METHODOLOGY.md](docs/AI_METHODOLOGY.md).

## Cài đặt và triển khai

- Hướng dẫn cài đặt (Android và Backend): docs/HUONG_DAN_TRIEN_KHAI.md
- Hướng dẫn triển khai (Android và Backend): docs/HUONG_DAN_CAI_DAT.md

## Cấu trúc thư mục

| Thư mục | Nội dung |
|---|---|
| `android/` | Dự án Android Studio (module `app`) |
| `backend/` | Backend FastAPI: `app/` (core, routers, services, models, tests), `sql/` (cấu trúc bảng Postgres), `data/` (dữ liệu seed và tập kiểm tra AI), `scripts/`, `docs/` (ghi chép kết quả đo, kiểm thử và demo nội bộ theo ngày) |
| `docs/` | Tài liệu thiết kế, bảo mật, phương pháp AI và kết quả kiểm thử |

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [docs/SEQUENCE_DIAGRAMS.md](docs/SEQUENCE_DIAGRAMS.md) | Sơ đồ trình tự 4 luồng chính |
| [docs/USE_CASE_DIAGRAM.md](docs/USE_CASE_DIAGRAM.md) | Sơ đồ Use Case |
| [docs/AI_METHODOLOGY.md](docs/AI_METHODOLOGY.md) | Phương pháp AI và đánh giá độ chính xác |
| [docs/SYNC_PAYLOAD_SCHEMA.md](docs/SYNC_PAYLOAD_SCHEMA.md) | Định dạng dữ liệu đồng bộ đã chốt |
| [docs/API_CONTRACT_VERIFY.md](docs/API_CONTRACT_VERIFY.md) | Báo cáo đối chiếu endpoint đồng bộ với định dạng đã chốt |
| [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md) | Tổng hợp rà soát bảo mật |
| [docs/UI6_DOI_CHIEU_MAN_LONG.md](docs/UI6_DOI_CHIEU_MAN_LONG.md) | Đối chiếu chéo giao diện các màn với bản thiết kế (Ngày UI-6) |
| [docs/UI7_KET_QUA_HOI_QUY.md](docs/UI7_KET_QUA_HOI_QUY.md) | Kết quả hồi quy và kiểm thử bản release R8 (Ngày UI-7) |
| [docs/NGAY53_KET_QUA_KIEM_THU_CUOI.md](docs/NGAY53_KET_QUA_KIEM_THU_CUOI.md) | Kết quả kiểm thử cuối và đối chiếu Exit Criteria (Ngày 53) |