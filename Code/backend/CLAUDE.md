# CLAUDE.md — Backend (Code/backend)

General rules (approval workflow, git, security, source of truth) are in the root `CLAUDE.md`. This file adds backend rules.

## Stack
Java 25, Spring Boot 4.1.x, Spring Security, Spring Data JPA, Bean Validation, Flyway (`spring-boot-starter-flyway`; migrations in `src/main/resources/db/migration`, Hibernate `ddl-auto=validate`), Lombok (only if already used). Build: **Gradle 9.8** (wrapper).
DB: PostgreSQL 16, UUID PKs, IANA timezone support. No Redis for MVP.
Check `build.gradle` before adding dependencies. Never bump versions or swap working libraries.

## Commands (run in `Code/backend/`; verify against build.gradle / README first)
- Build: `./gradlew build`
- Test: `./gradlew test` (single: `./gradlew test --tests "ClassName"`)
- Run: `./gradlew bootRun`
- DB: `docker compose up -d` (only if a compose file exists), or `./scripts/setup-dev.sh --docker` from repo root
Never run migrations or data-changing commands without approval.

## Configuration & Environment
- `Code/backend/.env` (KEY=VALUE) is loaded automatically via `spring.config.import=optional:file:.env[.properties]` in `application.properties`. Precedence: real OS env vars > `.env` > defaults in `application.properties`.
- No `.env` -> fallback to H2 in-memory (`jdbc:h2:mem:planwise`). Never rely on this silently: when PostgreSQL is expected, confirm it.
- Keys: `SERVER_PORT` (default 8080), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `CORS_ALLOWED_ORIGINS`.
- Local PostgreSQL: container `planwise-postgres` (PostgreSQL 16), DB `planwise_ai`, user `planwise_user`, port 5432. `setup-dev.sh --docker` creates the container from the DB name/user/password in `.env`, so keep them in sync.
- `.env` is personal and secret: never read, print, edit, or commit it. To check it, compare against the container config without printing values. New keys go to `.env.example` with placeholders only.
- Verify a PostgreSQL connection by running `./gradlew bootRun` and checking the log for `HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection` (`jdbc:h2` = fell back to H2), plus `pg_stat_activity` on the DB side. Stop the app afterwards.
- Port 8080 may be taken by another local service (e.g. nginx); use `SERVER_PORT` instead of changing code.
- `POSTGRES_PASSWORD` on the container only applies when its data volume is first initialized; if the volume is recreated, update `DB_PASSWORD` in `.env` to match.

## Architecture
Modular Monolith + Layered: Controller -> Service -> Repository -> PostgreSQL.
- Controller: HTTP, validation, responses. No complex business logic.
- Service: business logic. Repository: data access. DTOs for request/response. Entity = persistence model.
- Never return JPA entities from the API.
- Scheduling Engine: deterministic algorithm, not LLM-dependent.
- AI Service: provider calls + output validation, behind an interface/adapter.
- No microservices, no extra frameworks. Do not rename Java packages, replace the build tool, change the API contract, or delete/rewrite modules.
- If the architecture seems wrong: explain cause, impact, options; wait for approval.

## Database
10 tables: `users`, `tasks`, `availability_rules`, `fixed_events`, `schedule_sessions`, `team_groups`, `group_members`, `task_assignments`, `notifications`, `ai_request_logs`.
- Match ERD + data dictionary exactly before writing entities/migrations. No adding/dropping/renaming tables or columns without approval.
- snake_case, explicit PK/FK, unique + check constraints. Versioned Flyway migrations; never edit applied migrations; no destructive migrations without approval.

## Business Rules (full list: BR-01..BR-32 in design doc)
**Tasks**
- Priority: LOW, MEDIUM, HIGH, URGENT. Status: TODO, IN_PROGRESS, COMPLETED, CANCELLED.
- Valid title; estimated duration = positive integer within limit.
- `is_overdue` computed from deadline + status, never stored. Soft delete per design.
- Never schedule COMPLETED/CANCELLED tasks.

**Scheduling**
- Only within valid availability; no overlap with fixed events or CONFIRMED sessions; never past deadline.
- Order: priority desc, then deadline asc. Don't split tasks unless configured.
- Not enough time -> return conflict info, never a wrong schedule.
- Proposals are PROPOSED, grouped by `proposalBatchId`. CONFIRMED is protected (never overwritten/moved).
- User confirms explicitly; re-check conflicts at confirm time with transaction + concurrency control.

**Group & Assignment**
- Group creator = LEADER (manages group, assigns tasks). MEMBER can ACCEPT/DECLINE; DECLINE requires a reason.
- Assignment status: PENDING, ACCEPTED, DECLINED, REVOKED — independent from Task status.
- One source record per task in `tasks`; never clone group tasks into personal tasks. Link via `task_assignments`.
- Only ACCEPTED assignments enter a member's to-do list. Enforce authorization in backend.

**Privacy**
- LEADER sees only shareable BUSY/FREE intervals. Never expose personal task titles, private fixed-event content, personal notes, or unauthorized session details via DTO/API.

**AI**
- Provider NOT chosen (Gemini or OpenAI under consideration). Don't pick provider/model or call real APIs without approval.
- Flow: AI suggests -> backend validates -> user confirms.
- Uses: parse natural language into a structured task; suggest fixes for overload.
- AI never writes official tasks, changes confirmed schedules, or touches the DB.
- Validate JSON, types, deadline, priority, duration. On timeout / invalid JSON / quota: fail safely, allow manual input.

## Coding Conventions
- PascalCase types, camelCase methods/vars, UPPER_SNAKE_CASE constants, lowercase packages.
- Constructor injection. DTOs + Bean Validation. Centralized exception handling.
- No duplicated code, no needless abstraction, no hardcoded secrets.

## REST API
- JSON, correct HTTP methods/status codes, base path `/api/v1`.
- Consistent success/error format, meaningful error codes, no stack traces to clients.
- OpenAPI/Swagger where suitable.
- Check the existing API contract before standardizing; don't impose a new response format without team approval.
- Verify authentication + authorization on sensitive endpoints.

## Testing
- JUnit. Mockito / Spring Boot Test / Testcontainers only after checking existing deps and getting approval to add.
- Every `@SpringBootTest` (and other Spring context tests) MUST use `@ActiveProfiles("test")`. The `test` profile (`src/test/resources/application-test.properties`) forces an isolated H2 DB (`jdbc:h2:mem:planwise_test`, `create-drop`), so tests never touch the developer's PostgreSQL even when `.env` exists. Never point tests at the dev DB.
- Run `./gradlew clean build` if tests fail with stale-class errors (e.g. "Unable to find a @SpringBootConfiguration").
- Cover: valid/invalid input, edge cases, invalid states, missing permission, not found, DB constraints, conflicts/concurrency, API responses, cross-module behavior.
- Scheduling: overlap, deadline, availability, confirmed sessions, overload, concurrency.
- Assignment: accept, decline, revoke, permissions.
- No fake tests; don't mock away the business logic under test.
