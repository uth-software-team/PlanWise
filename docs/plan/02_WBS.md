# 02 — WBS (nguồn sự thật duy nhất)

[← README](README.md)

- Mỗi WBS ID = 1 GitHub Issue/PR (tối đa ~1.5 ngày). Branch luôn tạo từ `develop`.
- P0 = bắt buộc cho demo; P1 = mặc định **CUT** (R-P1), chỉ làm sau gate 11-30 khi được duyệt.
- Mọi Spring context test dùng `@ActiveProfiles("test")` (H2 `planwise_test`, Flyway off). `@Tag("pg")` = test PostgreSQL qua Testcontainers.
- Cột "Date" = ngày phải merge (hoặc giờ cụ thể nếu có).

## Foundation (TV1)

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| F-01 | Module skeleton + quy ước gọi chéo module | TV1 | P0 | 2 | — | `modules/*/package-info.java`, `Code/backend/README.md` | `feature/foundation-module-skeleton` | Đủ 9 package (CG-01); quy ước CG-02 ghi trong README | `./gradlew build` | 11-18 |
| F-04 | `ErrorCode` (CG-05) + `ConflictDto` + `NotificationService` no-op | TV1 | P0 | 2 | — | `common/exception`, `common/dto`, `modules/notification` | (cùng F-01) | Đủ mọi mã lỗi trước 11-19 để owner không phải sửa `ErrorCode`; bean no-op inject được | `GlobalExceptionHandlerTests` thêm case 409 `conflicts[]` | 11-18 |
| F-02 | V2, V9 + bảng cấp số + review nháp V3..V10 + chạy thử chuỗi trên PostgreSQL | TV1 | P0 | 3 | F-01, contract PRs | `src/main/resources/db/migration` | `feature/db-auth-notification-schema` | V2..V10 merge đúng thứ tự; khớp ERD (trừ `rest_blocks`); `bootRun` trên PG có log `PgConnection`, Flyway lên v10, `ddl-auto=validate` không lỗi | bootRun PG (log) | 11-19 16:00 |
| F-05 | Snapshot OpenAPI | TV1 | P0 | 1 | TK-01, SC-01, GR-01 | `docs/api/openapi-v1.json` | `docs/api-contract-v1` | Đủ mọi endpoint MVP dưới `/api/v1` | Đối chiếu bảng "Tổng quan REST API" của doc | 11-20 10:00 |
| F-03 | Testcontainers + job CI `pg` | TV1 | P0 | 3 | F-02 | `build.gradle`, `MigrationPgTests`, `.github/workflows/` | `test/db-migration-pg` | `@Tag("pg")` chạy Flyway + `validate` trên postgres:16 trong CI; job `test` mặc định không cần Docker | pg | 11-25 |
| QA-01 | Checklist freeze backend | TV1 | P0 | 2 | BE P0 | — | — | Không còn stub 501 (trừ AI-02 theo CG-09a); snapshot OpenAPI cập nhật; bootRun PG ok | `./gradlew build` | 11-25 |

## Auth (TV1)

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| AU-01 | `User` entity/repo + `UserQueryService` | TV1 | P0 | 2 | F-02 | `modules/auth` | `feature/auth-register-login` | Map đúng V1; email trim + lowercase | repository (H2) | 11-20 |
| AU-02 | Register/login/logout + `JwtService` + expiry 24h | TV1 | P0 | 5 | AU-01 | `modules/auth`, `application.properties`, `.env.example` | (cùng AU-01) | Register 201; trùng email 409 `EMAIL_ALREADY_EXISTS`; sai mật khẩu 401 `INVALID_CREDENTIALS`; token có `sub`, `email`, `exp` = 24h; logout 204; default `JWT_EXPIRATION_MINUTES=1440` | WebMvc + service | 11-21 |
| AU-03 | `GET/PUT /v1/users/me` | TV1 | P0 | 2 | AU-02 | `modules/auth` | `feature/auth-profile` | Timezone IANA hợp lệ; `planningStart < planningEnd` | validation | 11-21 |
| AU-04 | Forgot/reset password (log link) | TV1 | P1 | 4 | gate 11-30 | `modules/auth` | `feature/auth-password-reset` | BR-14: token có hạn, dùng 1 lần, phản hồi giống nhau cho email lạ | tests | sau 11-30 / CUT |

