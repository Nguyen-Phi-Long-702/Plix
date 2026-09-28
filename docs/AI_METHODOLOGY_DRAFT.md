# AI Methodology — bản nháp (phân biệt ML cổ điển và LLM)

> Bản nháp Ngày 25. Chỉ mô tả những gì đã có trong code hoặc đã ghi trong kế hoạch; phần nào chưa làm thì ghi rõ là chưa triển khai.

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
- Tập kiểm tra: `backend/data/eval_categorization_test.csv` (50 ghi chú mới, không trùng và không chứa nguyên ghi chú nào của seed). Số liệu đo thật: `backend/data/eval_categorization_result.md`.
- Lưu ý: mỗi danh mục chỉ có ít ghi chú kiểm tra nên tỷ lệ theo danh mục dao động mạnh (chỉ cần sai 1 ghi chú là tỷ lệ đổi nhiều).