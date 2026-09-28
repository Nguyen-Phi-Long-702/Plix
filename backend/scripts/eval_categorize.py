import csv
import getpass
import json
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

from app.services.ai.tfidf import tokenize

SUPABASE_URL = "https://sfyrczbpecskjuyhctip.supabase.co"
SUPABASE_ANON_KEY = "sb_publishable_7SbslVyr_3iiPCXMaMxFLQ_f6hKNJuQ"
API_BASE_URL = "https://plix-7jfp.onrender.com"

DATA_DIR = Path(__file__).resolve().parents[1] / "data"
SEED_CSV = DATA_DIR / "seed_categorization.csv"
TEST_CSV = DATA_DIR / "eval_categorization_test.csv"
RESULT_MD = DATA_DIR / "eval_categorization_result.md"


def read_csv(path):
    with open(path, encoding="utf-8-sig", newline="") as csv_file:
        return list(csv.DictReader(csv_file))


def contains_phrase(long_tokens, short_tokens):
    """True nếu short_tokens xuất hiện thành cụm liền nhau trong long_tokens."""
    size = len(short_tokens)
    return size > 0 and any(
        long_tokens[i : i + size] == short_tokens
        for i in range(len(long_tokens) - size + 1)
    )


def check_test_set(seed_rows, test_rows):
    """Dừng ngay nếu tập test vi phạm yêu cầu của kế hoạch (note mới, không
    trùng seed): ghi chú nào trùng seed, chứa nguyên 1 ghi chú seed (hoặc nằm
    trọn trong 1 ghi chú seed) đều bị loại vì là gần-trùng, làm số liệu đẹp giả.
    Cũng dừng nếu tập test có ghi chú lặp hoặc nhãn không thuộc 10 category
    của seed."""
    if not test_rows:
        raise SystemExit("Tập test rỗng")
    if set(test_rows[0].keys()) != {"note", "category"}:
        raise SystemExit("File tập test phải có đúng 2 cột: note,category")
    seed_tokens = [tokenize(row["note"]) for row in seed_rows]
    seed_categories = {row["category"] for row in seed_rows}
    seen_notes = set()
    for row in test_rows:
        tokens = tokenize(row["note"])
        for seed_note in seed_tokens:
            if contains_phrase(tokens, seed_note) or contains_phrase(seed_note, tokens):
                raise SystemExit(
                    f"Ghi chú trùng hoặc gần trùng tập seed "
                    f"(chứa/nằm trong ghi chú seed {' '.join(seed_note)!r}): {row['note']!r}"
                )
        key = " ".join(tokens)
        if key in seen_notes:
            raise SystemExit(f"Ghi chú bị lặp trong tập test: {row['note']!r}")
        seen_notes.add(key)
        if row["category"] not in seed_categories:
            raise SystemExit(
                f"Nhãn không thuộc 10 category của seed: {row['category']!r} "
                f"(ghi chú {row['note']!r})"
            )


def call_api(method, url, headers=None, body=None, timeout=120):
    """Gọi HTTP, trả về JSON đã parse. Lỗi HTTP/kết nối -> dừng script và in
    rõ nguyên nhân (không đếm là 'dự đoán sai' để số liệu không bị méo)."""
    request_headers = dict(headers or {})
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        request_headers["Content-Type"] = "application/json"
    request = urllib.request.Request(
        url, data=data, headers=request_headers, method=method
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"{method} {url} -> HTTP {error.code}: {detail}")
    except OSError as error:
        raise SystemExit(f"{method} {url} -> lỗi kết nối: {error}")


def login(email, password):
    body = call_api(
        "POST",
        f"{SUPABASE_URL}/auth/v1/token?grant_type=password",
        headers={"apikey": SUPABASE_ANON_KEY},
        body={"email": email, "password": password},
    )
    return body["access_token"]


