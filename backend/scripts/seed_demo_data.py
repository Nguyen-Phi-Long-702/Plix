"""Nạp dữ liệu demo (Ngày 57) cho TÀI KHOẢN DEMO riêng, qua chính API sync của backend.

Chạy từ thư mục backend:  python -m scripts.seed_demo_data
Chạy lại nhiều lần được: id cố định (demo-...) nên chỉ ghi đè, không nhân đôi dữ liệu.
Ngày giờ giao dịch tính lùi từ lúc chạy script: 56 ngày gần nhất (8 tuần).
"""
import getpass
import random
from datetime import datetime, timedelta

from app.services.ai.anomaly_stats import MIN_SAMPLES_FOR_MEAN_STD, STATUS_NORMAL
from app.services.ai.forecast import MIN_WEEKS_OF_HISTORY, VN_TZ, weekly_net_series
from scripts.eval_categorize import API_BASE_URL, SEED_CSV, call_api, login, read_csv

RANDOM_SEED = 57
HISTORY_DAYS = 56
SALARY_AMOUNT = 15_000_000

# 2 danh mục tự tạo để demo Quản lý danh mục: sửa tên "Quà tặng", xoá "Thú cưng".
CUSTOM_CATEGORIES = [
    {"id": "demo-cat-thu-cung", "name": "Thú cưng", "type": "expense"},
    {"id": "demo-cat-qua-tang", "name": "Quà tặng", "type": "expense"},
]

# (category_id, tên trong seed_categorization.csv hoặc None, số giao dịch, chi thấp nhất, chi cao nhất)
# Mỗi danh mục chi >= 10 giao dịch để /anomaly dùng mean + std thay vì báo "chưa đủ dữ liệu".
EXPENSE_PLANS = [
    ("sys_an_uong", "Ăn uống", 14, 25_000, 120_000),
    ("sys_di_chuyen", "Di chuyển", 12, 20_000, 150_000),
    ("sys_nhu_yeu_pham", "Nhu yếu phẩm", 10, 100_000, 300_000),
    ("sys_giai_tri", "Giải trí", 10, 50_000, 250_000),
    ("sys_hoa_don", "Hoá đơn (điện/nước/internet)", 10, 100_000, 500_000),
    ("sys_suc_khoe", "Sức khoẻ", 10, 80_000, 400_000),
    ("sys_giao_duc", "Giáo dục", 10, 150_000, 600_000),
    ("sys_nha_o", "Nhà ở/Thuê nhà", 10, 500_000, 1_500_000),
    ("sys_mua_sam", "Mua sắm", 10, 100_000, 700_000),
    ("sys_khac_chi", None, 10, 20_000, 200_000),
    ("demo-cat-thu-cung", None, 10, 50_000, 400_000),
    ("demo-cat-qua-tang", None, 10, 100_000, 600_000),
]

# Ghi chú cho các danh mục không có trong seed_categorization.csv.
EXTRA_NOTES = {
    "sys_khac_chi": [
        "phí chuyển khoản", "phí ngân hàng", "gửi bưu phẩm", "đóng quỹ lớp", "phí dịch vụ",
        "tiền lẻ", "ủng hộ từ thiện", "phí làm giấy tờ", "đồ dùng linh tinh", "phí thẻ",
    ],
    "demo-cat-thu-cung": [
        "thức ăn cho mèo", "cát vệ sinh mèo", "tiêm phòng cho mèo", "đồ chơi cho mèo", "khám thú y",
        "hạt cho chó", "tắm spa cho chó", "vòng cổ cho chó", "vitamin cho thú cưng", "phụ kiện thú cưng",
    ],
    "demo-cat-qua-tang": [
        "quà sinh nhật bạn", "quà sinh nhật mẹ", "hoa tặng bạn gái", "quà cưới", "quà tân gia",
        "quà 20/10", "quà Tết", "quà cảm ơn thầy cô", "quà kỷ niệm", "phong bì mừng",
    ],
}
SIDE_INCOME_NOTES = [
    "bán đồ cũ", "dạy kèm", "làm thêm cuối tuần", "thưởng dự án",
    "hoàn tiền", "freelance thiết kế", "bán sách cũ", "nhận lại tiền cho mượn",
]
EXPENSE_PAYMENT_METHODS = ["cash", "e_wallet", "bank_transfer"]


def to_ms(moment):
    return round(moment.timestamp() * 1000)


def build_transactions(now, seed_notes):
    """Sinh toàn bộ giao dịch demo (cố định theo RANDOM_SEED, chỉ ngày giờ phụ thuộc lúc chạy)."""
    rng = random.Random(RANDOM_SEED)
    updated_at = to_ms(now)
    transactions = []

    def add(tx_id, tx_type, category_id, amount, days_back, note, payment_method):
        moment = (now - timedelta(days=days_back)).replace(
            hour=rng.randint(7, 21), minute=rng.randint(0, 59), second=0, microsecond=0
        )
        transactions.append({
            "id": tx_id,
            "updated_at": updated_at,
            "is_deleted": False,
            "amount": amount,
            "type": tx_type,
            "category_id": category_id,
            "note": note,
            "payment_method": payment_method,
            "occurred_at": to_ms(moment),
            "is_recurring": False,
        })

    for category_id, seed_name, count, low, high in EXPENSE_PLANS:
        notes = EXTRA_NOTES[category_id] if seed_name is None else seed_notes[seed_name][:count]
        for index in range(count):
            add(
                f"demo-tx-{category_id}-{index + 1:02d}", "expense", category_id,
                rng.randrange(low, high + 1, 1000), rng.randint(1, HISTORY_DAYS),
                notes[index], rng.choice(EXPENSE_PAYMENT_METHODS),
            )

    # Thu nhập: 2 kỳ lương cách nhau 30 ngày + 1 khoản thu nhập khác mỗi tuần (8 tuần).
    for index, days_back in enumerate((5, 35)):
        add(f"demo-tx-sys_luong-{index + 1:02d}", "income", "sys_luong", SALARY_AMOUNT,
            days_back, seed_notes["Lương"][index], "bank_transfer")
    for week, note in enumerate(SIDE_INCOME_NOTES):
        add(f"demo-tx-sys_thu_nhap_khac-{week + 1:02d}", "income", "sys_thu_nhap_khac",
            rng.randrange(200_000, 800_001, 1000), 7 * week + rng.randint(1, 7), note, "e_wallet")
    return transactions


