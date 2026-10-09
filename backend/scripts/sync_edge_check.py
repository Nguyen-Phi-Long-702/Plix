import getpass
import time
import urllib.error
import urllib.request
import uuid

from scripts.eval_categorize import call_api, login
from scripts.sync_functional_check import (
    DEFAULT_BASE_URL,
    TABLES,
    check,
    make_record,
    push,
)

MAX_PAGES = 50  # chặn vòng lặp vô hạn khi kéo nhiều lô
EXPECTED_MAX_PULL_LIMIT = 500  # MAX_PULL_LIMIT mặc định của backend
BIG_COUNT = EXPECTED_MAX_PULL_LIMIT + 1
PUSH_CHUNK = 250  # số bản ghi mỗi lần đẩy, giữ body nhỏ hơn giới hạn 1MB
BAD_TOKEN_HEADERS = {"Authorization": "Bearer khong.phai.jwt"}


def pull_page(base_url, headers, table, since, limit):
    return call_api(
        "GET",
        f"{base_url}/api/v1/sync/{table}/pull?since={since}&limit={limit}",
        headers=headers,
    )


def pull_all(base_url, headers, table, since, limit=500):
    """Kéo giống client: lặp theo has_more, since mới = updated_at lớn nhất của lô
    trước. Vì so sánh `>=` nên bản ghi cuối lô trước có thể xuất hiện lại ở lô sau;
    gom theo id (giống việc ghi vào Room theo id). Trả về dict id -> bản ghi."""
    by_id = {}
    pages = 0
    while pages < MAX_PAGES:
        body = pull_page(base_url, headers, table, since, limit)
        pages += 1
        for item in body["records"]:
            by_id[item["id"]] = item
        if not body["has_more"] or not body["records"]:
            return by_id
        since = max(item["updated_at"] for item in body["records"])
    raise SystemExit(f"Kéo bảng {table} quá {MAX_PAGES} lô mà server vẫn báo còn dữ liệu")


def status_of_get(url, headers):
    request = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code
    except OSError as error:
        raise SystemExit(f"GET {url} -> lỗi kết nối: {error}")


def new_id():
    return f"ngay36-{uuid.uuid4()}"


class Run:
    """Một lần chạy: nhớ updated_at lớn nhất đã đẩy của từng bản ghi để cuối cùng
    đánh dấu xoá (tombstone) đúng chính sách, không xoá vật lý."""

    def __init__(self, base_url, headers, base):
        self.base_url = base_url
        self.headers = headers
        self.base = base  # epoch ms lúc bắt đầu; mọi updated_at của bản test đều lớn hơn base
        self.latest = {}  # (bảng, id) -> updated_at lớn nhất đã đẩy

    def push(self, table, records):
        for record in records:
            key = (table, record["id"])
            self.latest[key] = max(self.latest.get(key, 0), record["updated_at"])
        return push(self.base_url, self.headers, table, records)

    def pull_all(self, table, since, limit=500):
        return pull_all(self.base_url, self.headers, table, since, limit)

    def pull_page(self, table, since, limit=500):
        return pull_page(self.base_url, self.headers, table, since, limit)

    def cleanup(self):
        for table in TABLES:
            ids = [key[1] for key in self.latest if key[0] == table]
            for start in range(0, len(ids), PUSH_CHUNK):
                chunk = ids[start : start + PUSH_CHUNK]
                tombstones = [
                    make_record(table, record_id, self.latest[(table, record_id)] + 1000, variant="b", is_deleted=True)
                    for record_id in chunk
                ]
                try:
                    push(self.base_url, self.headers, table, tombstones)
                except BaseException as error:  # SystemExit từ call_api cũng phải bắt lại
                    print(f"Không đánh dấu xoá được {len(chunk)} bản ghi bảng {table}: {error}")


def case_reinstall(run, table):
    """G1. Cài lại app + đăng nhập: Room trống, con trỏ = 0 -> kéo về đủ dữ liệu,
    bản đã xoá vẫn là tombstone (không hiện lại)."""
    live_id, deleted_id = new_id(), new_id()
    live = make_record(table, live_id, run.base + 1000)
    run.push(table, [live, make_record(table, deleted_id, run.base + 1000)])
    tombstone = make_record(table, deleted_id, run.base + 2000, variant="b", is_deleted=True)
    run.push(table, [tombstone])

    pulled = run.pull_all(table, 0)
    return [check(
        f"{table} | G1 cài lại app + đăng nhập: kéo từ con trỏ 0 về đủ dữ liệu, bản đã xoá vẫn là tombstone",
        pulled.get(live_id) == live and pulled.get(deleted_id) == tombstone,
        f"còn sống={pulled.get(live_id)}, đã xoá={pulled.get(deleted_id)}",
    )]


def case_second_device(run, table):
    """G2. Đăng nhập thiết bị thứ 2 (cùng tài khoản, con trỏ = 0), rồi thiết bị 2
    sửa/thêm bản ghi; thiết bị 1 (con trỏ cũ) kéo về thấy thay đổi."""
    a_id, b_id = new_id(), new_id()
    original = make_record(table, a_id, run.base + 1000)
    run.push(table, [original])
    cursor_of_device_1 = original["updated_at"]

    pulled_by_device_2 = run.pull_all(table, 0)
    edited = make_record(table, a_id, run.base + 2000, variant="b")
    created_by_device_2 = make_record(table, b_id, run.base + 2000)
    run.push(table, [edited, created_by_device_2])
    pulled_by_device_1 = run.pull_all(table, cursor_of_device_1)

    return [
        check(
            f"{table} | G2a thiết bị 2 đăng nhập lần đầu: kéo về đúng dữ liệu thiết bị 1 đã đẩy",
            pulled_by_device_2.get(a_id) == original,
            pulled_by_device_2.get(a_id),
        ),
        check(
            f"{table} | G2b thiết bị 1 kéo theo con trỏ cũ: nhận bản sửa và bản mới từ thiết bị 2",
            pulled_by_device_1.get(a_id) == edited and pulled_by_device_1.get(b_id) == created_by_device_2,
            f"bản sửa={pulled_by_device_1.get(a_id)}, bản mới={pulled_by_device_1.get(b_id)}",
        ),
    ]


