import csv
import os
import sys
from pathlib import Path

import httpx

API_PREFIX = "/api/v1"
CSV_PATH = Path(__file__).resolve().parents[3] / "data" / "test_categorization_holdout.csv"
RESULTS_PATH = Path(__file__).resolve().parents[3] / "data" / "day25_holdout_results.csv"


def main() -> None:
    base_url = os.getenv("PLIX_API_BASE_URL", "http://127.0.0.1:8000")
    token = os.getenv("PLIX_TEST_JWT")
    if not token:
        print("LOI: thieu bien moi truong PLIX_TEST_JWT.")
        sys.exit(1)

    headers = {"Authorization": f"Bearer {token}"}

    with open(CSV_PATH, encoding="utf-8-sig") as csv_file:
        test_rows = list(csv.DictReader(csv_file))
    print(f"Doc {len(test_rows)} dong tu {CSV_PATH.name}")

    with httpx.Client(base_url=base_url, headers=headers, timeout=15.0) as client:
        categories_response = client.get(f"{API_PREFIX}/categories")
        categories_response.raise_for_status()
        category_name_by_id = {c["id"]: c["name"] for c in categories_response.json()}
        print(f"Da lay {len(category_name_by_id)} danh muc tu GET {API_PREFIX}/categories")

        results = []
        correct_count = 0
        for row in test_rows:
            note = row["note"]
            true_category = row["category"]

            response = client.post(f"{API_PREFIX}/categorize", json={"note": note})
            response.raise_for_status()
            body = response.json()
            category_id = body["category_id"]
            confidence = body["confidence"]
            predicted_category = category_name_by_id.get(category_id, "(khong xac dinh)")

            is_correct = predicted_category == true_category
            if is_correct:
                correct_count += 1

            results.append(
                {
                    "note": note,
                    "true_category": true_category,
                    "predicted_category": predicted_category,
                    "confidence": f"{confidence:.4f}",
                    "is_correct": "1" if is_correct else "0",
                }
            )

    with open(RESULTS_PATH, "w", encoding="utf-8", newline="") as out_file:
        writer = csv.DictWriter(
            out_file,
            fieldnames=["note", "true_category", "predicted_category", "confidence", "is_correct"],
        )
        writer.writeheader()
        writer.writerows(results)

    total = len(results)
    accuracy = (correct_count / total * 100) if total else 0.0
    print(f"\nDa luu ket qua chi tiet vao {RESULTS_PATH.name}")
    print(f"SO CAU DUNG: {correct_count}/{total}")
    print(f"DO CHINH XAC THUC TE: {accuracy:.1f}%")

    print("\n--- Breakdown theo tung danh muc (luu lai de dung cho sprint 7) ---")
    by_category = {}
    for r in results:
        stat = by_category.setdefault(r["true_category"], {"correct": 0, "total": 0})
        stat["total"] += 1
        if r["is_correct"] == "1":
            stat["correct"] += 1
    for category, stat in by_category.items():
        acc = stat["correct"] / stat["total"] * 100
        print(f"{category}: {stat['correct']}/{stat['total']} ({acc:.0f}%)")


if __name__ == "__main__":
    main()