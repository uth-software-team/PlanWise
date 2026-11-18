# 06 — Testing & Definition of Done

[← README](README.md) · WBS ID: [02_WBS.md](02_WBS.md)

## Quy tắc chung

- Mọi `@SpringBootTest` / `@WebMvcTest` / `@DataJpaTest` dùng `@ActiveProfiles("test")` → H2 `jdbc:h2:mem:planwise_test`, `create-drop`, Flyway off. Test **không bao giờ** chạm PostgreSQL dev.
- Test PostgreSQL thật chỉ qua Testcontainers `@Tag("pg")` (F-03, AS-PG).
- Không fake test; không mock đi chính business logic đang test.
- Frontend: chưa có test tự động (không thêm lib test khi chưa duyệt) → dùng lint/build + E2E thủ công theo checklist IT-01.

## Trách nhiệm test

| Người | Phạm vi |
|---|---|
| TV1 Kiệt | Auth: register/login/401/403/409, JWT 24h, secret; Testcontainers migration; error format; snapshot contract vs controller; privacy GR-04; checklist E2E; regression |
| TV2 Kiều | Engine: overlap biên half-open, deadline, availability, CONFIRMED được bảo vệ, overload / `INSUFFICIENT_TIME`, deterministic; edit/confirm: version, `STALE_VERSION`, rollback toàn batch, concurrency; integration SC-05b |
| TV3 Khôi | Task: validation, lifecycle, soft delete, ownership; availability/fixed events: timezone, DST, WEEKLY, effective range; calendar; DB constraints |
| TV4 Thư | Ma trận quyền group/assignment (leader / member / người ngoài); **BR-04 hai tầng: service (409) + `@Tag("pg")` partial unique**; decline bắt buộc reason; Gemini timeout / 429 / JSON sai / fallback Fake |

## 6 luồng demo (gate 11-30, E2E 12-01)

| # | Luồng | Owner chính |
|---|---|---|
| 1 | Đăng ký → đăng nhập → sửa profile (timezone, planning window) | TV1 |
| 2 | Tạo / sửa / đổi status / xoá task; thấy badge quá hạn | TV3 |
| 3 | Khai báo availability + fixed events → generate → chỉnh session → confirm; thấy conflict khi có | TV2 + TV3 |
| 4 | Tạo nhóm → leader giao task → member accept / decline (có reason) → task ACCEPTED vào lịch member | TV4 + TV2 |
| 5 | AI parse câu tự nhiên → sửa draft → lưu task; tắt Gemini → vẫn nhập tay được | TV4 |
| 6 | Leader xem BUSY/FREE của member, không lộ title/note | TV1 + TV4 |

## Definition of Done — Task

- [ ] Đạt Acceptance Criteria trong [02_WBS.md](02_WBS.md).
- [ ] Test viết đủ (valid / invalid / not found / forbidden / edge) và `./gradlew build` xanh local.
- [ ] Không secret, không log dữ liệu nhạy cảm; DTO không lộ entity.
- [ ] Swagger hiển thị đúng; nếu đổi contract → đã cập nhật snapshot.
- [ ] FE: đủ 4 trạng thái Loading / Empty / Error / Success; `npm run lint` + `npm run build` xanh; không `any` lỏng.

## Definition of Done — PR

- [ ] Tên nhánh đúng quy ước; commit theo Conventional Commits.
- [ ] CI xanh (`gitflow`, `test`, + `pg` / `frontend` khi áp dụng).
- [ ] ≥ 1 approval trong SLA 4h; không conflict; đã sync `develop`.
- [ ] Mô tả: what / why / how tested / BR-HC liên quan / WBS ID.
- [ ] Contract change có snapshot mới + người dùng API đã duyệt.
