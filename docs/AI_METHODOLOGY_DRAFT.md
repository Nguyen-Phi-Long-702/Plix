# AI Methodology — bản nháp (phân biệt ML cổ điển và LLM)

> Bản nháp Ngày 25, cập nhật Ngày 27. Chỉ mô tả những gì đã có trong code hoặc đã ghi trong kế hoạch; phần nào chưa làm thì ghi rõ là chưa triển khai.

## 1. Hai loại kỹ thuật AI trong dự án

| Tiêu chí | Machine Learning cổ điển (TF-IDF + Naive Bayes) | Mô hình ngôn ngữ lớn — LLM (Gemini) |
|---|---|---|
| Dùng để làm gì | Gợi ý danh mục giao dịch từ ghi chú (`POST /api/v1/categorize`, AI-01) và học lại từ các lần người dùng sửa gợi ý (`/correction` + `/retrain`, AI-02) | Chỉ là phương án dự phòng cho nhập nhanh bằng ngôn ngữ tự nhiên (`/parse-nl`, AI-07) khi parser tự viết có confidence thấp. Không tham gia phân loại danh mục |
| Trạng thái | Đã triển khai | Chưa triển khai; theo kế hoạch làm ở Day 44 (Could Have) |
| Ai viết và huấn luyện | Nhóm tự viết thuật toán (không dùng thư viện ML) và tự huấn luyện trên seed dataset `data/seed_categorization.csv` | Mô hình do Google huấn luyện sẵn, nhóm chỉ gọi qua API (Google AI Studio); nhóm không huấn luyện hay tinh chỉnh |
| Chạy ở đâu | Trên backend FastAPI của nhóm; tham số lưu ở bảng `ai_model_params` | Ở phía Google, được gọi từ backend; API key chỉ nằm ở backend |
| Dữ liệu người dùng | Ghi chú chỉ đi qua backend của nhóm | Chỉ gửi câu text người dùng nhập, không gửi lịch sử giao dịch; free tier có thể dùng dữ liệu prompt để cải thiện sản phẩm nên có toggle tắt |
| Cá nhân hoá | Mỗi người dùng có bộ tham số riêng; dùng model riêng khi `training_sample_count` ≥ 10, trước đó dùng model seed dùng chung (`user_id = '__seed__'`) | Không: mỗi lần gọi chỉ gửi câu người dùng vừa nhập |
| Đầu ra | Danh mục kèm `confidence` (normalized score, xem mục 3) | Văn bản JSON theo schema cố định; `ValidationService` kiểm tra schema và whitelist danh mục, loại item sai |
| Kiểm tra được bên trong | Có: công thức tường minh, tham số lưu trong database | Không: nhóm chỉ thấy đầu vào và đầu ra qua API |
| Giới hạn vận hành | Cần backend đang chạy (Render free tier có cold start) | Có quota; timeout ngắn (kế hoạch: khoảng 8 giây); lỗi 429 thì báo "AI đang bận, nhập tay"; chỉ dùng Gemini Flash hoặc Flash-Lite, không dùng Pro |

## 2. ML cổ điển trong dự án: phân loại danh mục

Code: `backend/app/services/ai/tfidf.py`, `naive_bayes.py`, `categorize_service.py`.

1. Tiền xử lý (`tokenize`): chuyển chữ thường, thay dấu câu bằng khoảng trắng, tách từ theo khoảng trắng.
2. Huấn luyện trên tập (ghi chú, danh mục):
   - TF-IDF: `TF = số lần từ xuất hiện trong note / tổng số từ của note`; `IDF = log(tổng số note / số note chứa từ đó)`. Khi huấn luyện, IDF được tính và lưu vào cột `idf` của `ai_model_params`.
   - Naive Bayes: `log-prior(category) = log(số note của category / tổng số note)`; `log-likelihood(từ, category) = log((số lần từ xuất hiện trong các note của category + 1) / (tổng số từ của category + kích thước vocabulary))` (Laplace smoothing +1).
3. Dự đoán (`NaiveBayesClassifier.predict`): bỏ các từ không có trong vocabulary; điểm của mỗi category = `log-prior + Σ log-likelihood(từ)` (mỗi lần từ xuất hiện đều được cộng); chọn category có điểm cao nhất.
4. Đổi tên category thành `category_id`: tra bảng `categories` trong danh sách hợp lệ của người dùng (category hệ thống và category riêng); không tìm thấy thì trả `category_id = null`.
5. Chọn model: dùng model riêng của người dùng nếu `training_sample_count` ≥ 10, ngược lại dùng model seed.

Ghi chú kỹ thuật: hiện `/categorize` gọi trực tiếp `NaiveBayesClassifier.predict()` trên số lần xuất hiện của từ; hàm `TfidfVectorizer.transform()` (trọng số TF-IDF) chưa được gọi ở bước dự đoán. TF-IDF hiện chỉ được tính và lưu khi huấn luyện.

## 3. Thuật ngữ `confidence`: normalized score, không phải calibrated probability

