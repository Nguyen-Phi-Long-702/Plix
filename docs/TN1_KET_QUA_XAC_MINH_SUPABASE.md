# Kết quả xác minh Supabase Auth cho quên và đổi mật khẩu — Ngày DP-1

**Người thực hiện:** Nguyễn Đại Vương
**Thiết bị:** Máy tính Windows (PowerShell, curl.exe)

## 1. CXM-01 — Mẫu email 'Reset password' dùng `{{ .Token }}`

- Nội dung mẫu cần đặt: tiêu đề trong thân thư "Đặt lại mật khẩu Plix" và dòng "Mã xác nhận (6 chữ số): {{ .Token }}", không còn `{{ .ConfirmationURL }}` (không áp dụng được, xem kết luận).
- Email nhận được: có, sau khoảng 1 phút, hiển thị liên kết.
- Kết luận: Không làm được: SMTP mặc định của Supabase không cho sửa mẫu email.
- Thông báo trên Dashboard (chép ngắn) và ngày xem: [CẦN ĐIỀN]
- Phương án quay về theo R4: liên kết kèm deep link, ngoài 120 giờ.

## 2. CXM-02 — Thân yêu cầu, phản hồi và mã HTTP

| Điểm cuối | Thân yêu cầu | Header | HTTP khi thành công | Tên trường JSON của phản hồi thành công |
|---|---|---|---|---|
| `POST auth/v1/recover` | `{"email": "<email>"}` | `apikey` | 200 | thân rỗng |
| `POST auth/v1/verify` | `{"type": "recovery", "email": "<email>", "token": "<mã 6 chữ số>"}` | `apikey` | không xác minh được | không xác minh được |
| `PUT auth/v1/user` | `{"password": "<mật khẩu mới>"}` | `apikey`, `Authorization: Bearer <token đã ẩn>` | [CẦN ĐIỀN] | [CẦN ĐIỀN] |

- `verify`: không xác minh được vì CXM-01 không làm được, email chỉ có liên kết nên không có mã 6 chữ số thật để gọi `verify` thành công.
- Kết luận: `recover` khớp kế hoạch; `verify` và `PUT` chưa có phản hồi thành công thật để đối chiếu với kế hoạch.

## 3. CXM-03 — Mã HTTP và `error_code` thực tế

| Trường hợp | Điểm cuối | HTTP | `error_code` | Tên trường thông điệp lỗi |
|---|---|---|---|---|
| Mã sai | `POST auth/v1/verify` | 403 | `otp_expired` | `msg` |
| Mã hết hạn | `POST auth/v1/verify` | không xác minh được | không xác minh được | không xác minh được |
| Mật khẩu trùng mật khẩu hiện tại | `PUT auth/v1/user` | 422 | `same_password` | `msg` |
| Mật khẩu yếu | `PUT auth/v1/user` | 422 | `weak_password` | `msg` |
| Email chưa đăng ký | `POST auth/v1/recover` | 200 | không có | không có |
| Gọi lại trong 60 giây | `POST auth/v1/recover` | 403 | `otp_expired` | `msg` |

- Mã sai: thử ngày 16/11/2026 với mã `000000`; phản hồi `{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}`.
- Mã hết hạn: không xác minh được vì cần một mã thật đã hết hạn, mà CXM-01 không làm được nên không có mã thật.

## 4. CXM-04 — Thời hạn mã thực tế (không xác minh được)

- Thời hạn cấu hình trên Dashboard: 3600 giây.
- Đã chờ ... phút kể từ lúc gửi: không xác minh được vì không có mã 6 chữ số thật (CXM-01 không làm được).
- Kết luận: không xác minh được thời hạn thực tế của mã.

## 5. CXM-05 — Độ dài mật khẩu tối thiểu

- Độ dài ngắn nhất được chấp nhận: 6 ký tự.
- Kết quả mật khẩu quá ngắn: HTTP 422, `error_code` weak_password.
- So với hint "Tối thiểu 8 ký tự" ở màn Đăng ký: không khớp.

## 6. CXM-06 — Cấu hình Auth hiện tại

| Mục | Giá trị |
|---|---|
| Confirm email | Tắt |
| Nhà cung cấp SMTP | SMTP mặc định của Supabase |
| Hạn mức email mỗi giờ | 2 |
| Người nhận email với SMTP mặc định | [CẦN ĐIỀN: chỉ thành viên organization hoặc khác; ghi `email_address_not_authorized` nếu đã gặp khi dùng địa chỉ ngoài] |

## 7. Bảng mã lỗi cần ánh xạ cho DP-2-V2

| Tình huống | HTTP thực tế | `error_code` thực tế | Dự kiến trong thiết kế | Khớp |
|---|---|---|---|---|
| Mã sai | 403 | `otp_expired` | 403 `otp_expired` | Có |
| Mã hết hạn | không xác minh được | không xác minh được | 403 `otp_expired` | không xác minh được |
| Mật khẩu yếu | 422 | `weak_password` | 422 `weak_password` | Có |
| Mật khẩu mới trùng mật khẩu hiện tại | 422 | `same_password` | 422 `same_password` | Có |
| Giới hạn gửi email | 429 | `over_email_send_rate_limit` | 429 | Có |

Tên trường thông điệp lỗi của Supabase: `msg`. `ErrorResponseDto` hiện đọc `error_code` và `message`, nên `extractServerMessage` không đọc được câu thông báo của Supabase Auth (tên trường khác nhau).
