# Bản nháp: Phân biệt thuật ngữ AI dùng trong dự án — Ngày 27

**Người viết:** Nguyễn Phi Long & Nguyễn Đại Vương

## 1. Vì sao cần tài liệu này

Dự án dùng từ "AI" cho nhiều kỹ thuật khác nhau về bản chất. Để tránh gọi mọi
thuật toán thống kê là "AI" một cách mơ hồ trong báo cáo, tài liệu này phân
biệt rõ 2 nhóm kỹ thuật đang dùng trong sprint 3.

## 2. Machine Learning cổ điển — dùng cho `/categorize`, `/correction`, `/retrain`

- **Thuật toán:** TF-IDF (tự viết công thức) + Naive Bayes (tự viết công thức),
  KHÔNG dùng thư viện ML có sẵn, KHÔNG phải mô hình ngôn ngữ lớn (LLM).
- **Cách hoạt động:** vector hoá note bằng TF-IDF theo vocabulary xây từ tập
  training của từng user; Naive Bayes tính log-prior + log-likelihood (có
  Laplace smoothing) để chọn category có tổng điểm cao nhất.
- **Huấn luyện riêng theo từng user:** tham số lưu trong `ai_model_params`
  với PK là `user_id`; model mặc định dùng chung lưu ở sentinel `user_id='__seed__'`.
- **"Confidence" là gì:** là **normalized score** (softmax của các điểm log),
  KHÔNG phải xác suất đã hiệu chỉnh (calibrated probability) theo nghĩa thống
  kê chặt chẽ. Báo cáo/README phải dùng đúng thuật ngữ "điểm tin cậy tương đối",
  không viết "xác suất chính xác X%".

## 3. Mô hình ngôn ngữ lớn (LLM) — dự kiến dùng cho `/parse-nl` fallback (sprint 5)

- **Mô hình:** Gemini API (dịch vụ ngoài, gọi qua HTTP, không tự huấn luyện).
- **Khác biệt cốt lõi với ML cổ điển ở Mục 2:**
  - ML cổ điển: tham số tự tính từ dữ liệu của chính user đó, chạy hoàn toàn
    trong backend của dự án, không gọi dịch vụ ngoài.
  - LLM: mô hình đã được huấn luyện sẵn quy mô lớn bởi bên thứ ba (Google),
    dự án chỉ gọi API, không có quyền truy cập/điều chỉnh tham số bên trong.
- **Phạm vi dùng trong dự án:** chỉ làm fallback khi parser rule-based tự viết
  (`/parse-nl`) không xử lý được hoặc confidence thấp — không dùng LLM cho
  `/categorize`.

## 4. Tóm tắt bảng phân loại

| Endpoint | Kỹ thuật | Loại |
|---|---|---|
| `/categorize`, `/retrain` | TF-IDF + Naive Bayes tự viết | Machine Learning cổ điển |
| `/anomaly` | mean/std hoặc IQR theo ngưỡng | Thống kê thuần (không phải ML) |
| `/forecast` | Exponential smoothing / moving average | Thống kê thuần (không phải ML) |
| `/parse-nl` (chính) | Rule-based: regex, từ điển từ khoá | Thuật toán tất định, không phải ML/LLM |
| `/parse-nl` (fallback, Could Have) | Gemini API | LLM (mô hình ngôn ngữ lớn) |

*Ghi chú: `/anomaly`, `/forecast`, `/parse-nl` (phần rule-based) thuộc Sprint 5,
đưa vào bảng này chỉ để tài liệu phân biệt được đầy đủ nhất quán cho báo cáo —
Ngày 27 không code các endpoint này.*