# Kết quả kiểm thử cuối và đối chiếu Exit Criteria Sprint 5 — Ngày 53

**Người thực hiện:** Nguyễn Phi Long (Android và Backend)
**Ngày chạy:** 01/11/2026
**Nhánh / commit đã kiểm thử:** `sprint6-vuong-long` / `7d92fca`
**Thiết bị Android:** Emulator Pixel 5 (AVD), Android 11

## 1. Chạy lại bộ kiểm thử lần cuối

| Hạng mục | Lệnh | Người chạy | Kết quả |
|---|---|---|---|
| Unit test Android | `gradlew.bat testDebugUnitTest --rerun` (trong `android`) | Long | 409 test, 0 lỗi — ĐẠT |
| Test instrumented Android (lần 1) | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | Long | 29 test, 2 lỗi — KHÔNG ĐẠT (xem mục 3) |
| Test instrumented Android (chạy lại) | `gradlew.bat connectedDebugAndroidTest` (trong `android`) | Long | 29 test, 2 lỗi — KHÔNG ĐẠT (xem mục 3) |
| Backend | `python -m pytest -q` (trong `backend`) | Long | 379 test, 0 lỗi — ĐẠT |

Số test Android chạy ra khớp số `@Test` đếm được trong mã nguồn (409 và 29). So với Ngày UI-7 (409, 29 và 379 test): không đổi.

## 2. Đối chiếu Exit Criteria Sprint 5 (Integration Gate)

| Tiêu chí | Bằng chứng | Kết quả |
|---|---|---|
| Test suite pass | Mục 1: 2 test hiệu năng instrumented lỗi ở cả 2 lần chạy | KHÔNG ĐẠT |
| Đồng bộ (5 bảng, toàn bộ tình huống) pass | Các test `test_sync_*` (Backend); `SyncPusherTest`, `SyncPullerTest`, `SyncTombstoneTest`, `SyncPullerRoomTest` (Android) nằm trong lần chạy ở mục 1 | ĐẠT |
| Backend AI (phân loại, bất thường) pass | Các test `test_categorize_*`, `test_anomaly_*` nằm trong lần chạy Backend ở mục 1 | ĐẠT |
| Kiểm thử bảo mật pass | Các test `test_security_*`, `test_sync_expired_token` nằm trong lần chạy Backend ở mục 1 | ĐẠT |
| Security Review hoàn chỉnh | `docs/SECURITY_REVIEW.md`, `docs/API_CONTRACT_VERIFY.md` (0 sai lệch tên trường, kiểu, cấu trúc); vấn đề chưa sửa đã ghi vào backlog B1–B7 | ĐẠT |
| Performance baseline đạt | Ngày 50: Room lần đo chậm nhất 75.8 ms (< 100 ms), `backend/docs/ngay50-ket-qua-hieu-nang-room.md`; Backend lần đo chậm nhất 2793.2 ms (< 3000 ms), `backend/docs/ngay50-ket-qua-hieu-nang-backend.md`. Ngày 51 có 1 lần đo Room là 179.5 ms, chạy lại khi máy yên thì đạt. Ngày 53: cả 2 lần chạy đều vượt ngưỡng (263.6 / 261.3 ms ở lần 1; 206.9 / 119.0 ms ở lần chạy lại), xem mục 3 | KHÔNG ĐẠT |
| ProGuard/R8 đã test sớm | `backend/docs/ngay51-ket-qua-regression-va-ban-release.md` (Ngày 51) và `docs/UI7_KET_QUA_HOI_QUY.md` (bước R1–R6 đạt) | ĐẠT |

**Kết luận:** Exit Criteria Sprint 5 KHÔNG ĐẠT — Integration Gate KHÔNG ĐẠT (còn 2 test hiệu năng Room vượt ngưỡng 100 ms).

## 3. Lỗi phát hiện và cách xử lý

Hai test thuộc `TransactionListPerformanceTest` (ngưỡng < 100 ms, trung vị) lỗi ở cả hai lần chạy trên commit `7d92fca`, Pixel 5, Android 11. 27 test instrumented còn lại đạt.

| Test | Lần 1 | Lần chạy lại |
|---|---|---|
| `transactionList_5000Rows_sortedListUnder100ms` | 263.6352 ms | 206.9291 ms |
| `transactionList_filterByCategoryAndDateRange_under100ms` | 261.3291 ms | 118.958 ms |

Không sửa mã và không đổi ngưỡng. Lần chạy lại sau khi để máy yên vẫn vượt ngưỡng. Nguyên nhân chưa xác định (không điều tra thêm).

## 4. Lỗi ghi nhận, chưa sửa

Các mục backlog B1–B7 trong `docs/SECURITY_REVIEW.md` vẫn mở như đã ghi ở Ngày 52; Ngày 53 không sửa.