# PlanWise AI — Kế hoạch triển khai (v2.1)

Kế hoạch phân công và triển khai cho giai đoạn **2026-11-18 → 2026-12-03** (nộp toàn bộ: code backend + frontend + báo cáo).
Phê duyệt: 2026-11-18 (v2.1). Mọi thay đổi kế hoạch phải qua PR vào `develop` và được TV1 + người bị ảnh hưởng đồng ý.

## Nhóm

| Mã | Thành viên | Vai trò | Module chính | File cá nhân |
|---|---|---|---|---|
| TV1 | Huỳnh Thanh Kiệt | Foundation & Technical Lead | Foundation, Migration numbering, OpenAPI, Auth, FE scaffold, Integration, Release | [members/TV1_HuynhThanhKiet.md](members/TV1_HuynhThanhKiet.md) |
| TV2 | Võ Thị Kim Kiều | Feature Developer | Smart Scheduling & Schedule Management | [members/TV2_VoThiKimKieu.md](members/TV2_VoThiKimKieu.md) |
| TV3 | Phan Ký Khôi | Feature Developer | Personal Tasks, Availability & Fixed Events, Calendar | [members/TV3_PhanKyKhoi.md](members/TV3_PhanKyKhoi.md) |
| TV4 | Hồ Ngọc Anh Thư | Feature Developer | Team, Task Assignment, AI Task Parsing (Gemini) | [members/TV4_HoNgocAnhThu.md](members/TV4_HoNgocAnhThu.md) |

## Mốc chính

| Ngày | Mốc |
|---|---|
| 11-19 16:00 | ✅ DB ready (migration V2..V10 merged đúng thứ tự) |
| 11-20 10:00 | ✅ API contract frozen (`docs/api/openapi-v1.json`) |
| 11-21 | ✅ Security ready (register/login/me) |
| 11-23 | ✅ FE scaffold ready |
| 11-25 | ✅ Backend P0 freeze (GR-04, AS-PG, SC-07 concurrency test, AI-02 được giãn đến 11-26) |
| 11-30 | ✅ Gate: 6 luồng demo chạy qua UI |
| 12-01 → 12-02 | Đệm ổn định (E2E, regression, README) |
| 12-02 | ✅ Release candidate `release/1.0.0` |
| 12-03 | Nộp bài: `main` + tag `v1.0.0` + báo cáo |

## Mục lục

| File | Nội dung |
|---|---|
| [00_OVERVIEW.md](00_OVERVIEW.md) | Trạng thái đã kiểm chứng, foundation blockers, phân công, cân bằng tải |
| [01_CONTRACT_GATES.md](01_CONTRACT_GATES.md) | Quyết định cần freeze + ngày freeze + trạng thái |
| [02_WBS.md](02_WBS.md) | **Nguồn sự thật duy nhất** cho mọi WBS ID |
| [03_DEPENDENCY_GRAPH.md](03_DEPENDENCY_GRAPH.md) | Đồ thị phụ thuộc (Mermaid), critical path |
| [04_ROADMAP.md](04_ROADMAP.md) | Lịch từng ngày 11-18 → 12-03 + checkpoint |
| [05_GIT_PR_PLAN.md](05_GIT_PR_PLAN.md) | Branch, reviewer, SLA review, thứ tự merge, CI |
| [06_TEST_DOD.md](06_TEST_DOD.md) | Trách nhiệm test, 6 luồng demo, Definition of Done |
| [07_RISKS_MVP_CUTS.md](07_RISKS_MVP_CUTS.md) | Risk register + thứ tự cắt (cần duyệt) |

## Cách dùng

1. Mỗi người mở **file cá nhân** trong `members/` — đủ để làm việc hằng ngày.
2. Mỗi WBS ID = 1 GitHub Issue/PR. Đặt tên PR có WBS ID, ví dụ `feat(task): add task CRUD [TK-02]`.
3. Mỗi tối cập nhật bảng **Progress log** trong file cá nhân (qua PR `docs/...` hoặc gom vào PR feature cuối ngày).
4. Bị chặn > 2h: báo nhóm ngay, ghi vào cột Blocked.
5. Muốn đổi contract/DB sau freeze: xem [01_CONTRACT_GATES.md](01_CONTRACT_GATES.md) và [05_GIT_PR_PLAN.md](05_GIT_PR_PLAN.md).

## Quy tắc vận hành (tóm tắt)

- **R-Review:** mỗi PR được review trong ≤ 4h làm việc; 2 khung review/ngày **11:00** và **16:00**.
- **R-FE:** owner bắt đầu FE của module mình ngay khi API module đó merge và FE-01 (router) đã merge, miễn không làm trễ việc BE mà người khác đang chờ.
- **R-P1:** ND-01, FE-ND, AU-04, DB-01, FE-DB mặc định **CUT**; chỉ làm sau gate 11-30 khi được duyệt, không dùng 2 ngày đệm.
- **R-Migration:** owner viết nháp migration bảng mình; TV1 cấp số, review và merge theo thứ tự TK-01 (V4–V6) → GR-01 (V7–V9) → SC-01 (V10) trong ngày 11-19 (V2, V3 đã merge 11-18); không apply lên PostgreSQL local trước gate DB ready.

## Giả định

- Năng suất thực 6h/8h/ngày; 11-18 ~4h, 12-03 ~2h → 90h/người, 360h cả nhóm. Làm cả cuối tuần.
- Báo cáo (Word) nằm ngoài repo; không có slide.
- Môi trường teammate (JDK 25, Docker, Node 20+) chưa kiểm chứng — làm ở I-3.
