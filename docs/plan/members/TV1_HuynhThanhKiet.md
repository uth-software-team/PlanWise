# TV1 — Huỳnh Thanh Kiệt (Foundation & Technical Lead)

[← README](../README.md) · Nguồn sự thật WBS: [02_WBS.md](../02_WBS.md)

## Vai trò

- Foundation backend, Flyway numbering + merge migration, OpenAPI contract owner, Security/JWT owner.
- Auth & Profile (BE + FE) — nhỏ nhưng chặn FE và là hạ tầng bảo mật.
- FE scaffold, CI, Testcontainers, QA, integration, release, ghép báo cáo.
- Không làm phần lớn business feature; chỉ thêm GR-04 (2h).

**Package/file sở hữu:** `common/**`, `config/**`, `modules/auth`, `modules/notification`, `db/migration` (cấp số + merge), `docs/api/openapi-v1.json`, `build.gradle`, `application*.properties`, `.github/workflows/**`, FE `src/app`, `src/routes`, `src/api`, `src/components` (shared), `package.json`.

**Tổng giờ:** 90h = BE 24 · FE 23 · Review 11 · Báo cáo 9 · Integration/QA/Release 23 (P1 không tính: AU-04 4h, ND-01 5h, FE-ND 3h — mặc định CUT).

## Task của tôi

| ID | Task | P | h | Depends | Branch | Acceptance | Tests | Date |
|---|---|---|---|---|---|---|---|---|
| F-01 | Module skeleton + quy ước gọi chéo | P0 | 2 | — | `feature/foundation-module-skeleton` | 9 package; CG-02 trong README | build | 11-18 |
| F-04 | `ErrorCode` + `ConflictErrorDto` + `ConflictErrorResponse` + `NotificationApi` no-op | P0 | 2 | — | (cùng F-01) | Đủ mã CG-05 | handler test 409 | 11-18 |
| F-02 | V2, V9 + cấp số + review/merge V3..V10 + chạy thử PG | P0 | 3 | F-01, contract PRs | `feature/db-auth-notification-schema` | Flyway lên v10 trên PG, `validate` ok | bootRun PG | 11-19 16:00 |
| F-05 | Snapshot OpenAPI | P0 | 1 | TK-01, SC-01, GR-01 | `docs/api-contract-v1` | Đủ endpoint MVP | đối chiếu doc | 11-20 10:00 |
| AU-01 | `User` entity/repo + `UserApi` | P0 | 2 | F-02 | `feature/auth-register-login` | Map đúng V1 | repo | 11-20 |
| AU-02 | Register/login/logout + `JwtService` + 24h | P0 | 5 | AU-01 | (cùng AU-01) | 201/409/401; `exp` 24h; `.env.example` 1440 | WebMvc + service | 11-21 |
| AU-03 | `GET/PUT /v1/users/me` | P0 | 2 | AU-02 | `feature/auth-profile` | Timezone IANA; planning window hợp lệ | validation | 11-21 |
| FE-00 | Vite + TS + Tailwind + shadcn + lint + CI FE | P0 | 3 | CG-13 | `feature/fe-scaffold` | lint/build xanh trong CI | build/lint | 11-22 |
| FE-01 | Router, layout, nav, auth guard | P0 | 4 | FE-00 | (cùng FE-00) | Redirect `/login` | build/lint | 11-22 16:00 |
| FE-02 | API client + JWT + 401 + types OpenAPI | P0 | 4 | F-05, FE-01 | `feature/fe-api-client` | Không `any` | build/lint | 11-23 16:00 |
| FE-03 | Shared UI (4 trạng thái, form, toast, tz utils) | P0 | 2 | FE-01 | (cùng FE-02) | Dùng lại được | build/lint | 11-23 |
| FE-AU | Login / Register / Profile | P0 | 6 | FE-02, AU-03 | `feature/fe-auth` | Luồng 1 qua UI | E2E thủ công | 11-24 |
| F-03 | Testcontainers + CI `pg` | P0 | 3 | F-02 | `test/db-migration-pg` | `@Tag("pg")` xanh trong CI | pg | 11-25 |
| QA-01 | Checklist freeze backend | P0 | 2 | BE P0 | — | Không còn stub 501 (trừ AI-02/CG-09a) | build | 11-25 |
| GR-04 | `GET /v1/groups/{id}/availability` chỉ BUSY | P0 | 2 | GR-03, CAL-01 | `feature/group-availability` | Không lộ title/note | privacy | 11-26 |
| IT-01 | Checklist E2E 6 luồng | P0 | 4 | — | `docs/e2e-demo-flows` | Bước + kết quả mong đợi | — | 11-27 |
| RP-01 | Dàn ý + mẫu báo cáo + phân phần | P0 | 1 | — | ngoài repo | | — | 11-27 |
| RP-02 | Báo cáo: Foundation / Auth / kiến trúc | P0 | 5 | RP-01 | ngoài repo | Đúng mẫu | — | 11-28 → 11-30 |
| IT-DRY | Chủ trì dry-run 6 luồng | P0 | 4 | FE P0 | — | Bug list có owner | E2E | 11-29 |
| IT-GATE | Gate 6 luồng qua UI | P0 | — | IT-DRY | — | Pass → xét mở P1 | E2E | 11-30 |
| IT-02 | E2E chính thức + fix | P0 | 6 | IT-GATE | `fix/*` | 6 luồng pass | E2E | 12-01 |
| IT-03 | Regression + README/setup (chủ trì) | P0 | 3 | IT-02 | `docs/setup-guide` | Chạy từ máy mới | regression | 12-02 |
| RP-06 | Ghép, soát, xuất báo cáo | P0 | 3 | RP-02..05 | ngoài repo | Bản cuối | — | 12-02 |
| RL-01 | `release/1.0.0` → `main` → tag `v1.0.0` → merge ngược | P0 | 2 | IT-03 | `release/1.0.0` | CI xanh | CI | 12-02 / 12-03 |
| AU-04 | Forgot/reset password | P1 | 4 | gate | `feature/auth-password-reset` | BR-14 | tests | sau 11-30 / CUT |
| ND-01 | Notifications list / mark read | P1 | 5 | gate | `feature/notification-basic` | BR-13 | tests | sau 11-30 / CUT |
| FE-ND | Chuông thông báo | P1 | 3 | ND-01 | `feature/fe-notifications` | | — | sau 11-30 / CUT |

