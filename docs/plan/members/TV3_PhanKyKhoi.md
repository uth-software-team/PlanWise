# TV3 — Phan Ký Khôi (Feature Developer — Tasks, Availability, Calendar)

[← README](../README.md) · Nguồn sự thật WBS: [02_WBS.md](../02_WBS.md)

## Vai trò

- Owner **Personal Tasks**, **Availability Rules & Fixed Events**, **Calendar query** (BE + FE). Đây là input chính của Scheduling.
- Reviewer chính cho BE của TV4 và các PR của TV1.

**Package/file sở hữu:** `modules/task`, `modules/availability` (gồm calendar), `modules/dashboard` (P1), nháp migration V3 `tasks`, V4 `availability_rules`, V5 `fixed_events`; FE `features/tasks`, `features/availability`, `features/dashboard` (P1).

**Tổng giờ:** 90h = BE 40 · FE 24 · Review 4 · Báo cáo 5 · Integration 17 (P1 không tính: DB-01 3h, FE-DB 4h — mặc định CUT).

**Business rules chính:** BR-01, BR-02, BR-03, BR-07, HC-02, HC-03, HC-08; priority LOW/MEDIUM/HIGH/URGENT; status TODO/IN_PROGRESS/COMPLETED/CANCELLED; overdue tính, không lưu; soft delete.

## Task của tôi

| ID | Task | P | h | Depends | Branch | Acceptance | Tests | Date |
|---|---|---|---|---|---|---|---|---|
| TK-00 | Nháp V3, V4, V5 | P0 | 2 | ERD | trong `feature/contract-task-availability` | Khớp ERD; CHECK ONCE/WEEKLY | review TV1 | 11-18 |
| TK-01 | Contract Task/Availability/FixedEvent/Calendar + interface | P0 | 5 | F-01, F-04 | `feature/contract-task-availability` | Stub 501; chữ ký `TaskApi`, `AvailabilityApi`, `CalendarApi` | build | 11-19 |
| TK-02 | Task entity + CRUD | P0 | 7 | F-02, TK-01 | `feature/task-crud` | BR-01/02/03; owner-only; soft delete; `version` | WebMvc + service | 11-21 |
| TK-03 | Status + filter + paging | P0 | 3 | TK-02 | `feature/task-status-filter` | `PageResponse` | tests | 11-21 |
| TK-04 | `TaskApi` impl | P0 | 2 | TK-02 | (cùng TK-03) | HC-08; `createGroupTask` | unit | 11-21 |
| AV-02 | AvailabilityRule CRUD | P0 | 4 | F-02 | `feature/availability-rules` | day 1–7; start < end | tests | 11-22 |
| AV-04 | Expand availability theo tz (DST-safe) | P0 | 3 | AV-02 | (cùng AV-02) | half-open | unit ≥ 5 | 11-23 |
| AV-03 | FixedEvent CRUD ONCE/WEEKLY + expand | P0 | 5 | AV-04 | `feature/fixed-events` | effectiveFrom/Until | tests | **11-24 11:00** |
| CAL-01 | `busyIntervals` + `GET /v1/calendar` | P0 | 5 | AV-03, SC-05a | `feature/calendar-busy` | Chỉ dữ liệu của chính user | tests | 11-25 |
| AV-05 | Test bổ sung availability/calendar | P0 | 1 | CAL-01 | (cùng CAL-01) | Case biên | — | 11-25 |
| FE-TK-01 | Task list | P0 | 6 | FE-02, TK-03, AV-03 merged | `feature/fe-tasks` | 4 trạng thái; badge overdue | E2E thủ công | 11-24 chiều → 11-26 sáng |
| FE-TK-02 | Task form / status / xoá + tab "Được giao" | P0 | 6 | FE-TK-01, AS-02 | (cùng FE-TK-01) | Luồng 2 qua UI | E2E thủ công | 11-27 |
| FE-AV | Availability + Fixed events | P0 | 8 | AV-03 | `feature/fe-availability` | | E2E thủ công | 11-29 |
| RP-04 | Báo cáo: Tasks / Availability / Calendar | P0 | 5 | RP-01 | ngoài repo | Đúng mẫu | — | 11-30 |
| IT-DRY / IT-02 / IT-03 | Dry-run, E2E, regression | P0 | ~13 | — | `fix/*` | | E2E | 11-29 → 12-02 |
| DB-01 | Dashboard summary | P1 | 3 | gate | `feature/dashboard-summary` | BR-15 | tests | sau 11-30 / CUT |
| FE-DB | Dashboard FE | P1 | 4 | DB-01 | `feature/fe-dashboard` | | — | sau 11-30 / CUT |

