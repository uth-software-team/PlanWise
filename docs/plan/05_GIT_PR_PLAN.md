# 05 — Git Branch & PR Plan

[← README](README.md) · WBS ID: [02_WBS.md](02_WBS.md)

## Flow

- `feature/*`, `fix/*`, `test/*`, `docs/*` → PR → `develop`.
- `release/1.0.0` từ `develop` → PR → `main` → tag `v1.0.0` → PR merge ngược vào `develop`.
- **Không bao giờ commit/push trực tiếp lên `main` hoặc `develop`.**
- Tên nhánh: cột Branch trong [02_WBS.md](02_WBS.md). Regex CI (`.github/workflows/gitflow-check.yml`) và hook `scripts/git-hooks/pre-push` chấp nhận `feature|fix|test|docs|hotfix/<kebab-case>` và `release/v?x.y.z` (vào `main` + `develop`) — đã kiểm chứng, không cần sửa CI.
- Commit: Conventional Commits, tiếng Anh, `type(scope): summary`; ghi WBS ID trong tiêu đề PR, ví dụ `feat(task): add task CRUD [TK-02]`.

## Ma trận reviewer

| PR của | Reviewer chính |
|---|---|
| Contract PR, migration, file chung, QA | TV1 |
| TV2 BE (SC-*) | TV1 |
| TV1 BE (AU-*, F-03, GR-04) và TV1 FE (FE-00..03, FE-AU) | TV3 |
| TV3 Task (TK-*) và AV-02 / AV-04 | TV4 |
| TV3 AV-03, CAL-01 | TV2 (chỉ 2 PR này — người dùng dữ liệu) |
| TV4 BE (GR-*, AS-*, AI-*) | TV3 |
| TV2 FE (FE-CAL, FE-SC-*) | TV4 |
| TV3 FE (FE-TK-*, FE-AV) | TV4 |
| TV4 FE (FE-GR-01, FE-AS-*, FE-AI) | TV3 |

## Review SLA (R-Review)

- Mỗi PR được review trong **≤ 4h làm việc**.
- 2 khung review/ngày: **11:00** và **16:00** — ai cũng kiểm tra PR đang chờ mình trong 2 khung này.
- Quá SLA → nhắn nhóm; TV1 review thay.

## CI bắt buộc trước khi merge

| Check | Từ ngày |
|---|---|
| `gitflow` (tên nhánh, commit message, file nhạy cảm) | đang có |
| `test` (`./gradlew build`, JDK 25) | đang có |
| `pg` (Testcontainers `@Tag("pg")`) | 11-25 (F-03) |
| `frontend` (`npm run lint` + `npm run build`) | 11-22 (FE-00) |

Thêm: ≥ 1 approval (khôi phục ruleset approvals = 1 khi teammate vào repo — I-1), không conflict, nhánh đã sync `develop`.

## Thứ tự merge

1. F-01 / F-04 (11-18)
2. Contract PRs kèm nháp migration + F-02 (11-19): F-02 (V2, V3 — đã merge 11-18) → TK-01 (V4–V6) → GR-01 (V7–V9) → SC-01 (V10). Tất cả merge xong trong ngày 11-19; **không ai apply migration lên PostgreSQL local trước gate DB ready**, nên Flyway luôn thấy đủ V2..V10 và chạy đúng thứ tự số. TV1 chạy thử cả chuỗi trên PG sau khi PR cuối merge rồi mới công bố DB ready.
3. F-05 (11-20 10:00)
4. AU, TK, GR, SC-02..04 song song
5. AV-02/04 → AS-01/02 → SC-05a
6. AV-03 (11-24 11:00) → SC-05b
7. SC-06 / SC-07, CAL-01, AI-01/02
8. GR-04, F-03, AS-PG, QA-01
9. FE theo module

## Chống xung đột

- PR ≤ 400 dòng, sống ≤ 1.5 ngày.
- Rebase nhánh chưa push lên `develop` mỗi sáng; nhánh đã push thì merge `develop` vào.
- File chung chỉ owner sửa (xem [03_DEPENDENCY_GRAPH.md](03_DEPENDENCY_GRAPH.md)).
- Đổi contract sau freeze: PR riêng + cập nhật snapshot + TV1 và người dùng API duyệt (xem [01_CONTRACT_GATES.md](01_CONTRACT_GATES.md)).
- Không `git reset --hard`, force push, hay `--no-verify`.
- Xoá nhánh feature sau khi merge.
