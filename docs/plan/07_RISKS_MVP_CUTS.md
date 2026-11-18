# 07 — Risk Register & MVP Cuts

[← README](README.md)

## Risk register

| ID | Rủi ro | Xác suất | Ảnh hưởng | Giảm thiểu | Owner | Hạn quyết định |
|---|---|---|---|---|---|---|
| R1 | Scheduling trễ (TV2 không có slack) | Cao | Cao | Tách SC-05a/b; engine test trước; nếu SC-05a trễ > 4h → TV1 nhận SC-06; vẫn trễ → C-3 | TV2 / TV1 | 11-23 |
| R2 | Frontend bắt đầu từ 0 | Cao | Cao | Scaffold sớm; FE xen kẽ từ 11-23; types sinh từ OpenAPI; không lib lịch; shadcn/ui | TV1 | 11-22 |
| R3 | Migration merge sai thứ tự / đổi DB sau freeze | TB | Cao | R-Migration: merge đúng thứ tự trong 11-19, không apply sớm; không sửa migration đã apply | TV1 | 11-19 |
| R4 | Gemini hết quota / timeout / JSON sai | Cao | TB | `FakeAiClient` mặc định; 503 → nhập tay; demo không phụ thuộc Gemini | TV4 | 11-23 |
| R5 | Bảo mật: IDOR, lộ lịch riêng | TB | Cao | Mọi query lọc theo `currentUserId`; privacy test GR-04; review checklist | TV1 | 11-26 |
| R6 | Review nghẽn | TB | Cao | SLA ≤ 4h, khung 11:00/16:00, ma trận reviewer chia đều | TV1 | 11-18 |
| R7 | Teammate chưa vào repo / lỗi môi trường (Windows) | TB | Cao | Mời + setup ngay 11-18; Git Bash/WSL cho script | TV1 | 11-18 |
| R8 | Frontend ít test | Cao | TB | Dry-run 11-29 + E2E 12-01 theo checklist | TV1 | 11-29 |
| R9 | Báo cáo dồn cuối | TB | TB | Chia phần từ 11-27; ghép xong 12-02 | TV1 | 11-27 |
| R10 | Ốm / vắng | TB | Cao | Reviewer chính của mỗi module nắm code để thay | cả nhóm | — |
| R11 | Shared API/contract thay đổi sau freeze | TB | Cao | PR contract-change, TV1 + consumer duyệt | TV1 | 11-20 |

## Thứ tự cắt MVP (mỗi bước cần user duyệt)

| Bước | Cắt | Ghi chú |
|---|---|---|
| C-0 | P1: ND-01, FE-ND, AU-04, DB-01, FE-DB | **Mặc định đã CUT** (R-P1); chỉ mở lại sau gate 11-30 khi được duyệt, không dùng 12-01/02 |
| C-1 | Polish (animation, responsive nâng cao) | |
| C-3 | Edit session chỉ còn xoá + generate lại | Khi R1 xảy ra |
| C-4 | AI chỉ dùng `FakeAiClient` | Khi Gemini không ổn định |
| C-5 | UI không thiết yếu (profile nâng cao, filter phụ) | |

Ngoài kế hoạch (không làm trong MVP): auto-rescheduling, AI overload advice, chia task nhiều session, `rest_blocks`, refresh token, invite `/join`.

**Không cắt 6 luồng demo** (xem [06_TEST_DOD.md](06_TEST_DOD.md)).
