# Kết quả Sync Test Suite Phần 2 (trường hợp phức tạp) — Ngày 36

**Người thực hiện:** Nguyễn Đại Vương, Nguyễn Phi Long
**Ngày giờ chạy:** 
**Backend đã kiểm tra:** local (uvicorn + Postgres Supabase) và Render (https://plix-7jfp.onrender.com)
**Android:** nhánh `sprint4-android-long`, emulator/thiết bị đã dùng

## 1. Test tự động phía backend (pytest, pool giả)

Lệnh `python -m pytest` trong `backend`: 127 passed, 0 failed.

## 2. Script `scripts/sync_edge_check.py` (Postgres thật) — kết quả local / Render

| Case | transactions | categories | budgets | goals | corrections |
|---|---|---|---|---|---|
| G1 Cài lại app + đăng nhập (since=0), bản đã xoá vẫn là tombstone | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| G2a Thiết bị 2 đăng nhập lần đầu thấy đúng dữ liệu | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| G2b Thiết bị 1 nhận bản sửa và bản mới từ thiết bị 2 | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| G3 Kéo lỗi (401) rồi kéo lại cùng since: dữ liệu y như trước | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |
| G4 Kéo một phần rồi lỗi, kéo tiếp từ con trỏ lô đầu: đủ, không mất | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt | local: Đạt / Render: Đạt |

(Mỗi ô ghi `ĐẠT` hoặc `KHÔNG ĐẠT` theo dạng `local / Render`; các dòng G2a–G4 điền tương tự dòng G1.)

| Case (chỉ bảng transactions, chỉ chạy trên Render) | Kết quả |
|---|---|
| G5a Xin limit=100000 khi có 501 bản ghi: server chỉ trả 500, has_more = true | Render: Đạt |
| G5b Kéo tiếp từ con trỏ của lô đầu: đủ 501 bản ghi, không mất | Render: Đạt |

Tổng: local 25/25, Render 27/27.

Chạy lại script cũ trên Render (hồi quy): `simulate_two_devices.py` (Ngày 34) 5/5; `sync_functional_check.py` (Ngày 35) 35/35.

## 3. Test Android (Long chạy)

| Nội dung | Kết quả |
|---|---|
| `gradlew.bat testDebugUnitTest` toàn bộ | tổng số test, số lỗi |
| SyncPullerTest | kết quả |
| SyncPusherTest | |
| SyncTombstoneTest | |
| LogoutUseCaseTest | |
| AuthAuthenticatorTest | |
| SyncPullerRoomTest (instrumented, emulator) | x/2 |

## 4. Kiểm tra thủ công trên emulator (app → Render → Supabase)

| Mã | Nội dung | transactions | categories | budgets | goals | corrections |
|---|---|---|---|---|---|---|
| M1 | Cài lại app + đăng nhập: mục giữ có lại, mục đã xoá không hiện, không nhân đôi | | | | | không áp dụng |
| M2 | Thiết bị thứ 2 đăng nhập thấy đủ dữ liệu; bản sửa và bản mới từ thiết bị 2 về thiết bị 1 | | | | | không áp dụng |
| M3 | Đăng xuất (không còn dữ liệu chờ) rồi đăng nhập lại: dữ liệu đủ, mục đã xoá không hiện | | | | | không áp dụng |

## 5. Đối chiếu toàn bộ tình huống Mục 18.6 (Master Plan)

| # | Tình huống | Bằng chứng | Kết quả |
|---|---|---|---|
| 1 | Local insert khi offline | Ngày 35 mục 4, E1 (thủ công) | |
| 2 | Local update bản đã `synced` → `pending`, `updated_at = now` | `UpdateTransactionUseCaseTest.execute_validEntity_setsUpdatedAtAndPendingStatus` | |
| 3 | Local delete (`is_deleted = 1`, `pending`, không xoá vật lý) | `DeleteTransactionUseCaseTest`, `SyncTombstoneTest` (5 bảng), Ngày 35 E2 | |
| 4 | Network failure giữa lúc gửi | `SyncPusherTest` (2 test), Ngày 35 E1 | |
| 5 | Timeout (coi như failure, retry) | Đọc code: `SyncWorker` bắt `IOException` → `Result.retry()` (cùng nhánh với dòng 4) | |
| 6 | Duplicate request | Ngày 35 F6, F7; `test_sync_tombstone_duplicate.py` | |
| 7 | Server failure 5xx | Đọc code: `SyncPusher.pushBatch` ném `HttpException` trước `markSynced`; `SyncWorker.handleHttpFailure` retry khi mã ≥ 500 (chưa có test tự động riêng cho push 5xx) | |
| 8 | Token expired giữa lúc sync | Ngày 34: `AuthAuthenticatorTest` (2 test), `test_sync_expired_token.py` (2 test) | |
| 9 | Conflict khác `updated_at` | `simulate_two_devices.py` (chạy lại hôm nay), `SyncPullerTest.upsertBatch_differentUpdatedAt_newerWinsInEitherArrivalOrder` | |
| 10 | Conflict trùng `updated_at` | `simulate_two_devices.py`, `SyncPullerTest.upsertBatch_sameUpdatedAt_firstStoredVersionWinsAndLaterOneNeverOverwrites` | |
| 11 | Pull sau login | G1, G2a, M1 | |
| 12 | First sync (tài khoản mới, cloud rỗng) | `SyncPullerTest.pullAll_emptyFirstResponse_appliesNothingAndKeepsCursor`; backend `test_pull_with_no_rows_returns_empty_and_skips_has_more_query` | |
| 13 | Sync sau thời gian dài offline (lô 50) | `SyncPusherTest.push_networkLostOnSecondBatch_firstBatchSyncedRestStayPending`, `push_afterNetworkReturns_remainingPendingRecordsGetSynced` | |
| 14 | Multi-device | G2a, G2b, M2 | |
| 15 | Pull không trả về tombstone (phòng "delete resurrection"): KHÔNG áp dụng, pull LUÔN trả tombstone | Ngày 35 F4; G1; `SyncPullerTest.pullAll_freshInstallReceivesTombstone_storesItAsDeletedAndKeepsItHidden` | |
| 16 | Pull thành công một phần (all-or-nothing) | G4; `SyncPullerTest` (4 test lỗi giữa chừng); `SyncPullerRoomTest` (2 test) | |
| 17 | Logout còn dữ liệu pending | Ngày 33: `LogoutUseCaseTest` (các nhánh đăng xuất) và kiểm thử thủ công Ngày 33; M3 | |
| 18 | Reinstall app | G1; M1; `SyncPullerTest.pullAll_freshInstallReceivesTombstone_storesItAsDeletedAndKeepsItHidden` | |
| B1 | Con trỏ không tiến khi có lỗi (bổ sung theo Day 36) | G3; `SyncPullerTest` (4 test lỗi); `SyncPullerRoomTest.pull_batchWithCorruptRecord_writesNothingAndKeepsCursor` | |
| B2 | Phản hồi quá lớn / soft-limit (bổ sung theo Day 36) | G5a, G5b; backend `test_pull_limit_larger_than_max_pull_limit_is_capped`; `SyncPullerTest.pullAll_moreThan500Records_loopsWithNewCursorUntilAllPulled`, `pullAll_501RecordsWithSameUpdatedAt_stopsWhenSameBatchReturnedTwice`, `pullAll_exactly500Records_stopsAfterOneCallWhenServerSaysNoMore` | |

Ghi chú: ở Ngày 36 "soft-limit" được kiểm tra bằng giới hạn thật của code (`MAX_PULL_LIMIT = 500` + `has_more`); "all-or-nothing" áp dụng cho từng lô.

## 6. Lỗi phát hiện và cách sửa


## 7. Kết luận

Số case đạt / tổng số. Đạt 100% case Must Have thì ghi rõ.