# 03 — Đồ thị phụ thuộc

[← README](README.md) · WBS ID: [02_WBS.md](02_WBS.md)

```mermaid
graph LR
  F01[F-01/F-04 skeleton + ErrorCode] --> C1[TK-01 + nháp V3-5]
  F01 --> C2[SC-01 + nháp V8]
  F01 --> C3[GR-01 + nháp V6,7,10]
  C1 --> F02[F-02 V2,V9 + merge V2..V10]
  C2 --> F02
  C3 --> F02
  C1 --> F05[F-05 contract freeze]
  C2 --> F05
  C3 --> F05
  F02 --> AU[AU-01..03 Auth]
  F02 --> TK[TK-02..04 Tasks]
  F02 --> AV2[AV-02/04 Availability]
  F02 --> GR[GR-02/03 Group]
  SC2[SC-02 slots] --> SC3[SC-03 allocation] --> SC5a[SC-05a generate mock]
  SC2 --> SC4[SC-04 HC validator] --> SC6[SC-06 edit]
  SC5a --> SC6
  AV2 --> AV3[AV-03 fixed events]
  TK --> AS1[AS-01 assign]
  GR --> AS1 --> AS2[AS-02 respond]
  SC5a --> SC5b[SC-05b wiring]
  TK --> SC5b
  AV2 --> SC5b
  AV3 --> SC5b
  AS2 --> SC5b
  SC5b --> SC7[SC-07 confirm]
  SC6 --> SC7
  AV3 --> CAL[CAL-01 calendar]
  SC5a --> CAL
  CAL --> GR4[GR-04 group availability]
  GR --> GR4
  C3 --> AI1[AI-01 AiClient] --> AI2[AI-02 parse]
  F02 --> F03[F-03 Testcontainers] --> ASPG[AS-PG]
  AS1 --> ASPG
  F05 --> FE01[FE-00/01 scaffold + router] --> FE02[FE-02/03 api client + shared UI]
  FE02 --> FEAU[FE-AU]
  FE01 --> FEGR[FE-GR-01]
  FE02 --> FETK[FE-TK-01/02]
  FE02 --> FEAV[FE-AV]
  FE02 --> FECAL[FE-CAL]
  CAL --> FECAL
  SC5b --> FESC1[FE-SC-01/02]
  SC7 --> FESC3[FE-SC-03]
  AS2 --> FEAS[FE-AS-01/02]
  GR4 --> FEAS
  AI2 --> FEAI[FE-AI]
  FEAU --> GATE[IT-GATE 11-30]
  FEGR --> GATE
  FETK --> GATE
  FEAV --> GATE
  FECAL --> GATE
  FESC1 --> GATE
  FESC3 --> GATE
  FEAS --> GATE
  FEAI --> GATE
  GATE --> IT2[IT-02 E2E] --> IT3[IT-03 regression] --> RL[RL-01 release]
```

## Critical path

**SC-02 → SC-03 → SC-05a → SC-05b → SC-07 → FE-SC-03 → IT-GATE → IT-02 → RL-01**

- SC-05b chờ: TK-04 (11-21), AV-04 (11-23), AS-02 (11-23), **AV-03 (11-24 11:00)**.
- AV-03 là phụ thuộc sát critical path nhất: TV2 review AV-03 ở khung 11:00 ngày 11-24.
- TV2 không có slack trong 11-18 → 11-25 (xem R1 ở [07_RISKS_MVP_CUTS.md](07_RISKS_MVP_CUTS.md)).

## Việc song song được

- Engine SC-02..04 và SC-05a không cần DB/dữ liệu thật (dùng mock interface).
- Test module dùng `jwt()` của spring-security-test → CRUD không phải chờ Auth.
- AI-01 độc lập với mọi module khác.
- FE Groups từ 11-23, Tasks từ chiều 11-24 (R-FE).

## Điểm xung đột file chung

| File/khu vực | Quy tắc |
|---|---|
| `common/exception/ErrorCode.java` | TV1 thêm sẵn mọi mã ở F-04; module chỉ append đúng section nếu thật cần |
| `SecurityConfig`, `application.properties`, `build.gradle` | Chỉ TV1 sửa |
| `db/migration/` | Owner viết nháp, TV1 cấp số + merge đúng thứ tự |
| `docs/api/openapi-v1.json` | Chỉ TV1 cập nhật |
| FE `router`, `src/api`, `package.json` | TV1 giữ; người khác thêm route bằng PR nhỏ |

## Thời điểm bắt đầu sớm nhất an toàn

| Module | Sớm nhất | Lý do |
|---|---|---|
| CRUD Task / Availability / Group, Auth | 11-20 | Sau F-02 (DB ready) |
| Engine SC-02 | 11-18 | Java thuần |
| Assignment AS-01 | 11-22 | Sau GR-03 + TK-04 |
| SC-05a | 11-22 | Dùng mock interface |
| SC-05b | 11-24 | Sau AV-03 merge |
| Calendar CAL-01 | 11-24/25 | Sau AV-03 + SC-05a |
| FE module | 11-22 16:00 (sau FE-01); gọi API thật từ 11-23 16:00 (sau FE-02) | |