def week_start(occurred_at_ms):
    day = datetime.fromtimestamp(occurred_at_ms / 1000, VN_TZ).date()
    return day - timedelta(days=day.weekday())


def check_locally(transactions, now):
    """Dừng ngay nếu dữ liệu sinh ra không đạt yêu cầu của Ngày 57 (kiểm tra bằng đúng hàm của backend)."""
    print("== Kiểm tra cục bộ ==")
    amounts_by_category = {}
    for tx in transactions:
        if tx["type"] == "expense":
            amounts_by_category.setdefault(tx["category_id"], []).append(tx["amount"])
    for category_id, amounts in amounts_by_category.items():
        if len(amounts) < MIN_SAMPLES_FOR_MEAN_STD:
            raise SystemExit(f"Danh mục {category_id} chỉ có {len(amounts)} giao dịch, chưa đủ 10")
        print(f"  {category_id}: {len(amounts)} giao dịch chi")

    now_ms = to_ms(now)
    series = weekly_net_series([(tx["type"], tx["amount"], tx["occurred_at"]) for tx in transactions], now_ms)
    current_week = week_start(now_ms)
    income_weeks = {week_start(tx["occurred_at"]) for tx in transactions if tx["type"] == "income"}
    expense_weeks = {week_start(tx["occurred_at"]) for tx in transactions if tx["type"] == "expense"}
    both_weeks = sorted(week for week in income_weeks & expense_weeks if week < current_week)
    print(f"  Số tuần trọn vẹn: {len(series)}; số tuần có cả thu và chi: {len(both_weeks)}")
    print(f"  NET theo tuần (cũ -> mới): {series}")
    if len(series) < MIN_WEEKS_OF_HISTORY or len(both_weeks) < MIN_WEEKS_OF_HISTORY:
        raise SystemExit(f"Cần ít nhất {MIN_WEEKS_OF_HISTORY} tuần trọn vẹn có cả thu và chi để dự báo")


def push(table, records, headers):
    result = call_api(
        "POST", f"{API_BASE_URL}/api/v1/sync/{table}/push", headers=headers, body={"records": records}
    )
    if result["rejected"] or len(result["upserted_ids"]) != len(records):
        raise SystemExit(f"Push {table} không trọn vẹn: {result}")
    print(f"  Đã đẩy {len(records)} bản ghi {table}")


def check_on_server(transactions, headers):
    """Hỏi lại backend thật (đường dữ liệu mà app sẽ dùng) để chắc chắn không còn 'chưa đủ dữ liệu'."""
    print("== Kiểm tra trên backend thật ==")
    amounts_by_category = {}
    for tx in transactions:
        if tx["type"] == "expense":
            amounts_by_category.setdefault(tx["category_id"], []).append(tx["amount"])
    for category_id, amounts in amounts_by_category.items():
        middle = sorted(amounts)[len(amounts) // 2]
        body = call_api(
            "GET", f"{API_BASE_URL}/api/v1/anomaly?category_id={category_id}&amount={middle}", headers=headers
        )
        print(f"  /anomaly {category_id}: {body['status']}")
        if body["status"] != STATUS_NORMAL:
            raise SystemExit(f"/anomaly cho {category_id} không trả 'normal': {body}")
    forecast = call_api("GET", f"{API_BASE_URL}/api/v1/forecast", headers=headers)
    print(f"  /forecast: {forecast}")
    if forecast["status"] != "ok":
        raise SystemExit(f"/forecast chưa đủ dữ liệu: {forecast}")


def main():
    email = input("Email TÀI KHOẢN DEMO: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    token = login(email, password)
    headers = {"Authorization": f"Bearer {token}"}

    now = datetime.now(VN_TZ)
    seed_notes = {}
    for row in read_csv(SEED_CSV):
        seed_notes.setdefault(row["category"], []).append(row["note"])
    transactions = build_transactions(now, seed_notes)
    check_locally(transactions, now)

    print("Làm nóng backend (/health, Render free tier có thể mất 30-60 giây)...")
    call_api("GET", f"{API_BASE_URL}/api/v1/health")

    categories = [
        {"id": c["id"], "updated_at": to_ms(now), "is_deleted": False, "name": c["name"], "type": c["type"]}
        for c in CUSTOM_CATEGORIES
    ]
    print("== Đẩy dữ liệu lên backend ==")
    push("categories", categories, headers)
    push("transactions", transactions, headers)
    check_on_server(transactions, headers)
    print("Xong. Trên điện thoại: Đăng xuất rồi Đăng nhập lại tài khoản demo để app kéo dữ liệu về.")


if __name__ == "__main__":
    main()