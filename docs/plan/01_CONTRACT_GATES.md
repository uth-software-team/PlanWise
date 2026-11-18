# 01 — Contract Gates

[← README](README.md)

Trạng thái: **APPROVED** = đã được duyệt; **OPEN** = cần nhóm xác nhận trước ngày freeze; **PROPOSED** = đề xuất, chờ duyệt.
Sau ngày freeze, mọi thay đổi phải qua PR riêng có TV1 + người dùng API/bảng đó duyệt.

## Quyết định nền (đã duyệt)

| ID | Quyết định | Status |
|---|---|---|
| D1–D11 | Xem `Code/backend/CLAUDE.md` > Team Decisions (BIGINT PK, 12 bảng ERD, `/api/v1`, error codes theo doc, không refresh token…) | APPROVED |
| G1–G11 | Package `modules.<name>`, migration do TV1 quản lý, Testcontainers, ApiResponse + `conflicts[]`, engine greedy earliest-fit, Gemini qua `AiClient`, logout phía client, leader thêm member bằng email, cắt `rest_blocks`, FE bắt đầu khi contract Auth+Task ổn định, CI FE | APPROVED |
| E1 | Notifications + Dashboard tối thiểu, P1 (mặc định CUT theo R-P1) | APPROVED |
| E2 | FixedEvent hỗ trợ ONCE + WEEKLY (ERD có `RecurrenceType`) | APPROVED |
| E3 | AvailabilityRule = `day_of_week` + `start_time`/`end_time` theo `users.timezone` | APPROVED |
| E4 | `GET /v1/calendar` có trong MVP (read-only) | APPROVED |
| E5 | Freeze contract bằng snapshot `docs/api/openapi-v1.json` | APPROVED |
| E6 | Model Gemini free tier + người giữ key | OPEN — hạn 11-23 (CG-08) |
| — | JWT hết hạn 24h; bài nộp gồm báo cáo Word (không slide) | APPROVED |

## Contract Gates

