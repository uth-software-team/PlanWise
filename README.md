# PlanWise Application

Hệ thống quản lý công việc và dự án thông minh PlanWise.

## Cấu trúc Dự án
- `Code/backend/`: Backend Spring Boot xây dựng theo kiến trúc **Modular Monolith**, phân chia thư mục **Package by Feature** và bên trong là **Layered Architecture**.
  - 👉 Xem hướng dẫn chi tiết về luồng hoạt động, cấu trúc và cách chạy tại [Code/backend/README.md]
- `Code/frontend/`: Ứng dụng Frontend giao diện người dùng.

## Bắt đầu Nhanh với Backend
```bash
cd Code/backend
./gradlew bootRun
```
Swagger UI sẵn sàng tại: [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)