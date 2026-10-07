import getpass
import time
import uuid

from scripts.eval_categorize import call_api, login

DEFAULT_BASE_URL = "http://127.0.0.1:8000"
TABLES = ("transactions", "categories", "budgets", "goals", "corrections")


def make_record(table, record_id, updated_at, variant="a", is_deleted=False):
    """Bản ghi đầy đủ trường theo Sync Payload Schema. variant "a" và "b" chỉ khác
    nhau đúng 1 trường dữ liệu để kiểm tra bản mới ghi đè bản cũ."""
    record = {"id": record_id, "updated_at": updated_at, "is_deleted": is_deleted}
    if table == "transactions":
        record.update(
            amount=50000 if variant == "a" else 70000,
            type="expense",
            category_id=None,
            note="ngay35-test",
            payment_method=None,
            occurred_at=1790000000000,
            is_recurring=False,
            recurrence_rule=None,
            recurrence_parent_id=None,
        )
    elif table == "categories":
        record.update(name=f"{record_id}-{variant}", type="expense")
    elif table == "budgets":
        record.update(
            period="2099-01",
            category_id=None,
            limit_amount=2000000 if variant == "a" else 3000000,
            threshold_percent=80,
        )
    elif table == "goals":
        record.update(
            name="ngay35-test",
            target_amount=20000000,
            current_amount=0 if variant == "a" else 500000,
            deadline=1800000000000,
        )
    else:  # corrections
        record.update(
            transaction_id="ngay35-tx",
            predicted_category_id=None,
            corrected_category_id="sys_an_uong" if variant == "a" else "sys_di_chuyen",
            created_at=1790000000000,
        )
    return record


def push(base_url, headers, table, records):
    return call_api(
        "POST",
        f"{base_url}/api/v1/sync/{table}/push",
        headers=headers,
        body={"records": records},
    )


def pull(base_url, headers, table, since):
    body = call_api(
        "GET",
        f"{base_url}/api/v1/sync/{table}/pull?since={since}&limit=500",
        headers=headers,
    )
    return body["records"]


def check(label, condition, detail):
    print(f"[{'ĐẠT' if condition else 'KHÔNG ĐẠT'}] {label}")
    if not condition:
        print(f"    chi tiết: {detail}")
    return condition


def rows_of(records, record_id):
    return [item for item in records if item["id"] == record_id]


def run_table(base_url, headers, table, base):
    """7 case functional cho 1 bảng. base là mốc thời gian (epoch ms) lúc bắt đầu
    chạy: mọi updated_at của bản test đều lớn hơn base nên pull since=base chỉ
    trả về bản ghi của lần chạy này."""
    results = []
    main_id = f"ngay35-{uuid.uuid4()}"
    dup_id = f"ngay35-{uuid.uuid4()}"
    retry_ids = [f"ngay35-{uuid.uuid4()}" for _ in range(3)]
    ok_response = lambda ids: {"upserted_ids": ids, "rejected": []}

    try:
        # F1. Đẩy bản ghi mới
        first = make_record(table, main_id, base + 1000)
        response = push(base_url, headers, table, [first])
        results.append(check(f"{table} | F1 đẩy bản ghi mới", response == ok_response([main_id]), response))

        # F2. Kéo về: đúng 1 bản ghi, mọi trường khớp với bản đã đẩy
        found = rows_of(pull(base_url, headers, table, base), main_id)
        results.append(check(f"{table} | F2 kéo về đúng dữ liệu đã đẩy", found == [first], found))

        # F3. Đẩy bản sửa có updated_at lớn hơn -> bản mới ghi đè
        edited = make_record(table, main_id, base + 2000, variant="b")
        push(base_url, headers, table, [edited])
        found = rows_of(pull(base_url, headers, table, base), main_id)
        results.append(check(f"{table} | F3 bản sửa (updated_at lớn hơn) ghi đè bản cũ", found == [edited], found))

        # F4. Xoá mềm: đẩy tombstone, kéo về vẫn còn bản ghi và is_deleted = true
        tombstone = make_record(table, main_id, base + 3000, variant="b", is_deleted=True)
        push(base_url, headers, table, [tombstone])
        found = rows_of(pull(base_url, headers, table, base), main_id)
        results.append(check(f"{table} | F4 tombstone vẫn được kéo về, is_deleted = true", found == [tombstone], found))

        # F5. Xoá không hồi sinh: bản còn sống cũ hơn hoặc bằng updated_at của tombstone bị bỏ qua
        older_live = make_record(table, main_id, base + 2500, variant="a")
        same_time_live = make_record(table, main_id, base + 3000, variant="a")
        older_response = push(base_url, headers, table, [older_live])
        same_time_response = push(base_url, headers, table, [same_time_live])
        found = rows_of(pull(base_url, headers, table, base), main_id)
        results.append(check(
            f"{table} | F5 bản còn sống cũ hơn/bằng không hồi sinh bản đã xoá",
            found == [tombstone]
            and older_response == ok_response([main_id])
            and same_time_response == ok_response([main_id]),
            f"pull={found}, phản hồi={older_response} / {same_time_response}",
        ))

        # F6. Gửi trùng yêu cầu: cùng 1 lô gửi 2 lần -> không lỗi, vẫn đúng 1 bản ghi
        dup = make_record(table, dup_id, base + 1000)
        responses = [push(base_url, headers, table, [dup]), push(base_url, headers, table, [dup])]
        found = rows_of(pull(base_url, headers, table, base), dup_id)
        results.append(check(
            f"{table} | F6 gửi trùng yêu cầu: không lỗi, không trùng bản ghi",
            responses == [ok_response([dup_id])] * 2 and found == [dup],
            f"phản hồi={responses}, pull={found}",
        ))

        # F7. Thử lại sau khi mất mạng: lô [A,B] đã lên server nhưng app không nhận được phản hồi nên gửi lại [A,B,C]
        records = [make_record(table, retry_id, base + 1000) for retry_id in retry_ids]
        push(base_url, headers, table, records[:2])
        response = push(base_url, headers, table, records)
        pulled = pull(base_url, headers, table, base)
        results.append(check(
            f"{table} | F7 gửi lại cả lô sau mất mạng: không lỗi, mỗi bản ghi đúng 1 dòng",
            response == ok_response(retry_ids)
            and all(rows_of(pulled, record["id"]) == [record] for record in records),
            f"phản hồi={response}",
        ))
    finally:
        # Dọn dẹp theo chính sách tombstone: không xoá vật lý, chỉ đánh dấu xoá
        for record_id in [main_id, dup_id] + retry_ids:
            try:
                push(base_url, headers, table, [make_record(table, record_id, base + 9000, variant="b", is_deleted=True)])
            except BaseException as error:  # SystemExit từ call_api cũng phải bắt lại
                print(f"Không đánh dấu xoá được {record_id}: {error}")
    return results


def main():
    base_url = input(f"Địa chỉ backend (Enter = {DEFAULT_BASE_URL}): ").strip() or DEFAULT_BASE_URL
    email = input("Email tài khoản test: ").strip()
    password = getpass.getpass("Mật khẩu: ")
    headers = {"Authorization": f"Bearer {login(email, password)}"}
    call_api("GET", f"{base_url}/api/v1/health")

    base = int(time.time() * 1000)
    results = []
    for table in TABLES:
        results += run_table(base_url, headers, table, base)

    print()
    print(f"Kết quả: {sum(results)}/{len(results)} case đạt")
    if not all(results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()