def build_report(results, run_time):
    total = len(results)
    correct = sum(1 for item in results if item["correct"])
    lines = [
        "# Kết quả đo độ chính xác `/api/v1/categorize` (Sprint 3, Ngày 25)",
        "",
        f"- Thời điểm chạy: {run_time:%Y-%m-%d %H:%M}",
        f"- Backend: {API_BASE_URL}",
        f"- Tập test: `data/eval_categorization_test.csv` ({total} ghi chú, không trùng tập seed)",
        "- Điều kiện hợp lệ: tài khoản đo chưa có model riêng "
        "(`training_sample_count` < 10) nên endpoint dùng model seed",
        "",
        "## Độ chính xác tổng thể",
        "",
        f"**{correct}/{total} = {correct / total * 100:.1f}%**",
        "",
        "## Theo từng danh mục",
        "",
        "| Danh mục | Đúng | Tổng | Tỷ lệ đúng |",
        "|---|---|---|---|",
    ]
    for category in dict.fromkeys(item["expected"] for item in results):
        items = [item for item in results if item["expected"] == category]
        ok = sum(1 for item in items if item["correct"])
        lines.append(f"| {category} | {ok} | {len(items)} | {ok / len(items) * 100:.0f}% |")
    lines += [
        "",
        "## Chi tiết từng ghi chú",
        "",
        "| # | Ghi chú | Kỳ vọng | Dự đoán | Confidence (normalized score) | Kết quả |",
        "|---|---|---|---|---|---|",
    ]
    for index, item in enumerate(results, start=1):
        verdict = "Đúng" if item["correct"] else "Sai"
        lines.append(
            f"| {index} | {item['note']} | {item['expected']} | "
            f"{item['predicted']} | {item['confidence']:.4f} | {verdict} |"
        )
    return "\n".join(lines) + "\n"


def main():
    seed_rows = read_csv(SEED_CSV)
    test_rows = read_csv(TEST_CSV)
    check_test_set(seed_rows, test_rows)
    print(f"Tập test hợp lệ: {len(test_rows)} ghi chú, không trùng/gần trùng seed.")

    email = input("Email tài khoản dùng để đo: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    token = login(email, password)
    auth_headers = {"Authorization": f"Bearer {token}"}

    print("Làm nóng backend (/health, Render free tier có thể mất 30-60 giây)...")
    call_api("GET", f"{API_BASE_URL}/api/v1/health")

    whoami = call_api("GET", f"{API_BASE_URL}/api/v1/whoami", headers=auth_headers)
    print(f"user_id của tài khoản đo: {whoami['user_id']}")

    categories = call_api(
        "GET", f"{API_BASE_URL}/api/v1/categories", headers=auth_headers
    )
    name_by_id = {category["id"]: category["name"] for category in categories}
    missing = {row["category"] for row in test_rows} - set(name_by_id.values())
    if missing:
        raise SystemExit(
            f"Backend không có category tên: {sorted(missing)} — "
            "tên trong Postgres không khớp CSV, số liệu sẽ sai"
        )

    results = []
    for index, row in enumerate(test_rows, start=1):
        print(f"[{index}/{len(test_rows)}] {row['note']}")
        body = call_api(
            "POST",
            f"{API_BASE_URL}/api/v1/categorize",
            headers=auth_headers,
            body={"note": row["note"]},
        )
        category_id = body["category_id"]
        if category_id is None:
            predicted = "(category_id = null)"
        else:
            predicted = name_by_id.get(category_id, f"(id lạ: {category_id})")
        results.append(
            {
                "note": row["note"],
                "expected": row["category"],
                "predicted": predicted,
                "confidence": body["confidence"],
                "correct": predicted == row["category"],
            }
        )

    report = build_report(results, datetime.now())
    RESULT_MD.write_text(report, encoding="utf-8")
    print()
    print(report)
    print(f"Đã ghi log kết quả vào {RESULT_MD}")


if __name__ == "__main__":
    main()