## Tasks, Availability, Calendar (TV3)

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| TK-00 | Nháp migration V3 `tasks`, V4 `availability_rules`, V5 `fixed_events` | TV3 | P0 | 2 | ERD | `db/migration` | trong `feature/contract-task-availability` | Khớp ERD; CHECK ONCE/WEEKLY như ERD | review TV1 | 11-18 (nộp 11-19 sáng) |
| TK-01 | Contract Task/Availability/FixedEvent/Calendar + interface CG-06 | TV3 | P0 | 5 | F-01, F-04 | `modules/task`, `modules/availability` | `feature/contract-task-availability` | DTO records + controller stub 501 hiện trong Swagger; `TaskQueryService`, `AvailabilityQueryService`, `CalendarQueryService` có chữ ký | build | 11-19 |
| TK-02 | Task entity + CRUD (gồm test) | TV3 | P0 | 7 | F-02, TK-01 | `modules/task` | `feature/task-crud` | BR-01/02/03; owner-only (user khác → 404); soft delete; `version`; `isOverdue` tính, không lưu | WebMvc + service | 11-21 |
| TK-03 | `PATCH /tasks/{id}/status` + filter + paging (gồm test) | TV3 | P0 | 3 | TK-02 | `modules/task` | `feature/task-status-filter` | Lọc status/priority/deadline; `PageResponse` | tests | 11-21 |
| TK-04 | `TaskQueryService` impl | TV3 | P0 | 2 | TK-02 | `modules/task` | (cùng TK-03) | Chỉ TODO/IN_PROGRESS chưa xoá (HC-08); `createGroupTask` tạo 1 record `tasks` có `group_id` | unit | 11-21 |
| AV-02 | AvailabilityRule CRUD (gồm test) | TV3 | P0 | 4 | F-02 | `modules/availability` | `feature/availability-rules` | `day_of_week` 1–7; start < end; owner-only | tests | 11-22 |
| AV-04 | Expand availability → khoảng `Instant` theo timezone (DST-safe) | TV3 | P0 | 3 | AV-02 | `modules/availability` | (cùng AV-02) | Half-open; đúng timezone user | unit ≥ 5 case (có zone có DST) | 11-23 |
| AV-03 | FixedEvent CRUD ONCE/WEEKLY + expand occurrences | TV3 | P0 | 5 | AV-04 | `modules/availability` | `feature/fixed-events` | CHECK ONCE/WEEKLY; `effectiveFrom/Until`; occurrences đúng tz | tests | **11-24 11:00** |
| CAL-01 | `CalendarQueryService.busyIntervals` + `GET /v1/calendar?start&end` | TV3 | P0 | 5 | AV-03, SC-05a | `modules/availability` | `feature/calendar-busy` | Trả sessions + fixed events của chính user | tests | 11-25 |
| AV-05 | Bổ sung test availability/calendar | TV3 | P0 | 1 | CAL-01 | tests | (cùng CAL-01) | Case biên: khoảng rỗng, chạm biên half-open | — | 11-25 |
| DB-01 | `GET /v1/dashboard/summary` tối thiểu | TV3 | P1 | 3 | gate 11-30 | `modules/dashboard` | `feature/dashboard-summary` | BR-15: đếm theo taskId, không trùng | tests | sau 11-30 / CUT |

