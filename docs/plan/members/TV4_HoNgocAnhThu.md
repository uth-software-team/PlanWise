# TV4 — Hồ Ngọc Anh Thư (Feature Developer — Team, Assignment, AI)

[← README](../README.md) · Nguồn sự thật WBS: [02_WBS.md](../02_WBS.md)

## Vai trò

- Owner **Team Management**, **Task Assignment**, **AI Task Parsing (Gemini)** (BE + FE).
- Reviewer chính cho Task/AV-02/04 và toàn bộ FE của TV2, TV3.

**Package/file sở hữu:** `modules/group`, `modules/assignment`, `modules/ai`, nháp migration V6 `team_groups` + `group_members`, V7 `task_assignments`, V10 `ai_request_logs`; FE `features/groups`, `features/assignments`, `features/ai`.

**Tổng giờ:** 90h = BE 45 · FE 22 · Review 5 · Báo cáo 5 · Integration 13.

**Business rules chính:** BR-04, BR-05, BR-06, BR-12; LEADER/MEMBER theo `groupId` (không có role hệ thống); một record `tasks` duy nhất cho task nhóm; Assignment PENDING/ACCEPTED/DECLINED/REVOKED độc lập Task status; decline bắt buộc reason; AI chỉ gợi ý → backend validate → user xác nhận.

## Task của tôi

| ID | Task | P | h | Depends | Branch | Acceptance | Tests | Date |
|---|---|---|---|---|---|---|---|---|
| GR-00 | Nháp V6, V7 (partial unique PENDING/ACCEPTED), V10 | P0 | 2 | ERD | trong `feature/contract-group-assignment-ai` | Khớp ERD | review TV1 | 11-18 |
| GR-01 | Contract group/member/assignment/AI + interface | P0 | 5 | F-04 | `feature/contract-group-assignment-ai` | Stub 501; chữ ký `GroupAccessApi`, `AssignmentApi` | build | 11-19 |
| GR-02 | Group CRUD (creator = LEADER) | P0 | 6 | F-02 | `feature/group-crud` | Member xem; leader sửa | tests | 11-20 |
| GR-03 | Members + `GroupAccessApi` | P0 | 6 | GR-02 | `feature/group-members` | BR-06; UNIQUE(group, user) | permission | 11-21 |
| AS-01 | Leader giao task + BR-04 ở service | P0 | 6 | GR-03, TK-04 | `feature/assignment-create` | `createGroupTask` (1 record); PENDING; trùng PENDING/ACCEPTED → 409 `ASSIGNMENT_ALREADY_ACTIVE` | tests | 11-22 |
| AS-02 | Accept / decline / revoke + list + notify | P0 | 4 | AS-01 | `feature/assignment-respond` | BR-05; decline thiếu reason → 400; `acceptedTaskIds` | permission matrix | 11-23 |
| AI-01 | `AiClient` + Gemini + Fake | P0 | 5 | GR-01 | `feature/ai-client` | Timeout 5s; key chỉ từ env; Fake khi thiếu key | `MockRestServiceServer` | 11-24 |
| AI-02 | `POST /v1/ai/tasks/parse` + validate + log | P0 | 7 | AI-01 | `feature/ai-task-parse` | Draft, không lưu; 503 khi lỗi; log hash | tests | 11-25 (freeze riêng 11-26 12:00 nếu cần) |
| AS-PG | `@Tag("pg")` partial unique V7 | P0 | 1 | F-03, AS-01 | `test/assignment-unique-pg` | 2 PENDING → vi phạm; DECLINED + PENDING ok | pg | 11-26 |
| FE-GR-01 | Groups + members | P0 | 6 | FE-01, GR-03 | `feature/fe-groups` | | E2E thủ công | 11-23 → 11-26 |
| FE-AS-01 | Leader giao task + danh sách | P0 | 5 | AS-02 | `feature/fe-assignments` | | E2E thủ công | 11-27 |
| FE-AS-02 | Inbox accept/decline + lưới BUSY/FREE | P0 | 6 | FE-AS-01, GR-04 | (cùng FE-AS-01) | Không hiện title riêng tư; luồng 4 + 6 | E2E thủ công | 11-28 |
| FE-AI | AI parse → draft → lưu; 503 → nhập tay | P0 | 5 | AI-02, FE-TK-02 | `feature/fe-ai-parse` | Luồng 5; chạy được khi tắt Gemini | E2E thủ công | 11-29 |
| RP-05 | Báo cáo: Group / Assignment / AI | P0 | 5 | RP-01 | ngoài repo | Đúng mẫu | — | 11-30 |
| IT-DRY / IT-02 / IT-03 | Dry-run, E2E, regression | P0 | ~13 | — | `fix/*` | | E2E | 11-29 → 12-02 |