| ID | Quyết định | Nội dung | Owner | Freeze | Status |
|---|---|---|---|---|---|
| CG-01 | Package | `group16.webapp.modules.<auth,task,availability,schedule,group,assignment,ai,notification,dashboard>` | TV1 | — | APPROVED (G1) |
| CG-02 | Liên kết giữa module | Tham chiếu sang module khác bằng `Long xxxId`, không `@ManyToOne` xuyên module. Chỉ gọi public `*Service` / `*QueryService`, không gọi Repository của module khác | TV1 | 11-19 | OPEN |
| CG-03 | Migrations | Owner viết nháp bảng mình, TV1 cấp số + review + merge đúng thứ tự V2→V10 (R-Migration). V2 `password_reset_tokens` (TV1), V3 `tasks`, V4 `availability_rules`, V5 `fixed_events` (TV3), V6 `team_groups`+`group_members`, V7 `task_assignments` (TV4), V8 `schedule_proposals`+`schedule_sessions` (TV2), V9 `notifications` (TV1), V10 `ai_request_logs` (TV4). Số mới xin TV1 | TV1 | 11-19 | APPROVED |
| CG-04 | Auth | HS256, `sub` = userId, claim `email`, **hết hạn 24h** (`app.jwt.expiration-minutes` default 1440 + `.env.example`; ai có `.env` cũ = 60 thì tự sửa). Public chỉ `/v1/auth/**` + Swagger. FE lưu token ở `localStorage` (MVP, rủi ro XSS đã ghi nhận). 401 → FE về `/login`. Logout = client xoá token | TV1 | 11-19 | OPEN |
| CG-05 | Error format | `ApiResponse{success, code, message, data, timestamp}`. Bổ sung: `CONSTRAINT_VIOLATION` 409 (`data.conflicts[{sessionId, code, message}]`), mã HC (`SESSION_OVERLAP`, `FIXED_EVENT_OVERLAP`, `OUTSIDE_AVAILABILITY`, `INVALID_TIME_RANGE`, `PAST_TIME`, `DEADLINE_EXCEEDED`, `DURATION_EXCEEDED`, `TASK_NOT_SCHEDULABLE`, `SESSION_AFFECTED`), `AI_UNAVAILABLE` 503, `ASSIGNMENT_ALREADY_ACTIVE` 409, `NOT_IMPLEMENTED` 501 (chỉ cho stub, phải hết trước freeze BE). TV1 thêm hết ở F-04 | TV1 | 11-19 | OPEN |
| CG-06 | Interface xuyên module | `TaskQueryService` (TV3): schedulable tasks, `createGroupTask`, summaries · `AvailabilityQueryService` (TV3): available intervals, fixed-event occurrences · `CalendarQueryService.busyIntervals(userId, range)` (TV3) · `ScheduleQueryService` (TV2): sessions theo khoảng · `GroupAccessService` (TV4): `requireLeader`, `requireMember`, `memberIds` · `AssignmentQueryService.acceptedTaskIds(userId)` (TV4) · `NotificationService.notify(...)` (TV1, no-op trong MVP) | owners | 11-19 | OPEN |
| CG-07 | Scheduling I/O | `SchedulingInput{zone, now, windowStart, windowEnd, tasks, availableIntervals, busyIntervals}` → `SchedulingResult{sessions, unscheduled[{taskId, reason}]}`; khoảng half-open `[start, end)`; 1 session liên tục/task/batch; reason `INSUFFICIENT_TIME`, `DEADLINE_PASSED`. API: generate 200 + `unscheduledTasks`; edit/confirm gửi `version` | TV2 | 11-19 | APPROVED (G4/G5) |
| CG-08 | Gemini | `AiClient` (RestClient, timeout 5s); `FakeAiClient` mặc định khi thiếu `GEMINI_API_KEY`; timeout / 429 / JSON sai → 503 `AI_UNAVAILABLE`; chỉ gửi text tối thiểu; log `prompt_hash`, không log text. Chọn model free tier + người giữ key | TV4 | 11-23 | OPEN (E6) |
| CG-09 | Contract freeze | Owner mở contract PR (DTO records + controller stub 501 + nháp migration) ngày 11-19; TV1 export `docs/api/openapi-v1.json` | TV1 | **11-20 10:00** | OPEN |
| CG-09a | Freeze riêng AI-02 | Nếu AI-02 chưa xong ngày 11-25 → freeze riêng **11-26 12:00** | TV4 | 11-26 12:00 | PROPOSED |
| CG-10 | Task được giao | FE lấy task được giao qua `GET /v1/assignments?status=ACCEPTED` (kèm task summary) → tránh phụ thuộc vòng Task ↔ Assignment | TV3 + TV4 | 11-19 | OPEN |
| CG-11 | Group availability | **Endpoint bổ sung, không có trong bảng API của doc:** `GET /v1/groups/{id}/availability?start&end` → mỗi member chỉ có các khoảng BUSY, không title/note | TV1 | 11-19 | OPEN — cần duyệt |
| CG-12 | DB khi chạy app | Chạy app cần PostgreSQL (Docker). H2 chỉ dùng cho test (partial index V7 không chạy trên H2) | TV1 | 11-19 | OPEN |
| CG-13 | FE stack | Vite + React + TS strict + Tailwind + shadcn/ui; folder `features/<module>`; thêm `react-router-dom` và `openapi-typescript` (dev) — **cần duyệt dependency**; fetch wrapper (không axios, không state lib); lịch tuần tự dựng bằng Tailwind grid (không FullCalendar); chỉnh session bằng form, không drag | TV1 | 11-21 | OPEN — cần duyệt |

## Quy trình đổi contract sau freeze

1. Mở PR riêng (`feature/<module>-contract-<desc>`), ghi rõ endpoint/DTO/bảng bị đổi và lý do.
2. Cập nhật `docs/api/openapi-v1.json` (hoặc migration mới — **không sửa migration đã apply**).
3. Reviewer: TV1 + người dùng API/bảng đó. Báo nhóm trong khung review kế tiếp.