- Cách tính: điểm của từng category (`log-prior + Σ log-likelihood`) được đưa qua softmax thành các số trong khoảng 0–1 có tổng bằng 1; `confidence` là số của category được chọn.
- Đây là điểm chuẩn hoá (normalized score) để so sánh tương đối giữa các category, không phải xác suất đã hiệu chỉnh (calibrated probability). Nhóm không có bước hiệu chỉnh nào nên không thể nói "confidence 0,8 nghĩa là đúng khoảng 80%". Naive Bayes còn giả định các từ độc lập với nhau nên điểm số càng không nên đọc như xác suất thật.
- Cách viết trong báo cáo, README và giao diện: dùng "confidence" hoặc "điểm tin cậy tương đối", không viết "xác suất chính xác X%". Giao diện Android hiển thị confidence dạng phần trăm (ví dụ "Ăn uống · 62%") chỉ là cách định dạng số trong khoảng 0–1, không mang nghĩa xác suất đúng 62%.
- Ngưỡng 0,4: dưới ngưỡng này giao diện hiển thị "Độ tin cậy thấp" và không điền danh mục (để người dùng chọn tay). Đây là ngưỡng do nhóm chọn cho giao diện, không suy ra từ việc hiệu chỉnh xác suất.
- Ví dụ minh hoạ (model seed, tại thời điểm viết nháp): có 10 category, mỗi category có cùng số ví dụ nên prior bằng nhau. Một ghi chú không chứa từ nào có trong vocabulary khiến 10 category bằng điểm nhau, confidence ra đúng 0,1000. Con số này chỉ phản ánh việc chia đều, không phản ánh mức chắc chắn.

## 4. Đo chất lượng

- Chỉ số: accuracy = số ghi chú được dự đoán đúng danh mục / tổng số ghi chú kiểm tra, tính cả tổng thể lẫn theo từng danh mục.
- Các lần đo đã thực hiện (đều gọi thật `POST /api/v1/categorize`; lần đo của Vương gọi API trên Render, lần đo của Long gọi backend chạy local bằng uvicorn với Postgres Supabase thật):

| Lần đo | Seed | Tập kiểm tra (50 ghi chú, 5 ghi chú mỗi danh mục) | Kết quả | Nguồn |
|---|---|---|---|---|
| Ngày 25 | 500 dòng | `backend/data/eval_categorization_test.csv` | 40/50 = 80,0% | `backend/data/eval_categorization_result.md` |
| Ngày 25 | 500 dòng | `backend/data/test_categorization_holdout.csv` | 38/50 = 76,0% | `backend/docs/ngay25-ket-qua-do-chinh-xac.md` |
| Ngày 26 | 600 dòng (60 dòng mỗi danh mục) | `backend/data/test_categorization_holdout.csv` | 43/50 = 86,0% | `backend/docs/ngay26-xu-ly-van-de-phat-sinh.md` |

- Hai tập kiểm tra là hai tập khác nhau nên 80,0% và 76,0% không so sánh trực tiếp với nhau. Mỗi danh mục chỉ có 5 ghi chú nên chỉ cần sai 1 ghi chú là tỷ lệ theo danh mục đổi 20 điểm phần trăm.
- Sau khi seed tăng lên 600 dòng, 3 ghi chú của `eval_categorization_test.csv` (`mua nước lau kính`, `đi công viên nước`, `phí quản lý toà nhà`) trùng với ghi chú trong seed (so khớp nguyên ghi chú, bỏ khác biệt chữ hoa, chữ thường và dấu câu), nên tập này chỉ còn hợp lệ cho lần đo trên seed 500 dòng. Với cùng cách so khớp, `test_categorization_holdout.csv` không có ghi chú nào trùng với seed 600 dòng.
- Thời gian huấn luyện lại: đo bằng `python -m app.services.ai.benchmark_retrain` với 5000 giao dịch giả lập của 1 người dùng, chạy từ máy cá nhân tới Supabase thật (chưa đo trực tiếp trên Render). `retrain_user_model` mất 1,037 giây; ước tính một lần gọi `/retrain` khoảng 1,470 giây (chưa gồm xác thực JWT). Nguồn: `backend/docs/ngay26-xu-ly-van-de-phat-sinh.md`.

## 5. Huấn luyện lại (`/retrain`) và giới hạn tần suất

Code: `backend/app/routers/retrain.py`, `backend/app/services/ai/retrain_service.py`, `backend/app/core/rate_limit.py`.

- Dữ liệu huấn luyện: các giao dịch chưa xoá của người dùng có danh mục chưa xoá trên Postgres (ghi chú → tên danh mục). Model được huấn luyện lại từ đầu trên tập này rồi ghi đè `ai_model_params` của đúng người dùng đó.
- `training_sample_count` là số dòng `corrections` chưa xoá của người dùng, không phải số giao dịch.
- Giới hạn tần suất: dựa trên `trained_at` của model, mặc định 3600 giây (biến `RETRAIN_COOLDOWN_SECONDS`). Gọi sớm hơn trả 429 kèm số phút còn phải chờ.
- Khoá chống chạy đồng thời: cột `is_training`. Khi đang huấn luyện, request thứ hai trả 409.
- Người dùng chưa có giao dịch nào có danh mục trên máy chủ: trả 400 và không làm thay đổi `trained_at` nên không kích hoạt giới hạn tần suất.
- Mọi lỗi trả body `{error_code, message}`.