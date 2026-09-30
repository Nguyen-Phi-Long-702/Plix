import asyncio
import os
import sys
import time

import asyncpg
import httpx

from app.core.config import DATABASE_URL

API_PREFIX = "/api/v1"
DEMO_NOTE = "do xang xe may"
TEST_TX_ID = "__ngay27_demo_tx__"


async def main() -> None:
    base_url = os.getenv("PLIX_API_BASE_URL", "http://127.0.0.1:8000")
    token = os.getenv("PLIX_TEST_JWT")
    if not token:
        print("LOI: thieu bien moi truong PLIX_TEST_JWT (dung tai khoan test MOI, chua tung retrain).")
        sys.exit(1)

    headers = {"Authorization": f"Bearer {token}"}
    pool = await asyncpg.create_pool(dsn=DATABASE_URL, ssl="require", min_size=1, max_size=3)

    try:
        async with httpx.AsyncClient(base_url=base_url, headers=headers, timeout=15.0) as client:
            whoami_response = await client.get(f"{API_PREFIX}/whoami")
            whoami_response.raise_for_status()
            user_id = whoami_response.json()["user_id"]
            print(f"[0] user_id tu JWT: {user_id}")

            categories_response = await client.get(f"{API_PREFIX}/categories")
            categories_response.raise_for_status()
            categories = categories_response.json()
            if len(categories) < 2:
                print("LOI: can it nhat 2 category de demo (category he thong da seed san).")
                sys.exit(1)
            category_a, category_b = categories[0], categories[1]
            print(f"[0] Dung 2 category that: {category_a['name']} / {category_b['name']}")

            now_ms = int(time.time() * 1000)
            await pool.execute(
                """
                INSERT INTO transactions (id, user_id, amount, category_id, note, occurred_at, updated_at)
                VALUES ($1, $2, 10000, $3, $4, $5, $5)
                ON CONFLICT (id) DO UPDATE SET note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """,
                TEST_TX_ID, user_id, category_a["id"], DEMO_NOTE, now_ms,
            )
            print(f"[0] Da tao giao dich tam {TEST_TX_ID} voi note '{DEMO_NOTE}'")

            categorize_response = await client.post(f"{API_PREFIX}/categorize", json={"note": DEMO_NOTE})
            categorize_response.raise_for_status()
            categorize_body = categorize_response.json()
            print(
                f"[1] POST /categorize -> category_id={categorize_body['category_id']}, "
                f"confidence={categorize_body['confidence']:.4f}"
            )

            correction_response = await client.post(
                f"{API_PREFIX}/correction",
                json={
                    "transaction_id": TEST_TX_ID,
                    "predicted_category_id": categorize_body["category_id"],
                    "corrected_category_id": category_b["id"],
                },
            )
            print(
                f"[2] POST /correction -> status={correction_response.status_code}, "
                f"body={correction_response.json()}"
            )

            retrain_results = await asyncio.gather(
                client.post(f"{API_PREFIX}/retrain"),
                client.post(f"{API_PREFIX}/retrain"),
            )
            statuses = sorted(r.status_code for r in retrain_results)
            print(f"[3] 2 request /retrain dong thoi -> status codes: {statuses}")
            for i, r in enumerate(retrain_results, start=1):
                print(f"     request {i}: {r.status_code} {r.json()}")
            if statuses != [200, 409]:
                print(
                    "    CANH BAO: ket qua khac ky vong [200, 409]. Kiem tra lai "
                    "PLIX_TEST_JWT co dung la tai khoan MOI, chua tung retrain hay khong."
                )

            retrain_again = await client.post(f"{API_PREFIX}/retrain")
            print(
                f"[4] Goi /retrain ngay sau do -> status={retrain_again.status_code}, "
                f"body={retrain_again.json()}"
            )
            if retrain_again.status_code != 429:
                print("    CANH BAO: ky vong 429 (dang trong thoi gian cho gioi han tan suat).")

    finally:
        await pool.execute("DELETE FROM corrections WHERE transaction_id = $1", TEST_TX_ID)
        await pool.execute("DELETE FROM transactions WHERE id = $1", TEST_TX_ID)
        print("[cleanup] Da xoa giao dich va correction tam thoi (khong de lai rac trong Postgres that).")
        await pool.close()


if __name__ == "__main__":
    asyncio.run(main())