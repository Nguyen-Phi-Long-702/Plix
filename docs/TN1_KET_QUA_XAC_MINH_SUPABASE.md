# Kết quả xác minh Supabase Auth cho quên và đổi mật khẩu — Ngày DP-1

**Người thực hiện:** Nguyễn Đại Vương
**Thiết bị:** Máy tính Windows (PowerShell, curl.exe)

## 1. CXM-01 — Mẫu email 'Reset password' dùng `{{ .Token }}`

- Nội dung mẫu đã sửa: tiêu đề trong thân thư "Đặt lại mật khẩu Plix" và dòng "Mã xác nhận (6 chữ số): {{ .Token }}", không còn `{{ .ConfirmationURL }}`.
- Email nhận được: có, sau khoảng 1 phút, hiển thị liên kết.
- Kết luận: Không làm được: SMTP mặc định của Supabase không cho sửa mẫu email.
- Nếu không làm được: nội dung thông báo trên Dashboard (chép ngắn), ngày xem, email thử nhận được ở dạng liên kết, phương án quay về theo R4 (liên kết kèm deep link, ngoài 120 giờ).

## 2. CXM-02 — Thân yêu cầu, phản hồi và mã HTTP

| Điểm cuối | Thân yêu cầu | Header | HTTP khi thành công | Tên trường JSON của phản hồi thành công |
|---|---|---|---|---|
| `POST auth/v1/recover` | `{"email": "<email>"}` | `apikey` | 200 | thân rỗng |
| `POST auth/v1/verify` | `{"type": "recovery", "email": "<email>", "token": "<mã 6 chữ số>"}` | `apikey` | <mã> | <danh sách tên trường; token đã ẩn> | không làm được
| `PUT auth/v1/user` | `{"password": "<mật khẩu mới>"}` | `apikey`, `Authorization: Bearer <token đã ẩn>` | <mã> | <danh sách tên trường> | không làm được

Kết luận: khác kế hoạch ở điểm 2, 3.

## 3. CXM-03 — Mã HTTP và `error_code` thực tế

| Trường hợp | Điểm cuối | HTTP | `error_code` | Tên trường thông điệp lỗi |
|---|---|---|---|---|
| Mã sai | `POST auth/v1/verify` | <mã> | <chuỗi> | <tên> | không làm được
| Mã hết hạn | `POST auth/v1/verify` | <mã> | <chuỗi> | <tên> | không làm được 
| Mật khẩu trùng mật khẩu hiện tại | `PUT auth/v1/user` | 422 | New password should be different from the old password | same_password |
| Mật khẩu yếu | `PUT auth/v1/user` | 422 | Password should be at least 6 characters. | weak_password |
| Email chưa đăng ký | `POST auth/v1/recover` | 200 | không có | không có |
| Gọi lại trong 60 giây | `POST auth/v1/recover` | 403 | Token has expired or is invalid | otp_expired |


## 4. CXM-04 — Thời hạn mã thực tế (không làm được)

- Thời hạn cấu hình trên Dashboard: <số> giây.
- Đã chờ <số phút> kể từ lúc gửi: kết quả <mã hết hạn / mã còn hiệu lực>.
- Kết luận: <thời hạn thực tế>.


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
| Người nhận email với SMTP mặc định | <Chỉ thành viên organization / khác>; lỗi gặp khi dùng địa chỉ ngoài: `email_address_not_authorized` <nếu đã gặp> |

## 7. Bảng mã lỗi cần ánh xạ cho DP-2-V2

| Tình huống | HTTP thực tế | `error_code` thực tế | Dự kiến trong thiết kế | Khớp |
|---|---|---|---|---|
| Mã sai | <mã> | <chuỗi> | 403 `otp_expired` | Có |
| Mã hết hạn | <mã> | <chuỗi> | 403 `otp_expired` | <Có/Không> |
| Mật khẩu yếu | weak_password | Password should be at least 6 characters | 422 `weak_password` | Có |
| Mật khẩu mới trùng mật khẩu hiện tại | 422 | same_password | 422 `same_password` | Có |
| Giới hạn gửi email | 429 | over_email_send_rate_limit | 429 | <Có/Không> |

Tên trường thông điệp lỗi của Supabase: <tên>. `ErrorResponseDto` hiện đọc `error_code` và `message`.