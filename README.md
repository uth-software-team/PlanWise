# PlanWise AI

Ứng dụng web (responsive) giúp **quản lý công việc** và **tự động xếp lịch thông minh**: người dùng nhập task, khung giờ rảnh và các sự kiện cố định, hệ thống đề xuất lịch làm việc hợp lý; hỗ trợ làm việc nhóm, giao việc, thông báo và trợ lý AI.

> Đồ án môn Công nghệ Phần mềm — Nhóm 16 (4 thành viên). Hạn chót: **03/12/2026**.

---

## Mục lục
1. [Tính năng chính](#1-tính-năng-chính)
2. [Trạng thái hiện tại](#2-trạng-thái-hiện-tại)
3. [Công nghệ](#3-công-nghệ)
4. [Cấu trúc repo](#4-cấu-trúc-repo)
5. [Bắt đầu nhanh](#5-bắt-đầu-nhanh)
6. [Quy trình làm việc nhóm (Git Flow)](#6-quy-trình-làm-việc-nhóm-git-flow)
7. [Tài liệu liên quan](#7-tài-liệu-liên-quan)

---

## 1. Tính năng chính

| Module | Mô tả |
|---|---|
| Auth & Profile | Đăng ký, đăng nhập (JWT), quản lý hồ sơ, múi giờ |
| Personal Tasks | CRUD task cá nhân: độ ưu tiên, deadline, thời lượng ước tính, trạng thái |
| Availability & Fixed Events | Khai báo khung giờ rảnh và các sự kiện cố định (lịch học, họp...) |
| Smart Scheduling | Thuật toán xếp lịch tất định: ưu tiên cao + deadline gần trước, không chồng lấn, không quá deadline |
| Schedule Management | Xem / xác nhận / điều chỉnh các phiên làm việc được đề xuất |
| Team & Assignment | Tạo nhóm (LEADER/MEMBER), giao task, thành viên chấp nhận hoặc từ chối |
| AI Assistance | Gợi ý chia nhỏ task, ước lượng thời gian (AI chỉ hỗ trợ, không quyết định lịch) |
| Notifications & Dashboard | Nhắc deadline, thông báo giao việc, tổng quan tiến độ |

## 2. Trạng thái hiện tại

| Phần | Trạng thái |
|---|---|
| Backend — khung dự án (`common/`, `config/`, security, Swagger) | ✅ Đã có |
| Backend — các module nghiệp vụ (`modules/`) | 🚧 Đang phát triển |
| Frontend | ⏳ Chưa bắt đầu (làm sau khi API backend ổn định) |

Thứ tự phát triển: **Backend → test backend → chốt API contract → Frontend → tích hợp → system test.**

## 3. Công nghệ

**Backend** (`Code/backend/`)
- Java 25, Spring Boot 4, Gradle (wrapper đi kèm)
- Spring Web MVC, Spring Data JPA, Spring Security (JWT resource server), Bean Validation
- PostgreSQL (môi trường thật) / H2 in-memory (dev, mặc định — không cần cài DB)
- Swagger UI qua springdoc-openapi
- Kiến trúc: **Modular Monolith**, package theo nghiệp vụ, mỗi module phân tầng Controller → Service → Repository

**Frontend** (`Code/frontend/`, dự kiến)
- React + TypeScript + Vite, Tailwind CSS, shadcn/ui

## 4. Cấu trúc repo

```
PlanWise/
├── Code/
│   ├── backend/            # Spring Boot API  → xem Code/backend/README.md
│   └── frontend/           # React app (chưa khởi tạo)
├── scripts/
│   └── setup-dev.sh        # Script thiết lập môi trường dev
├── CLAUDE.md               # Quy tắc chung cho AI assistant & quy trình nhóm
└── README.md
```

## 5. Bắt đầu nhanh

### Yêu cầu
- **Java 25** (`brew install openjdk@25` trên macOS)
- Docker *(tùy chọn — chỉ khi muốn chạy PostgreSQL)*
- Node.js 18+ *(khi bắt đầu frontend)*

### Cách 1 — dùng script (khuyên dùng)
```bash
git clone https://github.com/uth-software-team/PlanWise.git
cd PlanWise
git checkout develop

./scripts/setup-dev.sh            # Kiểm tra công cụ, tạo .env từ .env.example, build thử backend (dùng H2)
./scripts/setup-dev.sh --docker   # Như trên + bật PostgreSQL bằng Docker (port 5432)
./scripts/setup-dev.sh -d -r      # Bật PostgreSQL và chạy backend luôn
./scripts/setup-dev.sh --help     # Xem tất cả tùy chọn
```

### Cách 2 — chạy tay
```bash
cd Code/backend
./gradlew bootRun     # Chạy server dev (mặc định dùng H2 in-memory)
./gradlew test        # Chạy test
./gradlew build       # Build + test
```

Sau khi chạy:
- API base: `http://localhost:8080/api`
- Swagger UI: **http://localhost:8080/api/swagger-ui.html**

### Biến môi trường
Copy `Code/backend/.env.example` thành `Code/backend/.env` rồi chỉnh (script ở Cách 1 tự làm việc này). Các biến chính: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `CORS_ALLOWED_ORIGINS`.

> ⚠️ **Không bao giờ commit file `.env`** hay secret thật. Chỉ commit `.env.example` với giá trị mẫu.

## 6. Quy trình làm việc nhóm (Git Flow)

| Nhánh | Tạo từ | Merge vào | Mục đích |
|---|---|---|---|
| `main` | — | — | Code đã release / demo được. Được bảo vệ, gắn tag `vX.Y.Z` |
| `develop` | `main` | — | Nhánh tích hợp. Được bảo vệ |
| `feature/<module>-<mô-tả>` | `develop` | `develop` | Tính năng mới, vd `feature/task-crud` |
| `fix/<mô-tả>` | `develop` | `develop` | Sửa lỗi |
| `test/<mô-tả>` | `develop` | `develop` | Chỉ thêm test |
| `docs/<mô-tả>` | `develop` | `develop` | Chỉ tài liệu |
| `release/<version>` | `develop` | `main` + `develop` | Chốt phiên bản |
| `hotfix/<mô-tả>` | `main` | `main` + `develop` | Sửa gấp trên bản đã release |

**Quy tắc**
- Không commit/push trực tiếp lên `main` hoặc `develop` — mọi thay đổi đi qua **Pull Request**.
- PR cần: build/test pass, ít nhất **1 thành viên review**, không conflict, mô tả rõ (làm gì, vì sao, test thế nào, liên quan BR/use case nào).
- Commit theo **Conventional Commits** (tiếng Anh): `type(scope): summary`
  — `type`: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`
  — ví dụ: `feat(schedule): add conflict check on confirm`
- Đồng bộ với `develop` mới nhất trước khi mở PR. Xóa nhánh sau khi merge.

**Luồng cơ bản**
```bash
git checkout develop && git pull
git checkout -b feature/task-crud
# ... code, commit ...
git push -u origin feature/task-crud
# Mở Pull Request vào develop trên GitHub
```

## 7. Tài liệu liên quan
- [Code/backend/README.md](Code/backend/README.md) — kiến trúc backend, luồng request, xử lý lỗi, quy chuẩn thêm module
- [CLAUDE.md](CLAUDE.md) — quy tắc chung của dự án
- Tài liệu thiết kế (use case, class diagram, ERD, business rules BR-01..BR-32): lưu nội bộ của nhóm, không nằm trong repo
