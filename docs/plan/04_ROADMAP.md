# 04 — Roadmap từng ngày (2026-11-18 → 2026-12-03)

[← README](README.md) · WBS ID: [02_WBS.md](02_WBS.md)

Giả định: 6h làm thật/ngày (11-18 ~4h, 12-03 ~2h); làm cả cuối tuần. Review theo khung 11:00 và 16:00 mỗi ngày.

| Date | TV1 Kiệt | TV2 Kiều | TV3 Khôi | TV4 Thư | Deliverable / Gate |
|---|---|---|---|---|---|
| 11-18 T4 | F-01, F-04; mời teammate, đặt ruleset approvals = 1 | Setup env; SC-02 (4h) | Setup env; TK-00 nháp V3-5 | Setup env; GR-00 nháp V6, V7, V10 | Skeleton PR |
| 11-19 T5 | F-02 (V2, V9, cấp số); review + merge V2→V10 đúng thứ tự; chạy thử PG | SC-01 + nháp V8; SC-02 | TK-01 | GR-01 | **✅ DB ready (16:00)**; contract PRs merged |
| 11-20 T6 | F-05 (10:00); AU-01; AU-02 | SC-02 xong; SC-03 | TK-02 | GR-02 | **✅ API contract frozen 10:00** |
| 11-21 T7 | AU-02 xong; AU-03; FE-00 | SC-03 xong; SC-04 | TK-02 xong; TK-03; TK-04 | GR-03 | **✅ Security ready**; Tasks API merged |
| 11-22 CN | FE-00 xong; FE-01 (merge 16:00); FE-02 | SC-04 xong; SC-05a | AV-02; AV-04 | AS-01 | Group API merged |
| 11-23 T2 | FE-02 xong (16:00); FE-03; chốt CG-08 | SC-05a xong; SC-06 | Chỉ AV-04 xong + AV-03 (code xong cuối ngày) | AS-02; FE-GR-01 bắt đầu | **✅ FE scaffold ready**; Gemini model chốt |
| 11-24 T3 | FE-AU | SC-05b; SC-06 xong | Sáng: sửa review AV-03 → merge 11:00; chiều: FE-TK-01 bắt đầu | AI-01; FE-GR-01 | Generate chạy với dữ liệu thật |
| 11-25 T4 | F-03; QA-01 | SC-07 | CAL-01; AV-05 | AI-02 | **✅ Backend P0 freeze** (trừ phần giãn sang 11-26) |
| 11-26 T5 | GR-04; IT-01 | SC-07 concurrency test (1h); FE-CAL | FE-TK-01 xong (sáng); FE-TK-02 | AI-02 freeze 12:00 (nếu cần); AS-PG; FE-GR-01 xong | Phần BE được giãn xong |
| 11-27 T6 | IT-01 xong; RP-01; hỗ trợ FE | FE-SC-01 | FE-TK-02 xong; FE-AV | FE-AS-01 | Mẫu báo cáo có |
| 11-28 T7 | Hỗ trợ FE; RP-02 | FE-SC-02 | FE-AV; polish FE-TK | FE-AS-02 | |
| 11-29 CN | IT-DRY (chủ trì); review | FE-SC-03; IT-DRY | FE-AV xong; IT-DRY | FE-AI; IT-DRY | Danh sách bug |
| 11-30 T2 | IT-GATE; fix (4h); RP-02 xong (2h) | Polish (1h); RP-03 (5h) | Fix (1h); RP-04 (5h) | Fix (1h); RP-05 (5h) | **✅ Frontend integrated — 6 luồng chạy qua UI** → xét mở P1 (cần duyệt) |
| 12-01 T3 | IT-02 | IT-02 | IT-02 | IT-02 | Hạn cứng E2E (đệm 1) |
| 12-02 T4 | IT-03; RP-06; tạo `release/1.0.0` | IT-03 | IT-03 | IT-03 | **✅ Release candidate** (đệm 2) |
| 12-03 T5 | RL-01; nộp code + báo cáo | Kiểm tra cuối | Kiểm tra cuối | Kiểm tra cuối | Nộp bài |

## Checkpoint

| Checkpoint | Ngày | Điều kiện đạt |
|---|---|---|
| DB ready | 11-19 16:00 | V2..V10 merged; bootRun PG lên v10 |
| API contract frozen | 11-20 10:00 | `docs/api/openapi-v1.json` merged |
| Security ready | 11-21 | AU-01..03 merged |
| FE scaffold ready | 11-23 | FE-00..03 merged, CI FE xanh |
| Backend complete (P0 freeze) | 11-25 (phần giãn: 11-26) | QA-01 pass |
| Frontend integrated | 11-30 | IT-GATE: 6 luồng chạy qua UI |
| Release candidate | 12-02 | IT-03 xong, `release/1.0.0` tạo |
| Submission | 12-03 | `main` + tag `v1.0.0` + báo cáo |

**Đệm ổn định:** 12-01 và 12-02 (2 ngày).

## Cảnh báo khả thi

1. Contract freeze lệch nửa ngày so với time-box ban đầu (11-20 10:00 thay vì cuối 11-19).
2. GR-04, AS-PG, test concurrency SC-07 và có thể AI-02 xong vào 11-26 — không chặn FE nào trước 11-28.
3. TV2 là điểm rủi ro lớn nhất (R1): không có slack ở BE 11-18 → 11-25.