def case_error_keeps_data(run, table):
    """G3. Phía server: lần kéo lỗi (token sai -> 401) không tiêu hao dữ liệu;
    kéo lại cùng `since` trả đúng như trước (con trỏ nằm ở client, không tiến khi lỗi)."""
    record = make_record(table, new_id(), run.base + 1000)
    run.push(table, [record])

    before = run.pull_page(table, run.base)
    status = status_of_get(
        f"{run.base_url}/api/v1/sync/{table}/pull?since={run.base}&limit=500", BAD_TOKEN_HEADERS
    )
    after = run.pull_page(table, run.base)
    return [check(
        f"{table} | G3 kéo lỗi (token sai -> 401) rồi kéo lại cùng since: dữ liệu y như trước",
        status == 401 and after == before and record in after["records"],
        f"status={status}, giống trước={after == before}",
    )]


def case_partial_pull(run, table):
    """G4. Kéo thành công một phần: chỉ nhận lô đầu (limit=2) rồi 'lỗi'; lần sau
    kéo tiếp từ con trỏ của lô đầu phải lấy đủ phần còn lại, không mất bản ghi nào."""
    start = run.base + 10_000  # vùng updated_at riêng, không lẫn với các case khác
    records = [make_record(table, new_id(), start + step * 1000) for step in range(1, 6)]
    run.push(table, records)
    ids = [record["id"] for record in records]

    first = run.pull_page(table, start, limit=2)
    first_ids = [item["id"] for item in first["records"]]
    cursor = max(item["updated_at"] for item in first["records"]) if first["records"] else start
    rest = run.pull_all(table, cursor)

    received = {item["id"]: item for item in first["records"]}
    received.update(rest)
    return [check(
        f"{table} | G4 kéo một phần rồi lỗi: lần sau kéo tiếp từ con trỏ lô đầu, đủ 5 bản ghi, không mất",
        first_ids == ids[:2]
        and first["has_more"] is True
        and all(received.get(record["id"]) == record for record in records),
        f"lô đầu={first_ids}, has_more={first['has_more']}, nhận được={len(received)} bản ghi",
    )]


def case_large_response(run):
    """G5 (chỉ bảng transactions). Có hơn 500 bản ghi: dù xin limit rất lớn, server vẫn
    chỉ trả tối đa 500 và has_more=true; kéo tiếp từ con trỏ lấy nốt, không mất bản ghi nào."""
    table = "transactions"
    start = run.base + 20_000
    records = [make_record(table, new_id(), start + index) for index in range(1, BIG_COUNT + 1)]
    for from_index in range(0, len(records), PUSH_CHUNK):
        run.push(table, records[from_index : from_index + PUSH_CHUNK])

    first = run.pull_page(table, start, limit=100_000)
    cursor = max(item["updated_at"] for item in first["records"])
    rest = run.pull_all(table, cursor, limit=100_000)
    received = {item["id"]: item for item in first["records"]}
    received.update(rest)
    missing = [record["id"] for record in records if received.get(record["id"]) != record]
    return [
        check(
            f"{table} | G5a xin limit=100000 khi có {BIG_COUNT} bản ghi: server chỉ trả {EXPECTED_MAX_PULL_LIMIT}, has_more = true",
            len(first["records"]) == EXPECTED_MAX_PULL_LIMIT and first["has_more"] is True,
            f"trả về {len(first['records'])} bản ghi, has_more={first['has_more']}",
        ),
        check(
            f"{table} | G5b kéo tiếp từ con trỏ của lô đầu: đủ {BIG_COUNT} bản ghi, không mất bản ghi nào",
            not missing,
            f"thiếu hoặc sai {len(missing)} bản ghi",
        ),
    ]


def main():
    base_url = input(f"Địa chỉ backend (Enter = {DEFAULT_BASE_URL}): ").strip() or DEFAULT_BASE_URL
    email = input("Email tài khoản test: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    run_large = input(
        f"Chạy thêm G5 (tạo {BIG_COUNT} giao dịch rồi đánh dấu xoá, mất vài phút, chỉ nên chạy 1 lần)? (y/N): "
    ).strip().lower() == "y"
    headers = {"Authorization": f"Bearer {login(email, password)}"}
    call_api("GET", f"{base_url}/api/v1/health")

    run = Run(base_url, headers, int(time.time() * 1000))
    results = []
    try:
        for table in TABLES:
            for case in (case_reinstall, case_second_device, case_error_keeps_data, case_partial_pull):
                results += case(run, table)
        if run_large:
            results += case_large_response(run)
    finally:
        # Dọn dẹp theo chính sách tombstone: không xoá vật lý, chỉ đánh dấu xoá
        run.cleanup()

    print()
    print(f"Kết quả: {sum(results)}/{len(results)} case đạt")
    if not all(results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()