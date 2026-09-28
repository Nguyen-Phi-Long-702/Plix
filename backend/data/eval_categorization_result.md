# Kết quả đo độ chính xác `/api/v1/categorize` (Sprint 3, Ngày 25)

- Thời điểm chạy: 2026-09-28 16:23
- Backend: https://plix-7jfp.onrender.com
- Tập test: `data/eval_categorization_test.csv` (50 ghi chú, không trùng tập seed)
- Điều kiện hợp lệ: tài khoản đo chưa có model riêng (`training_sample_count` < 10) nên endpoint dùng model seed

## Độ chính xác tổng thể

**40/50 = 80.0%**

## Theo từng danh mục

| Danh mục | Đúng | Tổng | Tỷ lệ đúng |
|---|---|---|---|
| Di chuyển | 2 | 5 | 40% |
| Ăn uống | 4 | 5 | 80% |
| Nhu yếu phẩm | 4 | 5 | 80% |
| Hoá đơn (điện/nước/internet) | 5 | 5 | 100% |
| Giải trí | 4 | 5 | 80% |
| Sức khoẻ | 3 | 5 | 60% |
| Giáo dục | 5 | 5 | 100% |
| Nhà ở/Thuê nhà | 4 | 5 | 80% |
| Mua sắm | 4 | 5 | 80% |
| Lương | 5 | 5 | 100% |

## Chi tiết từng ghi chú