## Lịch từng ngày

- [ ] **11-18 (T4):** F-01, F-04 (mở PR); mời TV2–4 vào repo, đặt ruleset approvals = 1 (I-1).
- [ ] **11-19 (T5):** F-02 (V2, V9, cấp số); review + merge contract PRs và migration; chạy thử chuỗi V2..V10 trên PG → công bố **DB ready 16:00**.
- [ ] **11-20 (T6):** F-05 lúc 10:00 (**contract frozen**); AU-01; AU-02.
- [ ] **11-21 (T7):** AU-02 xong; AU-03 (**Security ready**); FE-00 bắt đầu.
- [ ] **11-22 (CN):** FE-00 xong; FE-01 (merge 16:00); FE-02 bắt đầu.
- [ ] **11-23 (T2):** FE-02 xong (16:00); FE-03 (**FE scaffold ready**); chốt CG-08 với TV4.
- [ ] **11-24 (T3):** FE-AU.
- [ ] **11-25 (T4):** F-03; QA-01 (**Backend P0 freeze**).
- [ ] **11-26 (T5):** GR-04; IT-01.
- [ ] **11-27 (T6):** IT-01 xong; RP-01; hỗ trợ FE (sẵn sàng nhận SC-06 nếu R1 xảy ra).
- [ ] **11-28 (T7):** hỗ trợ FE; RP-02.
- [ ] **11-29 (CN):** IT-DRY (chủ trì); review.
- [ ] **11-30 (T2):** IT-GATE; fix (4h); RP-02 xong (2h). Nếu gate pass → đề xuất mở P1 cho user duyệt.
- [ ] **12-01 (T3):** IT-02.
- [ ] **12-02 (T4):** IT-03; RP-06; tạo `release/1.0.0`.
- [ ] **12-03 (T5):** RL-01; nộp code + báo cáo.

Khung review mỗi ngày: 11:00 và 16:00.

## Phụ thuộc

**Upstream (tôi chờ):**
- TV3 TK-01, TV2 SC-01, TV4 GR-01 (contract + nháp migration) — 11-19 sáng → F-02, F-05.
- TV4 GR-03 (11-21), TV3 CAL-01 (11-25) → GR-04.
- Toàn bộ FE P0 → IT-DRY / IT-GATE.
- RP-03..05 từ TV2–4 (11-30) → RP-06.

**Downstream (chờ tôi):**
- Mọi người: F-01/F-04 (11-18), DB ready (11-19), contract frozen (11-20).
- TV2–4: FE-01 (11-22 16:00) và FE-02/03 (11-23) để làm FE.
- TV4: F-03 (11-25) → AS-PG; GR-04 (11-26) → FE-AS-02.

## API sở hữu / tiêu thụ

| Sở hữu | Tiêu thụ |
|---|---|
| `POST /v1/auth/register`, `/v1/auth/login`, `/v1/auth/logout` | `GroupAccessApi` (TV4) — cho GR-04 |
| `GET/PUT /v1/users/me` | `CalendarApi.busyIntervals` (TV3) — cho GR-04 |
| `GET /v1/groups/{id}/availability` (CG-11) | |
| `NotificationApi` (no-op; thật ở ND-01 — P1) | |
| `UserApi` | |
| P1: `/v1/auth/password/forgot`, `/v1/auth/password/reset`, `GET /v1/notifications`, `PATCH /v1/notifications/{id}/read` | |

## PR tôi phải review (SLA ≤ 4h, khung 11:00 / 16:00)

- Mọi contract PR (TK-01, SC-01, GR-01) và nháp migration.
- Toàn bộ BE của TV2 (SC-01..SC-07).
- Mọi PR đụng file chung (`ErrorCode`, `SecurityConfig`, `application*.properties`, `build.gradle`, workflow, `openapi-v1.json`, FE `router`/`src/api`/`package.json`).
- Review thay khi người khác quá SLA.

PR của tôi do **TV3** review.

## Báo cáo

RP-01 (dàn ý, 11-27), RP-02 (Foundation / Auth / kiến trúc, 5h, 11-28 → 11-30), RP-06 (ghép, 12-02).

## Definition of Done áp dụng

Xem [06_TEST_DOD.md](../06_TEST_DOD.md): DoD Task + DoD PR. Thêm cho tôi: mọi thay đổi file chung phải báo nhóm trong khung review kế tiếp; contract change phải cập nhật snapshot.

## Progress log

| Date | Done | Blocked | Next |
|---|---|---|---|
| | | | |
