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
- Xuất danh sách giao dịch ra file CSV (tối đa 1 lần/phút) và chia sẻ qua ứng dụng khác.
- Màn Giới thiệu: phiên bản, nhóm thực hiện, liên kết kho mã nguồn.
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

- Hướng dẫn cài đặt (dựng môi trường phát triển Android và Backend trên máy cá nhân): [docs/HUONG_DAN_CAI_DAT.md](docs/HUONG_DAN_CAI_DAT.md)
- Hướng dẫn triển khai (Supabase, Render và cài ứng dụng Android): [docs/HUONG_DAN_TRIEN_KHAI.md](docs/HUONG_DAN_TRIEN_KHAI.md)

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
| [docs/SO_DO_KIEN_TRUC.md](docs/SO_DO_KIEN_TRUC.md) | Sơ đồ kiến trúc hệ thống |
| [docs/SO_DO_ERD.md](docs/SO_DO_ERD.md) | Sơ đồ quan hệ thực thể cơ sở dữ liệu |
| [docs/BAO_CAO_KIEM_THU.md](docs/BAO_CAO_KIEM_THU.md) | Báo cáo kiểm thử |
| [docs/PHU_LUC_DANH_SACH_TEST.md](docs/PHU_LUC_DANH_SACH_TEST.md) | Danh sách toàn bộ test case |
| [docs/KICH_BAN_DEMO.md](docs/KICH_BAN_DEMO.md) | Kịch bản demo |
| [docs/NGAY57_KET_QUA_BAN_RELEASE_CUOI.md](docs/NGAY57_KET_QUA_BAN_RELEASE_CUOI.md) | Kết quả build và kiểm thử bản release cuối (Ngày 57) |
| [docs/MA_TRAN_TRUY_VET.md](docs/MA_TRAN_TRUY_VET.md) | Ma trận truy vết yêu cầu (75 mã) tới mã nguồn, kiểm thử và tài liệu |
| [docs/NGAY58_KET_QUA_KIEM_TRA_CUOI.md](docs/NGAY58_KET_QUA_KIEM_TRA_CUOI.md) | Kết quả diễn tập lần 2, danh sách kiểm tra cuối và phiên bản nộp (Ngày 58) |

## Giới hạn đã biết của hệ thống

Các giới hạn dưới đây được ghi rõ có chủ đích, không phải lỗi chưa phát hiện.