## Scheduling (TV2) — critical path

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| SC-02 | Engine model + `TimeInterval` + tính free slots | TV2 | P0 | 6 | — | `modules/schedule/engine` | `feature/schedule-engine-slots` | Java thuần, không Spring; available − busy; half-open | unit ≥ 8 | 11-20 |
| SC-01 | Contract scheduling + `ScheduleQueryService` + nháp V8 | TV2 | P0 | 5 | F-04 | `modules/schedule`, `db/migration` | `feature/contract-schedule` | Theo CG-07; stub 501 trong Swagger; V8 khớp ERD (`version`, UNIQUE, FK composite) | build | 11-19 |
| SC-03 | Allocation xác định + earliest-fit + reasons | TV2 | P0 | 8 | SC-02 | `modules/schedule/engine` | `feature/schedule-engine-allocation` | Sắp xếp: priority desc, deadline asc, remaining, createdAt, taskId; BR-08 (1 session liên tục/task); `INSUFFICIENT_TIME`, `DEADLINE_PASSED`; deterministic | unit ≥ 12 (deadline, overload, hoà) | 11-21 |
| SC-04 | `ConstraintValidator` HC-01..09 | TV2 | P0 | 6 | SC-02 | `modules/schedule/engine` | `feature/schedule-constraint-validator` | Trả đúng mã cho từng HC | unit ≥ 1 case/HC | 11-22 |
| SC-05a | Proposal/Session entity + generate service/endpoint trên interface mock | TV2 | P0 | 5 | SC-03, SC-01 (interface CG-06) | `modules/schedule` | `feature/schedule-generate` | Chỉ tạo PROPOSED; 1 batch PROPOSED/user (batch cũ bị thay); không đụng CONFIRMED/COMPLETED; 200 + `unscheduledTasks` | service (Mockito) + WebMvc | 11-23 |
| SC-05b | Nối impl thật: `TaskQueryService`, `AvailabilityQueryService`, `AssignmentQueryService` | TV2 | P0 | 5 | SC-05a, TK-04, AV-04, AV-03, AS-02 | `modules/schedule` | `feature/schedule-generate-wiring` | Generate với dữ liệu thật; task ACCEPTED được xếp | integration `@SpringBootTest` | 11-24 |
| SC-06 | Sửa/xoá session PROPOSED; GET/DELETE proposal | TV2 | P0 | 5 | SC-05a, SC-04 | `modules/schedule` | `feature/schedule-edit-session` | BR-10: đúng `version`, kiểm HC, tăng version; sửa CONFIRMED → 409 | tests | 11-24 |
| SC-07 | Confirm trong 1 transaction | TV2 | P0 | 7 | SC-06, SC-05b | `modules/schedule` | `feature/schedule-confirm` | BR-11: kiểm quyền, version (409 `STALE_VERSION`), toàn bộ HC; lỗi → 409 `CONSTRAINT_VIOLATION` + `conflicts[]`, rollback cả batch; khoá proposal row | tests + concurrency (2 confirm song song) — 1h tràn sang sáng 11-26 | 11-25 (+1h 11-26) |

## Team, Assignment, AI (TV4) + GR-04 (TV1)

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| GR-00 | Nháp migration V6 `team_groups`+`group_members`, V7 `task_assignments` (partial unique PENDING/ACCEPTED), V10 `ai_request_logs` | TV4 | P0 | 2 | ERD | `db/migration` | trong `feature/contract-group-assignment-ai` | Khớp ERD | review TV1 | 11-18 (nộp 11-19 sáng) |
| GR-01 | Contract group/member/assignment/AI + interface CG-06 | TV4 | P0 | 5 | F-04 | `modules/group`, `modules/assignment`, `modules/ai` | `feature/contract-group-assignment-ai` | Stub 501 trong Swagger; `GroupAccessService`, `AssignmentQueryService` có chữ ký | build | 11-19 |
| GR-02 | Group CRUD (creator = LEADER) | TV4 | P0 | 6 | F-02 | `modules/group` | `feature/group-crud` | Chỉ member xem; chỉ leader sửa | tests | 11-20 |
| GR-03 | Members (thêm bằng email, đổi role, xoá, rời) + `GroupAccessService` | TV4 | P0 | 6 | GR-02 | `modules/group` | `feature/group-members` | BR-06; UNIQUE(group, user); không lộ lịch cá nhân | permission tests | 11-21 |
| AS-01 | Leader giao task + kiểm tra BR-04 ở service | TV4 | P0 | 6 | GR-03, TK-04 | `modules/assignment` | `feature/assignment-create` | Tạo group task qua `createGroupTask` (1 record `tasks`); PENDING; task đã có PENDING/ACCEPTED → 409 `ASSIGNMENT_ALREADY_ACTIVE` | tests | 11-22 |
| AS-02 | Accept / decline (reason) / revoke + list + notify | TV4 | P0 | 4 | AS-01 | `modules/assignment` | `feature/assignment-respond` | BR-05; decline thiếu reason → 400; trạng thái Assignment độc lập Task; `acceptedTaskIds` impl | permission matrix | 11-23 |
| AS-PG | Test `@Tag("pg")` partial unique index V7 | TV4 | P0 | 1 | F-03, AS-01 | tests | `test/assignment-unique-pg` | 2 PENDING cùng task → vi phạm; DECLINED + PENDING được phép | pg | 11-26 |
| AI-01 | `AiClient` + `GeminiAiClient` + `FakeAiClient` | TV4 | P0 | 5 | GR-01 | `modules/ai` | `feature/ai-client` | RestClient timeout 5s; key chỉ từ env; Fake khi thiếu key | `MockRestServiceServer`: timeout, 429, JSON sai | 11-24 |
| AI-02 | `POST /v1/ai/tasks/parse` + validate output + `ai_request_logs` | TV4 | P0 | 7 | AI-01 | `modules/ai` | `feature/ai-task-parse` | Trả draft, không lưu task; validate title/duration/priority/deadline; lỗi → 503 `AI_UNAVAILABLE`; log hash | tests | 11-25 (freeze riêng 11-26 12:00 nếu cần) |
| GR-04 | `GET /v1/groups/{id}/availability` chỉ BUSY | TV1 | P0 | 2 | GR-03, CAL-01 | `modules/group` | `feature/group-availability` | Member/leader chỉ thấy khoảng BUSY; không title/note | privacy test | 11-26 |
| ND-01 | Notifications thật: list, mark read | TV1 | P1 | 5 | gate 11-30 | `modules/notification` | `feature/notification-basic` | BR-13 | tests | sau 11-30 / CUT |

