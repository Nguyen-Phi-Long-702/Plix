import getpass
import statistics
import time
from datetime import datetime

from scripts.eval_categorize import API_BASE_URL, call_api, login

# Ngày 50: đo thời gian phản hồi /categorize và /sync/transactions/pull trên Render
# sau khi đã làm nóng bằng /health, mục tiêu dưới 3 giây. Benchmark cơ bản, không phải load test.
RUN_COUNT = 5
LIMIT_MS = 3000.0
CATEGORIZE_NOTE = "cà phê"
PULL_LIMIT = 500


def measure(call):
    """Gọi 1 lần khởi động (không tính), rồi đo RUN_COUNT lần.
    Trả về (danh sách thời gian từng lần, đơn vị ms; kết quả của lần gọi cuối).
    Lỗi HTTP/kết nối làm call_api dừng script nên không bị tính là 1 lần đo."""
    call()
    runs_ms = []
    result = None
    for _ in range(RUN_COUNT):
        start = time.perf_counter()
        result = call()
        runs_ms.append((time.perf_counter() - start) * 1000)
    return runs_ms, result


def format_row(name, condition, runs_ms):
    verdict = "ĐẠT" if max(runs_ms) < LIMIT_MS else "KHÔNG ĐẠT"
    return (
        f"| {name} | {condition} | {min(runs_ms):.1f} | "
        f"{statistics.median(runs_ms):.1f} | {max(runs_ms):.1f} | < {LIMIT_MS:.0f} ms | {verdict} |"
    )


def main():
    email = input("Email tài khoản dùng để đo: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    token = login(email, password)
    auth_headers = {"Authorization": f"Bearer {token}"}

    print("Làm nóng backend (/health, Render có thể mất 30-60 giây)...")
    start = time.perf_counter()
    call_api("GET", f"{API_BASE_URL}/api/v1/health")
    print(f"/health xong sau {(time.perf_counter() - start) * 1000:.0f} ms (không tính vào kết quả)")

    print(f"Đo POST /api/v1/categorize ({RUN_COUNT} lần)...")
    categorize_runs, _ = measure(
        lambda: call_api(
            "POST",
            f"{API_BASE_URL}/api/v1/categorize",
            headers=auth_headers,
            body={"note": CATEGORIZE_NOTE},
        )
    )

    print(f"Đo GET /api/v1/sync/transactions/pull ({RUN_COUNT} lần)...")
    pull_runs, pull_body = measure(
        lambda: call_api(
            "GET",
            f"{API_BASE_URL}/api/v1/sync/transactions/pull?since=0&limit={PULL_LIMIT}",
            headers=auth_headers,
        )
    )
    record_count = len(pull_body["records"])

    print()
    print(f"Thời điểm chạy: {datetime.now():%Y-%m-%d %H:%M}")
    print()
    print("| Endpoint | Điều kiện đo | Nhỏ nhất (ms) | Trung vị (ms) | Lớn nhất (ms) | Mục tiêu | Kết quả |")
    print("|---|---|---|---|---|---|---|")
    print(format_row("`POST /api/v1/categorize`", f"ghi chú \"{CATEGORIZE_NOTE}\"", categorize_runs))
    print(
        format_row(
            "`GET /api/v1/sync/transactions/pull`",
            f"since=0, limit={PULL_LIMIT}, trả về {record_count} bản ghi",
            pull_runs,
        )
    )


if __name__ == "__main__":
    main()