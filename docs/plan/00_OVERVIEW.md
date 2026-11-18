# 00 — Tổng quan

[← README](README.md)

## 1. Trạng thái đã kiểm chứng (audit 2026-11-18)

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Java / Spring Boot / Gradle | VERIFIED | Java 25.0.1, Spring Boot 4.1.2, Gradle wrapper 9.8.2 |
| Package | PARTIAL | `group16.webapp.{common,config}`; `modules/` rỗng |
| PostgreSQL / Docker | PARTIAL | `Code/backend/compose.yaml` (postgres:16); kết nối chưa kiểm chứng trong audit |
| Flyway | PARTIAL | Chỉ `V1__create_users_table.sql` |
| JPA entity | PARTIAL | `BaseEntity`, `AuditableEntity`; chưa có `User` |
| Security / JWT / CORS | PARTIAL | `SecurityConfig` có HS256, BCrypt, CORS, 401/403 JSON; chưa phát token |
| Error format | VERIFIED | `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler` |
| Tests | VERIFIED | `./gradlew test --rerun-tasks --offline` → 34/34 pass, H2, `@ActiveProfiles("test")` |
| CI | VERIFIED | `gitflow-check.yml`: job `gitflow` + `test`; chưa có job frontend/pg |
| 7 module MVP | MISSING | Chưa có code nghiệp vụ |
| Frontend | MISSING | `Code/frontend/` chỉ có `CLAUDE.md`, `.gitignore`, `.env.example` rỗng |

Nguồn thiết kế: `PlanWise Document/PlanWise AI Document .docx` (BR-01..15, HC-01..09), `PlanWise Document/system design/ERD.png`. Quyết định nhóm D1–D11 (`Code/backend/CLAUDE.md`), G1–G11, E1–E6 (xem [01_CONTRACT_GATES.md](01_CONTRACT_GATES.md)).

## 2. Foundation blockers (TV1, theo thứ tự)

1. F-01 + F-04: skeleton `modules/*`, `ErrorCode` đầy đủ, `ConflictErrorDto` + `ConflictErrorResponse`, `NotificationApi` no-op (11-18).
2. F-02: V2, V9 + cấp số + merge V2..V10 đúng thứ tự, chạy thử trên PostgreSQL (11-19).
3. F-05: snapshot OpenAPI — contract frozen (11-20 10:00).
4. AU-01..03: User + register/login/JWT 24h + `/users/me` (11-21).
5. FE-00..03: scaffold frontend (11-23).
6. F-03: Testcontainers + CI `pg` (11-25).

Module test dùng `jwt()` của spring-security-test nên **TV2–TV4 không phải chờ Auth** để bắt đầu CRUD.

## 3. Phân công module

| Thành viên | Vai trò | Sở hữu | Migration nháp |
|---|---|---|---|
| TV1 Huỳnh Thanh Kiệt | Foundation & Tech Lead | Foundation, migration numbering, OpenAPI contract, Security/JWT, Auth & Profile (BE+FE), FE scaffold, GR-04, CI, Testcontainers, QA, release, ghép báo cáo | V2 `password_reset_tokens`, V9 `notifications` |
| TV2 Võ Thị Kim Kiều | Feature Dev | Smart Scheduling & Schedule Management (BE+FE, Calendar view) | V8 `schedule_proposals`, `schedule_sessions` |
| TV3 Phan Ký Khôi | Feature Dev | Personal Tasks, Availability & Fixed Events, Calendar query (BE+FE), Dashboard (P1) | V3 `tasks`, V4 `availability_rules`, V5 `fixed_events` |
| TV4 Hồ Ngọc Anh Thư | Feature Dev | Team, Task Assignment, AI Task Parsing (BE+FE) | V6 `team_groups` + `group_members`, V7 `task_assignments`, V10 `ai_request_logs` |

Owner đơn nhất: **Flyway numbering, OpenAPI contract, Security interfaces = TV1.**

Lý do: Scheduling là phần khó nhất và nằm trên critical path → giao trọn cho TV2, ít việc phụ. Task/Availability là input của Scheduling → cùng một owner (TV3). Group → Assignment liền mạch, AI độc lập → TV4. TV1 chỉ giữ Auth (nhỏ, là hạ tầng bảo mật, chặn FE) và GR-04 (2h).

## 4. Cân bằng tải (giờ, không tính P1)

| | BE (gồm test) | FE | Review | Báo cáo | Integration/QA/Release | **Tổng** | Capacity |
|---|---|---|---|---|---|---|---|
| TV1 | 24 | 23 | 11 | 9 | 23 | **90** | 90 |
| TV2 | 47 | 22 | 1 | 5 | 15 | **90** | 90 |
| TV3 | 40 | 24 | 4 | 5 | 17 | **90** | 90 |
| TV4 | 45 | 22 | 5 | 5 | 13 | **90** | 90 |
| **Tổng** | 156 (43%) | 91 (25%) | 21 (6%) | 24 (7%) | 68 (19%) | **360** | 360 |

- Lệch so với mục tiêu 45/40/15: giờ BE đã gồm test bắt buộc; "Integration" gồm dry-run, fix, 2 ngày đệm, release. FE + integration ≈ 44%.
- TV2 thừa 1h BE: test concurrency SC-07 tràn sang sáng 11-26.
- **Phương án thay thế khi quá tải:** nếu SC-05a trễ > 4h vào 11-23 → TV1 nhận SC-06 (dùng giờ hỗ trợ 11-27/28); nếu vẫn trễ → đề xuất cắt C-3 (cần duyệt, xem [07_RISKS_MVP_CUTS.md](07_RISKS_MVP_CUTS.md)).
- P1 (ND-01, FE-ND, AU-04, DB-01, FE-DB ≈ 19h) không nằm trong bảng trên; mặc định CUT.