## Frontend

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| FE-00 | Vite + React + TS strict + Tailwind + shadcn/ui + ESLint/Prettier + CI job FE | TV1 | P0 | 3 | CG-13 | `Code/frontend`, workflow | `feature/fe-scaffold` | `npm run build` + `npm run lint` xanh trong CI | build/lint | 11-22 |
| FE-01 | Router, layout, nav, auth guard | TV1 | P0 | 4 | FE-00 | `src/app`, `src/routes` | (cùng FE-00) | Route bảo vệ → `/login` khi chưa đăng nhập | build/lint | 11-22 16:00 |
| FE-02 | API client (fetch + JWT + 401) + types sinh từ OpenAPI | TV1 | P0 | 4 | F-05, FE-01 | `src/api` | `feature/fe-api-client` | Types sinh tự động, không `any` | build/lint | 11-23 16:00 |
| FE-03 | Shared UI: Loading/Empty/Error/Success, form, toast, timezone utils | TV1 | P0 | 2 | FE-01 | `src/components`, `src/lib` | (cùng FE-02) | Dùng lại được ở mọi feature | build/lint | 11-23 |
| FE-AU | Login / Register / Profile | TV1 | P0 | 6 | FE-02, AU-03 | `features/auth` | `feature/fe-auth` | Luồng 1 chạy qua UI | E2E thủ công | 11-24 |
| FE-GR-01 | Groups + members | TV4 | P0 | 6 | FE-01, GR-03 | `features/groups` | `feature/fe-groups` | Tạo nhóm, thêm member bằng email, đổi role/xoá | E2E thủ công | 11-23 → 11-26 |
| FE-TK-01 | Task list (filter, badge overdue, 4 trạng thái) | TV3 | P0 | 6 | FE-02, TK-03, AV-03 merged | `features/tasks` | `feature/fe-tasks` | | E2E thủ công | 11-24 chiều → 11-26 sáng |
| FE-TK-02 | Task form / đổi status / xoá + tab "Được giao" | TV3 | P0 | 6 | FE-TK-01, AS-02 | `features/tasks` | (cùng FE-TK-01) | Luồng 2 chạy qua UI | E2E thủ công | 11-27 |
| FE-AV | Availability rules + Fixed events (ONCE/WEEKLY) | TV3 | P0 | 8 | AV-03 | `features/availability` | `feature/fe-availability` | | E2E thủ công | 11-29 |
| FE-CAL | Lịch tuần (`GET /v1/calendar`) | TV2 | P0 | 5 | CAL-01 | `features/calendar` | `feature/fe-calendar` | Hiện sessions + fixed events, đúng timezone | E2E thủ công | 11-26 |
| FE-SC-01 | Generate + preview PROPOSED + lý do unscheduled | TV2 | P0 | 6 | SC-05b | `features/schedule` | `feature/fe-schedule-generate` | | E2E thủ công | 11-27 |
| FE-SC-02 | Sửa/xoá session PROPOSED (form) | TV2 | P0 | 5 | SC-06 | `features/schedule` | (cùng FE-SC-01) | Hiển thị lỗi HC | E2E thủ công | 11-28 |
| FE-SC-03 | Confirm + hiển thị conflicts + reload khi STALE_VERSION | TV2 | P0 | 5 | SC-07 | `features/schedule` | `feature/fe-schedule-confirm` | User phải bấm confirm rõ ràng; luồng 3 chạy qua UI | E2E thủ công | 11-29 |
| FE-AS-01 | Leader giao task + danh sách assignment | TV4 | P0 | 5 | AS-02 | `features/assignments` | `feature/fe-assignments` | | E2E thủ công | 11-27 |
| FE-AS-02 | Inbox member accept/decline (bắt buộc reason) + lưới BUSY/FREE | TV4 | P0 | 6 | FE-AS-01, GR-04 | `features/assignments`, `features/groups` | (cùng FE-AS-01) | Không hiện title riêng tư; luồng 4 + 6 chạy qua UI | E2E thủ công | 11-28 |
| FE-AI | AI parse → draft sửa được → `POST /tasks`; 503 → form thủ công | TV4 | P0 | 5 | AI-02, FE-TK-02 | `features/ai` | `feature/fe-ai-parse` | Demo chạy được khi tắt Gemini; luồng 5 | E2E thủ công | 11-29 |
| FE-ND | Chuông thông báo | TV1 | P1 | 3 | ND-01 | `features/notifications` | `feature/fe-notifications` | | — | sau 11-30 / CUT |
| FE-DB | Dashboard tối thiểu | TV3 | P1 | 4 | DB-01 | `features/dashboard` | `feature/fe-dashboard` | | — | sau 11-30 / CUT |