| # | Ghi chú | Kỳ vọng | Dự đoán | Confidence (normalized score) | Kết quả |
|---|---|---|---|---|---|
| 1 | tiền xăng đi làm | Di chuyển | Lương | 0.6245 | Sai |
| 2 | đặt xe công nghệ về quê | Di chuyển | Di chuyển | 0.7883 | Đúng |
| 3 | đặt vé đi tàu hoả ra Huế | Di chuyển | Giải trí | 0.7002 | Sai |
| 4 | đi thay dầu nhớt cho xe tay ga | Di chuyển | Di chuyển | 0.9881 | Đúng |
| 5 | phí BOT quốc lộ 1 | Di chuyển | Giáo dục | 0.3974 | Sai |
| 6 | ăn trưa với đồng nghiệp | Ăn uống | Ăn uống | 0.4843 | Đúng |
| 7 | bún đậu mắm tôm | Ăn uống | Ăn uống | 0.3085 | Đúng |
| 8 | một ly trà vải | Ăn uống | Ăn uống | 0.3803 | Đúng |
| 9 | cơm hộp buổi tối | Ăn uống | Ăn uống | 0.6649 | Đúng |
| 10 | ăn tối nhà hàng hải sản | Ăn uống | Lương | 0.2950 | Sai |
| 11 | đi chợ mua đồ ăn cho cả tuần | Nhu yếu phẩm | Mua sắm | 0.4362 | Sai |
| 12 | mua nước lau kính | Nhu yếu phẩm | Nhu yếu phẩm | 0.6537 | Đúng |
| 13 | mua nước rửa bát | Nhu yếu phẩm | Nhu yếu phẩm | 0.8985 | Đúng |
| 14 | mua hành lá và ớt | Nhu yếu phẩm | Nhu yếu phẩm | 0.4762 | Đúng |
| 15 | mua tã giấy cho em bé | Nhu yếu phẩm | Nhu yếu phẩm | 0.6126 | Đúng |
| 16 | thanh toán điện tháng 9 | Hoá đơn (điện/nước/internet) | Hoá đơn (điện/nước/internet) | 0.9872 | Đúng |
| 17 | wifi nhà tháng này | Hoá đơn (điện/nước/internet) | Hoá đơn (điện/nước/internet) | 0.8312 | Đúng |
| 18 | nộp nước sinh hoạt tháng này | Hoá đơn (điện/nước/internet) | Hoá đơn (điện/nước/internet) | 0.9214 | Đúng |
| 19 | gia hạn data 4g tháng 9 | Hoá đơn (điện/nước/internet) | Hoá đơn (điện/nước/internet) | 0.8555 | Đúng |
| 20 | tiền truyền hình k+ | Hoá đơn (điện/nước/internet) | Hoá đơn (điện/nước/internet) | 0.8878 | Đúng |
| 21 | cả nhóm đi rạp cuối tuần | Giải trí | Giải trí | 0.6450 | Đúng |
| 22 | đăng ký gói phim online tháng này | Giải trí | Hoá đơn (điện/nước/internet) | 0.4253 | Sai |
| 23 | vé vào cổng công viên giải trí | Giải trí | Giải trí | 0.9242 | Đúng |
| 24 | đi công viên nước | Giải trí | Giải trí | 0.6198 | Đúng |
| 25 | đêm nhạc cuối tuần với bạn | Giải trí | Giải trí | 0.2514 | Đúng |
| 26 | lấy cao răng | Sức khoẻ | Nhu yếu phẩm | 0.2194 | Sai |
| 27 | thuốc đau đầu | Sức khoẻ | Sức khoẻ | 0.7333 | Đúng |
| 28 | đi phòng khám tư | Sức khoẻ | Sức khoẻ | 0.8675 | Đúng |
| 29 | tiêm ngừa uốn ván | Sức khoẻ | Sức khoẻ | 0.3050 | Đúng |
| 30 | viên uống vitamin c | Sức khoẻ | Lương | 0.2027 | Sai |
| 31 | đóng tiền học kỳ này | Giáo dục | Giáo dục | 0.7489 | Đúng |
| 32 | sách luyện đề ielts | Giáo dục | Giáo dục | 0.7367 | Đúng |
| 33 | photo đề cương ôn thi cuối kỳ | Giáo dục | Giáo dục | 0.9042 | Đúng |
| 34 | đăng ký khoá học tiếng nhật online | Giáo dục | Giáo dục | 0.9967 | Đúng |
| 35 | sắm vở và bút cho năm học mới | Giáo dục | Giáo dục | 0.8965 | Đúng |
| 36 | trả tiền phòng tháng 10 | Nhà ở/Thuê nhà | Nhà ở/Thuê nhà | 0.7804 | Đúng |
| 37 | đặt cọc căn hộ mới | Nhà ở/Thuê nhà | Nhà ở/Thuê nhà | 0.9908 | Đúng |
| 38 | phí quản lý toà nhà | Nhà ở/Thuê nhà | Nhà ở/Thuê nhà | 0.9680 | Đúng |
| 39 | sửa vòi nước phòng trọ | Nhà ở/Thuê nhà | Nhà ở/Thuê nhà | 0.9634 | Đúng |
| 40 | tháng này đóng tiền trọ | Nhà ở/Thuê nhà | Hoá đơn (điện/nước/internet) | 0.8335 | Sai |
| 41 | mua áo thun mới | Mua sắm | Mua sắm | 0.6566 | Đúng |
| 42 | đôi giày sneaker trắng | Mua sắm | Mua sắm | 0.2546 | Đúng |
| 43 | săn sale áo khoác | Mua sắm | Mua sắm | 0.6545 | Đúng |
| 44 | mua quà sinh nhật cho bạn | Mua sắm | Nhu yếu phẩm | 0.3763 | Sai |
| 45 | mua kính râm | Mua sắm | Mua sắm | 0.4967 | Đúng |
| 46 | lương về tài khoản hôm nay | Lương | Lương | 0.8568 | Đúng |
| 47 | công làm thêm cuối tuần | Lương | Lương | 0.9740 | Đúng |
| 48 | lương dạy kèm toán | Lương | Lương | 0.9408 | Đúng |
| 49 | công ty trả lương | Lương | Lương | 0.9922 | Đúng |
| 50 | được thưởng cuối quý | Lương | Lương | 0.5523 | Đúng |
