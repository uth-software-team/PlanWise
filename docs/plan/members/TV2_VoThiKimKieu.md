# TV2 — Võ Thị Kim Kiều (Feature Developer — Smart Scheduling)

[← README](../README.md) · Nguồn sự thật WBS: [02_WBS.md](../02_WBS.md)

## Vai trò

- Owner **Smart Scheduling & Schedule Management** (BE + FE) — phần khó nhất, nằm trên **critical path**.
- Scheduling Engine là Java thuần, deterministic, không phụ thuộc LLM (G5, CG-07).

**Package/file sở hữu:** `modules/schedule/**` (gồm `engine/`), nháp migration V8 (`schedule_proposals`, `schedule_sessions`), FE `features/calendar`, `features/schedule`.

**Tổng giờ:** 90h = BE 47 · FE 22 · Review 1 · Báo cáo 5 · Integration 15.

**Business rules chính:** BR-07, BR-08, BR-09, BR-10, BR-11; HC-01..HC-09; khoảng half-open `[startAt, endAt)`; 1 session liên tục/task/batch; CONFIRMED/COMPLETED được bảo vệ; user phải confirm rõ ràng.

## Task của tôi

| ID | Task | P | h | Depends | Branch | Acceptance | Tests | Date |
|---|---|---|---|---|---|---|---|---|
| SC-02 | Engine model + `TimeInterval` + free slots | P0 | 6 | — | `feature/schedule-engine-slots` | Java thuần; available − busy; half-open | unit ≥ 8 | 11-20 |
| SC-01 | Contract scheduling + `ScheduleApi` + nháp V8 | P0 | 5 | F-04 | `feature/contract-schedule` | Theo CG-07; stub 501; V8 khớp ERD | build | 11-19 |
| SC-03 | Allocation xác định + earliest-fit + reasons | P0 | 8 | SC-02 | `feature/schedule-engine-allocation` | priority desc, deadline asc, remaining, createdAt, taskId; BR-08; `INSUFFICIENT_TIME`, `DEADLINE_PASSED` | unit ≥ 12 | 11-21 |
| SC-04 | `ConstraintValidator` HC-01..09 | P0 | 6 | SC-02 | `feature/schedule-constraint-validator` | Đúng mã từng HC | unit ≥ 1/HC | 11-22 |
| SC-05a | Proposal/Session entity + generate trên mock | P0 | 5 | SC-03, SC-01 | `feature/schedule-generate` | Chỉ PROPOSED; 1 batch/user; không đụng CONFIRMED; 200 + `unscheduledTasks` | Mockito + WebMvc | 11-23 |
| SC-05b | Nối impl thật (Task, Availability, Assignment) | P0 | 5 | SC-05a, TK-04, AV-04, AV-03, AS-02 | `feature/schedule-generate-wiring` | Generate với dữ liệu thật; task ACCEPTED được xếp | `@SpringBootTest` | 11-24 |
| SC-06 | Sửa/xoá PROPOSED; GET/DELETE proposal | P0 | 5 | SC-05a, SC-04 | `feature/schedule-edit-session` | BR-10; sửa CONFIRMED → 409 | tests | 11-24 |
| SC-07 | Confirm 1 transaction | P0 | 7 | SC-06, SC-05b | `feature/schedule-confirm` | BR-11; `STALE_VERSION`; `CONSTRAINT_VIOLATION` + `conflicts[]`; rollback cả batch | tests + concurrency (1h sáng 11-26) | 11-25 (+1h 11-26) |
| FE-CAL | Lịch tuần | P0 | 5 | CAL-01 | `feature/fe-calendar` | Sessions + fixed events, đúng tz | E2E thủ công | 11-26 |
| FE-SC-01 | Generate + preview + lý do unscheduled | P0 | 6 | SC-05b | `feature/fe-schedule-generate` | | E2E thủ công | 11-27 |
| FE-SC-02 | Sửa/xoá PROPOSED (form) | P0 | 5 | SC-06 | (cùng FE-SC-01) | Hiển thị lỗi HC | E2E thủ công | 11-28 |
| FE-SC-03 | Confirm + conflicts + reload khi STALE | P0 | 5 | SC-07 | `feature/fe-schedule-confirm` | Luồng 3 qua UI | E2E thủ công | 11-29 |
| RP-03 | Báo cáo: phần Scheduling | P0 | 5 | RP-01 | ngoài repo | Đúng mẫu | — | 11-30 |
| IT-DRY / IT-02 / IT-03 | Dry-run, E2E, regression | P0 | ~13 | — | `fix/*` | | E2E | 11-29 → 12-02 |