1. **Thời gian khởi động máy chủ (cold start).** Backend chạy trên Render gói miễn phí. Theo ước lượng trong kế hoạch của nhóm, sau khoảng 15 phút không có yêu cầu nào, lần gọi đầu tiên có thể mất khoảng 30–60 giây để máy chủ khởi động lại. Đây là giới hạn chấp nhận được của gói miễn phí. Cách xử lý: gọi `GET https://plix-7jfp.onrender.com/api/v1/health` để làm nóng máy chủ trước khi dùng thử hoặc demo. Các số đo hiệu năng ở `backend/docs/ngay50-ket-qua-hieu-nang-backend.md` được đo sau khi đã làm nóng.
2. **Hạn mức sử dụng Gemini.** Nhánh dự phòng Gemini dùng gói miễn phí của Google nên có hạn mức; nhóm không cam kết số lượt gọi. Khi Gemini hết hạn mức, quá thời gian chờ (8 giây), lỗi mạng hoặc máy chủ, hoặc thiếu khoá API, backend trả mã lỗi `AI_BUSY` và ứng dụng báo "AI đang bận, vui lòng nhập tay". Gemini chỉ được gọi khi người dùng đã bật công tắc Gemini dự phòng và bộ phân tích tự viết không xử lý được cả câu. Gói miễn phí của Google có thể dùng dữ liệu prompt để cải thiện sản phẩm (xem phần Tính năng AI về dữ liệu được gửi đi).
3. **Độ chính xác của AI phụ thuộc lượng dữ liệu.** Mô hình gợi ý danh mục được huấn luyện trên bộ dữ liệu mẫu 600 dòng (10 danh mục, mỗi danh mục 60 dòng); trên tập kiểm tra độc lập 50 ghi chú đạt 43/50 = 86,0% (đo Ngày 26). Mỗi danh mục chỉ có 5 ghi chú kiểm tra nên con số này chỉ là ước lượng thô; hai danh mục thấp nhất là Nhà ở/Thuê nhà và Mua sắm (cùng 3/5). Mô hình riêng của từng người dùng chỉ được dùng khi đã có ít nhất 10 lần sửa gợi ý (`training_sample_count` ≥ 10) và người dùng bấm "Cải thiện gợi ý AI" (tối đa 1 lần mỗi giờ); trước đó dùng mô hình mẫu dùng chung. Gợi ý có confidence dưới 0,4 được hiển thị là "Độ tin cậy thấp" và không tự điền danh mục. Phát hiện bất thường báo "chưa đủ dữ liệu" khi danh mục có dưới 5 giao dịch trên máy chủ; dự báo báo "chưa đủ dữ liệu" khi có dưới 4 tuần trọn vẹn.
4. **Mất dữ liệu đang chờ đồng bộ nếu chọn "Vẫn đăng xuất".** Khi đăng xuất mà còn thay đổi chưa đồng bộ, ứng dụng hiện hộp thoại xác nhận. Nếu người dùng chọn "Vẫn đăng xuất", các thay đổi chưa đồng bộ sẽ mất vĩnh viễn trên thiết bị đó; dữ liệu đã đồng bộ trước đó vẫn an toàn trên cloud. Gỡ ứng dụng khi còn thay đổi chưa đồng bộ cũng gây mất dữ liệu tương tự. Ứng dụng không có hàng đợi thử lại sau khi đăng xuất. Đây là lựa chọn có ý thức của người dùng sau khi đã được cảnh báo.
5. **Kết quả phát hiện bất thường có thể lệch với dữ liệu chưa đồng bộ.** Do kiến trúc local-first, máy chủ chỉ tính trên dữ liệu đã đồng bộ lên cloud, nên kết quả chỉ chính xác với dữ liệu đã đồng bộ. Khi còn giao dịch cùng danh mục đang chờ đồng bộ, ứng dụng hiển thị thêm dòng "Kết quả có thể chưa tính giao dịch mới nhất đang chờ đồng bộ". Đây là thống kê đơn giản nên chấp nhận tỷ lệ gắn cờ nhầm nhất định khi dữ liệu ít.
6. **Chưa có chức năng xoá tài khoản.** Xoá tài khoản chưa được xây dựng trong phạm vi Đồ án 1 (để lại cho giai đoạn sau).
7. **Cơ sở dữ liệu cục bộ chưa được mã hoá.** Mã hoá bằng SQLCipher là hạng mục không bắt buộc và nhóm chưa thực hiện (cấu hình Gradle không có SQLCipher). Cơ sở dữ liệu Room trên máy lưu dạng không mã hoá; token đăng nhập vẫn được lưu mã hoá và tính năng sao lưu tự động của hệ thống đã tắt. Hệ quả: nếu mất máy và máy không có khoá màn hình, dữ liệu tài chính cục bộ không được bảo vệ bằng mã hoá của ứng dụng.
8. **Thời gian Supabase tạm ngưng do không hoạt động chưa được xác nhận.** Dự án Supabase có thể bị tạm ngưng khi không hoạt động lâu. Nhóm chưa xác nhận được số ngày chính xác nên không ghi con số; cần kiểm tra tài liệu chính thức của Supabase tại thời điểm triển khai.

Các giới hạn khác đã ghi trong kế hoạch dự án:

9. **Không đồng bộ thời gian thực giữa nhiều thiết bị.** Dữ liệu được đồng bộ định kỳ mỗi 15 phút và sau mỗi lần ghi dữ liệu mới, nên có độ trễ giữa hai thiết bị.
10. **Confidence của gợi ý danh mục không phải xác suất.** Đây là điểm chuẩn hoá để so sánh tương đối, không phải xác suất đã hiệu chỉnh (xem phần Tính năng AI).
11. **Dự báo không phải số dư ngân hàng thật.** Dự báo net thu − chi chỉ là ước lượng dựa trên xu hướng gần nhất trong dữ liệu mà ứng dụng theo dõi.
12. **Chưa có báo cáo lỗi tự động (crash reporting).** Ứng dụng chưa tích hợp công cụ báo cáo sự cố.