## Lịch từng ngày

- [ ] **11-18 (T4):** setup env (`bash scripts/install-hooks.sh`, `./scripts/setup-dev.sh --docker`, `./gradlew build`); đọc doc phần Task/Availability + ERD; TK-00 nháp V3–V5.
- [ ] **11-19 (T5):** TK-01 (mở PR kèm nháp migration trước 11:00); sửa theo review TV1.
- [ ] **11-20 (T6):** TK-02.
- [ ] **11-21 (T7):** TK-02 xong; TK-03; TK-04 (**Tasks API merged** — TV2, TV4 đang chờ TK-04).
- [ ] **11-22 (CN):** AV-02; AV-04 bắt đầu.
- [ ] **11-23 (T2):** **chỉ** AV-04 xong + AV-03 (code xong cuối ngày, mở PR).
- [ ] **11-24 (T3):** sáng: sửa review AV-03 → merge trước 11:00 (TV2 đang chờ cho SC-05b); chiều: FE-TK-01 bắt đầu.
- [ ] **11-25 (T4):** CAL-01; AV-05 (**Backend P0 freeze**).
- [ ] **11-26 (T5):** FE-TK-01 xong (sáng); FE-TK-02.
- [ ] **11-27 (T6):** FE-TK-02 xong; FE-AV.
- [ ] **11-28 (T7):** FE-AV; polish FE-TK.
- [ ] **11-29 (CN):** FE-AV xong; IT-DRY.
- [ ] **11-30 (T2):** fix (1h); RP-04 (5h).
- [ ] **12-01 (T3):** IT-02.
- [ ] **12-02 (T4):** IT-03.
- [ ] **12-03 (T5):** kiểm tra cuối.

Khung review mỗi ngày: 11:00 và 16:00.

## Phụ thuộc

**Upstream (tôi chờ):**
- TV1: F-01/F-04 (11-18) → TK-01; DB ready (11-19) → CRUD; FE-02/03 (11-23) → FE.
- TV2: `ScheduleApi` impl SC-05a (11-23) → CAL-01.
- TV4: AS-02 (11-23) → tab "Được giao" trong FE-TK-02.

**Downstream (chờ tôi):**
- TV2: TK-04 (11-21), AV-04 (11-23), **AV-03 (11-24 11:00)** → SC-05b; CAL-01 (11-25) → FE-CAL.
- TV4: TK-04 `createGroupTask` (11-21) → AS-01; FE-TK-02 → FE-AI.
- TV1: CAL-01 `busyIntervals` (11-25) → GR-04.

## API sở hữu / tiêu thụ

| Sở hữu | Tiêu thụ |
|---|---|
| `GET/POST /v1/tasks`, `GET/PUT/DELETE /v1/tasks/{id}`, `PATCH /v1/tasks/{id}/status` | `ScheduleApi` (TV2) — cho CAL-01 |
| `GET/POST /v1/availability-rules`, `PUT/DELETE /v1/availability-rules/{id}` | `GET /v1/assignments?status=ACCEPTED` (TV4) — tab "Được giao" (CG-10) |
| `GET/POST /v1/fixed-events`, `PUT/DELETE /v1/fixed-events/{id}` | `POST /v1/ai/tasks/parse` gián tiếp qua FE-AI (TV4 gọi `POST /v1/tasks`) |
| `GET /v1/calendar?start&end` | |
| `TaskApi`, `AvailabilityApi`, `CalendarApi` | |
| P1: `GET /v1/dashboard/summary` | |

## PR tôi phải review (SLA ≤ 4h, khung 11:00 / 16:00)

- Toàn bộ BE của TV4: GR-01, GR-02, GR-03, AS-01, AS-02, AS-PG, AI-01, AI-02.
- PR của TV1: AU-01..03, F-03, GR-04, FE-00..03, FE-AU.
- FE của TV4: FE-GR-01, FE-AS-01/02, FE-AI.

PR của tôi: TK-*, AV-02/04 và FE do **TV4** review; AV-03, CAL-01 do **TV2** review.

## Báo cáo

RP-04 — phần Tasks / Availability / Calendar (BR-01..03, timezone/DST, ảnh UI), 5h, ngày 11-30.

## Definition of Done áp dụng

Xem [06_TEST_DOD.md](../06_TEST_DOD.md). Thêm: test timezone có DST, WEEKLY qua nhiều tuần, effective range; ownership (user khác → 404).

## Progress log

| Date | Done | Blocked | Next |
|---|---|---|---|
| | | | |