## Lịch từng ngày

- [ ] **11-18 (T4):** setup env (`bash scripts/install-hooks.sh`, `./scripts/setup-dev.sh --docker`, `./gradlew build`); đọc doc phần Group/Assignment/AI + ERD; GR-00 nháp V6, V7, V10.
- [ ] **11-19 (T5):** GR-01 (mở PR kèm nháp migration trước 11:00); sửa theo review TV1.
- [ ] **11-20 (T6):** GR-02.
- [ ] **11-21 (T7):** GR-03.
- [ ] **11-22 (CN):** AS-01.
- [ ] **11-23 (T2):** AS-02 (TV2 chờ cho SC-05b); FE-GR-01 bắt đầu; chốt model Gemini + người giữ key với nhóm (CG-08).
- [ ] **11-24 (T3):** AI-01; FE-GR-01.
- [ ] **11-25 (T4):** AI-02 (**Backend P0 freeze**; nếu chưa xong → freeze riêng 11-26 12:00).
- [ ] **11-26 (T5):** AI-02 freeze 12:00 (nếu cần); AS-PG; FE-GR-01 xong.
- [ ] **11-27 (T6):** FE-AS-01.
- [ ] **11-28 (T7):** FE-AS-02.
- [ ] **11-29 (CN):** FE-AI; IT-DRY.
- [ ] **11-30 (T2):** fix (1h); RP-05 (5h).
- [ ] **12-01 (T3):** IT-02.
- [ ] **12-02 (T4):** IT-03.
- [ ] **12-03 (T5):** kiểm tra cuối.

Khung review mỗi ngày: 11:00 và 16:00.

## Phụ thuộc

**Upstream (tôi chờ):**
- TV1: F-01/F-04 (11-18) → GR-01; DB ready (11-19) → CRUD; FE-01 (11-22 16:00), FE-02/03 (11-23) → FE; F-03 (11-25) → AS-PG; GR-04 (11-26) → FE-AS-02.
- TV3: TK-04 `createGroupTask` (11-21) → AS-01; FE-TK-02 (11-27) → FE-AI.

**Downstream (chờ tôi):**
- TV2: `AssignmentApi.acceptedTaskIds` AS-02 (11-23) → SC-05b.
- TV1: GR-03 `GroupAccessApi` (11-21) → GR-04.
- TV3: `GET /v1/assignments?status=ACCEPTED` (11-23) → FE-TK-02.

## API sở hữu / tiêu thụ

| Sở hữu | Tiêu thụ |
|---|---|
| `GET/POST /v1/groups`, `GET/PUT /v1/groups/{id}` | `TaskApi.createGroupTask` (TV3) |
| `POST /v1/groups/{id}/members`, `PATCH/DELETE /v1/groups/{id}/members/{userId}` | `UserApi` (TV1) — tìm user theo email |
| `GET/POST /v1/assignments`, `POST /v1/assignments/{id}/accept`, `/decline`, `/revoke` | `NotificationApi` (TV1, no-op) |
| `POST /v1/ai/tasks/parse` | `GET /v1/groups/{id}/availability` (TV1) — FE-AS-02 |
| `GroupAccessApi`, `AssignmentApi` | `POST /v1/tasks` (TV3) — FE-AI lưu draft |

Không có `POST /groups/{id}/join` trong MVP (G8). Gemini: key chỉ ở `.env` (`GEMINI_API_KEY`), không commit, không gửi dữ liệu nhạy cảm.

## PR tôi phải review (SLA ≤ 4h, khung 11:00 / 16:00)

- TV3: TK-01..TK-04, AV-02, AV-04.
- FE của TV2: FE-CAL, FE-SC-01..03.
- FE của TV3: FE-TK-01/02, FE-AV.

PR của tôi (BE + FE) do **TV3** review; contract/migration do TV1 review.

## Báo cáo

RP-05 — phần Group / Assignment / AI (quyền theo nhóm, BR-04/05, Gemini + fallback, ảnh UI), 5h, ngày 11-30.

## Definition of Done áp dụng

Xem [06_TEST_DOD.md](../06_TEST_DOD.md). Thêm: ma trận quyền (leader / member / người ngoài) cho mọi endpoint; BR-04 kiểm ở cả service và `@Tag("pg")`; AI test timeout / 429 / JSON sai / thiếu key.

## Progress log

| Date | Done | Blocked | Next |
|---|---|---|---|
| | | | |