## Lịch từng ngày

- [ ] **11-18 (T4):** setup env (`bash scripts/install-hooks.sh`, `./scripts/setup-dev.sh --docker`, `./gradlew build`); đọc doc phần Scheduling + HC; SC-02 (4h).
- [ ] **11-19 (T5):** SC-01 + nháp V8 (mở PR trước 11:00); SC-02 tiếp.
- [ ] **11-20 (T6):** SC-02 xong; SC-03.
- [ ] **11-21 (T7):** SC-03 xong; SC-04.
- [ ] **11-22 (CN):** SC-04 xong; SC-05a (entity, repo, service với mock interface).
- [ ] **11-23 (T2):** SC-05a xong; SC-06 (trên mock). ⚠️ Nếu SC-05a trễ > 4h → báo TV1 ngay (R1).
- [ ] **11-24 (T3):** review AV-03 ở khung 11:00; SC-05b (nối thật); SC-06 xong.
- [ ] **11-25 (T4):** SC-07; review CAL-01.
- [ ] **11-26 (T5):** SC-07 concurrency test (1h); FE-CAL.
- [ ] **11-27 (T6):** FE-SC-01.
- [ ] **11-28 (T7):** FE-SC-02.
- [ ] **11-29 (CN):** FE-SC-03; IT-DRY.
- [ ] **11-30 (T2):** polish FE schedule (1h); RP-03 (5h).
- [ ] **12-01 (T3):** IT-02.
- [ ] **12-02 (T4):** IT-03.
- [ ] **12-03 (T5):** kiểm tra cuối.

Khung review mỗi ngày: 11:00 và 16:00.

## Phụ thuộc

**Upstream (tôi chờ):**
- TV1: F-04 (11-18) → SC-01; DB ready (11-19) → SC-05a entity.
- TV3: `TaskApi` impl TK-04 (11-21), AV-04 (11-23), **AV-03 (11-24 11:00)** → SC-05b; CAL-01 (11-25) → FE-CAL.
- TV4: `AssignmentApi` AS-02 (11-23) → SC-05b.
- TV1: FE-02/03 (11-23) → FE.

**Downstream (chờ tôi):**
- TV3: `ScheduleApi` (interface 11-19; impl SC-05a 11-23) → CAL-01.
- Cả nhóm: SC-07 + FE-SC-03 → gate 11-30 (luồng 3, 4).

## API sở hữu / tiêu thụ

| Sở hữu | Tiêu thụ |
|---|---|
| `POST /v1/schedules/generate` | `TaskApi` (TV3) |
| `GET/DELETE /v1/schedules/proposals/{proposalId}` | `AvailabilityApi` (TV3) |
| `PUT/DELETE /v1/schedules/sessions/{id}` | `AssignmentApi.acceptedTaskIds` (TV4) |
| `POST /v1/schedules/{proposalId}/confirm` | `GET /v1/calendar` (TV3) — cho FE-CAL |
| `ScheduleApi` (sessions theo khoảng) | |

## PR tôi phải review (SLA ≤ 4h, khung 11:00 / 16:00)

- **Chỉ** AV-03 (11-24, khung 11:00) và CAL-01 (11-25) của TV3.

PR của tôi: BE do **TV1** review; FE do **TV4** review.

## Báo cáo

RP-03 — phần Scheduling (thuật toán, HC, confirm/rollback, ảnh UI), 5h, ngày 11-30.

## Definition of Done áp dụng

Xem [06_TEST_DOD.md](../06_TEST_DOD.md). Thêm cho Scheduling: test biên half-open, deadline, CONFIRMED bảo vệ, overload, deterministic (chạy 2 lần cùng input → cùng output), concurrency confirm.

## Progress log

| Date | Done | Blocked | Next |
|---|---|---|---|
| | | | |
