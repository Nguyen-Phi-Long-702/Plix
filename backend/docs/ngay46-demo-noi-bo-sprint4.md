# Demo nội bộ Sprint 4 (Anomaly, Forecast, NL Parser, Gemini) và Exit Sprint — Ngày 46

**Người thực hiện:** Nguyễn Đại Vương, Nguyễn Phi Long

## 1. Regression Sprint 4

| Hạng mục | Kết quả |
|---|---|
| `python -m pytest -q` trong `backend` (bản đã merge) | 270 |
| `gradlew.bat testDebugUnitTest` trong `android` (bản đã merge) | BUILD SUCCESSFUL |

## 2. Demo từng bước

| Bước | Nội dung | Kết quả |
|---|---|---|
| A1 | Chi tiêu thấp bất thường: banner thấp kèm giải thích | ĐẠT |
| A2 | Chi tiêu bình thường: không banner | ĐẠT |
| A3 | Chi tiêu cao bất thường: banner cao kèm giải thích | ĐẠT |
| A4 | Danh mục dưới 5 giao dịch: lời nhắn chưa đủ dữ liệu | ĐẠT |
| F1 | Biểu đồ dự báo và dòng giải thích ước tính (tài khoản A) | ĐẠT |
| F2 | Chưa đủ dữ liệu để dự báo (tài khoản B) | ĐẠT |
| P1 | Câu "phở 50k, grab 35k" ra 2 dòng đúng danh mục và số tiền | ĐẠT |
| P2 | Sửa một dòng, hiện "Đã chỉnh sửa" | ĐẠT |
| P3 | Lưu các dòng đã tick vào danh sách giao dịch | ĐẠT |
| P4 | Câu "ăn trưa 45.000đ rồi đổ xăng 80 nghìn" ra 2 dòng 45.000đ, 80.000đ | ĐẠT |
| P5 | Tắt công tắc Nhập nhanh: nút nhập nhanh biến mất | ĐẠT |
| G1 | Bật Gemini: câu phức tạp ra 2.000.000đ | ĐẠT |
| G2 | Tắt Gemini: cùng câu không gọi Gemini, dòng "Chưa chọn danh mục" | ĐẠT |
| G3 | "AI đang bận, nhập tay": kiểm bằng test tự động (`ErrorMapperTest`, `test_parse_nl_returns_ai_busy_when_gemini_is_unavailable`) | ĐẠT |
| O1 | Máy bay: ẩn nút nhập nhanh và thẻ dự báo, thêm giao dịch không lỗi | ĐẠT |
| O2 | Có mạng lại: hiện lại | ĐẠT |
| S1 | Smoke test: gợi ý danh mục và đồng bộ | ĐẠT |

Dòng "Kết quả có thể chưa tính giao dịch mới nhất đang chờ đồng bộ" (anomaly): kiểm bằng test tự động `CheckTransactionAnomalyUseCaseTest`, không demo trực tiếp.

## 3. Exit Criteria Sprint 4 (Master Plan Ngày 46)

- Anomaly + Forecast + NL Parser (Should Have) hoạt động: ĐẠT
- Gemini fallback (bonus Could Have): ĐẠT
- Regression Sprint 4: ĐẠT

## 4. Lỗi phát hiện và cách sửa

- Công tắc "Cho phép dùng Gemini dự phòng" chỉ lưu cục bộ, backend `/parse-nl` không nhận cờ nên vẫn gọi Gemini khi người dùng đã tắt. Sửa: app gửi `allow_gemini`, backend chỉ gọi Gemini khi `allow_gemini = true` (thiếu trường = false).