## Integration, Report, Release

| ID | Task | Owner | P | h | Depends | Files/Modules | Branch | Acceptance Criteria | Tests | Date |
|---|---|---|---|---|---|---|---|---|---|---|
| IT-01 | Checklist E2E cho 6 luồng demo | TV1 | P0 | 4 | — | `docs/` | `docs/e2e-demo-flows` | Mỗi luồng có bước + kết quả mong đợi | — | 11-27 |
| IT-DRY | Dry-run 6 luồng qua UI trên PostgreSQL + log bug | Cả nhóm (TV1 chủ trì) | P0 | 3–4/người | FE P0 | — | — | Danh sách bug có owner + hạn | E2E thủ công | 11-29 |
| IT-GATE | Gate: 6 luồng chạy qua UI | TV1 | P0 | — | IT-DRY + fix | — | — | Pass → mới xét mở P1 (cần duyệt) | E2E thủ công | 11-30 |
| IT-02 | E2E chính thức + fix | Cả nhóm | P0 | 6/người | IT-GATE | — | `fix/*` | 6 luồng pass | E2E + `./gradlew build` | 12-01 |
| IT-03 | Regression + README/setup (Windows/macOS/Linux) | Cả nhóm (TV1 chủ trì) | P0 | 3–6/người | IT-02 | README | `fix/*`, `docs/setup-guide` | Chạy được từ máy mới theo README | regression | 12-02 |
| RP-01 | Dàn ý + mẫu báo cáo + phân phần | TV1 | P0 | 1 | — | ngoài repo | — | Mỗi người biết phần mình | — | 11-27 |
| RP-02 | Báo cáo: phần Foundation/Auth/kiến trúc | TV1 | P0 | 5 | RP-01 | ngoài repo | — | Đúng mẫu | — | 11-28 → 11-30 |
| RP-03 | Báo cáo: phần Scheduling | TV2 | P0 | 5 | RP-01 | ngoài repo | — | Đúng mẫu | — | 11-30 |
| RP-04 | Báo cáo: phần Tasks/Availability/Calendar | TV3 | P0 | 5 | RP-01 | ngoài repo | — | Đúng mẫu | — | 11-30 |
| RP-05 | Báo cáo: phần Group/Assignment/AI | TV4 | P0 | 5 | RP-01 | ngoài repo | — | Đúng mẫu | — | 11-30 |
| RP-06 | Ghép, soát, xuất báo cáo | TV1 | P0 | 3 | RP-02..05 | ngoài repo | — | Bản cuối | — | 12-02 |
| RL-01 | `release/1.0.0` → PR vào `main` → tag `v1.0.0` → merge ngược `develop` | TV1 | P0 | 2 | IT-03 | — | `release/1.0.0` | CI xanh; tên nhánh khớp regex CI + hook (đã kiểm chứng) | CI | 12-02 / 12-03 |
