import getpass
import threading
import uuid

from scripts.eval_categorize import call_api, login

DEFAULT_BASE_URL = "http://127.0.0.1:8000"


def make_record(record_id, updated_at, amount, is_deleted=False):
    return {
        "id": record_id,
        "updated_at": updated_at,
        "is_deleted": is_deleted,
        "amount": amount,
        "type": "expense",
        "note": "ngay34-test",
        "occurred_at": 1790000000000,
    }


def push(base_url, headers, record):
    return call_api(
        "POST",
        f"{base_url}/api/v1/sync/transactions/push",
        headers=headers,
        body={"records": [record]},
    )


def pull_one(base_url, headers, record_id, since):
    body = call_api(
        "GET",
        f"{base_url}/api/v1/sync/transactions/pull?since={since}&limit=500",
        headers=headers,
    )
    found = [item for item in body["records"] if item["id"] == record_id]
    if len(found) != 1:
        raise SystemExit(f"Pull phải trả đúng 1 bản ghi {record_id}, thực tế {len(found)}")
    return found[0]


def check(label, condition, detail):
    print(f"[{'ĐẠT' if condition else 'KHÔNG ĐẠT'}] {label} - {detail}")
    return condition


def scenario_newer_wins(base_url, headers, record_id, a_first):
    """Khác updated_at: thiết bị B (updated_at lớn hơn) phải thắng, bất kể ai đến trước."""
    device_a = make_record(record_id, 5000, amount=111)
    device_b = make_record(record_id, 6000, amount=222)
    order = [device_a, device_b] if a_first else [device_b, device_a]
    for record in order:
        response = push(base_url, headers, record)
        if response["upserted_ids"] != [record_id] or response["rejected"]:
            raise SystemExit(f"Push bị từ chối hoặc sai định dạng: {response}")
    stored = pull_one(base_url, headers, record_id, since=0)
    label = "Khác updated_at, " + ("A đến trước, B đến sau" if a_first else "B đến trước, A đến sau")
    ok = check(
        label,
        stored["amount"] == 222 and stored["updated_at"] == 6000,
        f"server lưu amount={stored['amount']}, updated_at={stored['updated_at']} (kỳ vọng 222, 6000)",
    )
    return ok


def scenario_concurrent(base_url, headers, record_id):
    """Khác updated_at, 2 thiết bị gửi gần như cùng lúc (2 luồng)."""
    device_a = make_record(record_id, 5000, amount=111)
    device_b = make_record(record_id, 6000, amount=222)
    errors = []

    def run(record):
        try:
            push(base_url, headers, record)
        except BaseException as error:  # SystemExit từ call_api cũng phải bắt lại
            errors.append(error)

    threads = [threading.Thread(target=run, args=(record,)) for record in (device_a, device_b)]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    if errors:
        raise SystemExit(f"Có request lỗi: {errors}")
    stored = pull_one(base_url, headers, record_id, since=0)
    ok = check(
        "Khác updated_at, 2 thiết bị gửi gần như cùng lúc",
        stored["amount"] == 222 and stored["updated_at"] == 6000,
        f"server lưu amount={stored['amount']}, updated_at={stored['updated_at']} (kỳ vọng 222, 6000)",
    )
    return ok


def scenario_same_updated_at(base_url, headers, record_id, a_first):
    """Trùng updated_at (set tay): bản đang lưu (đến trước) thắng, bản đến sau bị bỏ qua nhưng không báo lỗi."""
    device_a = make_record(record_id, 7000, amount=111)
    device_b = make_record(record_id, 7000, amount=222)
    first, second = (device_a, device_b) if a_first else (device_b, device_a)
    push(base_url, headers, first)
    second_response = push(base_url, headers, second)
    stored = pull_one(base_url, headers, record_id, since=0)
    label = "Trùng updated_at, " + ("A đến trước" if a_first else "B đến trước")
    ok_stored = check(
        label,
        stored["amount"] == first["amount"] and stored["updated_at"] == 7000,
        f"server giữ amount={stored['amount']} (kỳ vọng {first['amount']}, bản đến trước)",
    )
    ok_response = check(
        label + " - phản hồi cho bản đến sau",
        second_response["upserted_ids"] == [record_id] and second_response["rejected"] == [],
        f"{second_response}",
    )
    return ok_stored and ok_response


def main():
    base_url = input(f"Địa chỉ backend (Enter = {DEFAULT_BASE_URL}): ").strip() or DEFAULT_BASE_URL
    email = input("Email tài khoản test: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    headers = {"Authorization": f"Bearer {login(email, password)}"}
    call_api("GET", f"{base_url}/api/v1/health")

    ids = [f"ngay34-{uuid.uuid4()}" for _ in range(5)]
    try:
        results = [
            scenario_newer_wins(base_url, headers, ids[0], a_first=True),
            scenario_newer_wins(base_url, headers, ids[1], a_first=False),
            scenario_concurrent(base_url, headers, ids[2]),
            scenario_same_updated_at(base_url, headers, ids[3], a_first=True),
            scenario_same_updated_at(base_url, headers, ids[4], a_first=False),
        ]
    finally:
        # Dọn dẹp theo đúng chính sách tombstone: không xoá vật lý, chỉ đánh dấu xoá (updated_at lớn hơn mọi bản test).
        for record_id in ids:
            try:
                push(base_url, headers, make_record(record_id, 9000, amount=0, is_deleted=True))
            except BaseException as error:
                print(f"Không đánh dấu xoá được {record_id}: {error}")

    print()
    print(f"Kết quả: {sum(results)}/{len(results)} kịch bản đạt")
    if not all(results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()