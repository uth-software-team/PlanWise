# Checklist E2E — 6 luồng demo (IT-01)

Dùng cho **IT-DRY** (11-29), **IT-GATE** (11-30) và **IT-02** (12-01). Mỗi luồng có điều kiện trước, các bước qua UI, kết quả mong đợi, ca âm và chỗ ghi kết quả.
Nguồn: [06_TEST_DOD.md](../plan/06_TEST_DOD.md) (6 luồng, owner), `Code/backend/CLAUDE.md` (business rules), [openapi-v1.json](../api/openapi-v1.json) (endpoint, mã lỗi). Đường dẫn API dưới đây bỏ tiền tố `/api` (proxy dev: `http://localhost:5173/api/v1/...`).

## 0. Tình trạng sẵn sàng

Kiểm trên `develop` = `1a91375` ngày 2026-11-28. **Cập nhật bảng này trước mỗi lần chạy.**

| Luồng | Backend trên `develop` | Frontend trên `develop` | Đang ở PR / nhánh | Chạy được qua UI? |
|---|---|---|---|---|
| 1 Auth + profile | Có (`/auth/*`, `/users/me`) | Chưa (login/register/settings ở PR #52) | #52 `feature/fe-auth` (TV1) | Sau khi #52 merge |
| 2 Tasks | **501** (TaskController stub) | Chưa (FE-TK-01/02, TV3) | #35 `feature/task-crud` (TV3, CHANGES_REQUESTED) | Không |
| 3 Lịch | Schedule có; availability-rules, fixed-events, calendar **501** | Có trang `/schedule`; chưa có FE-AV (TV3), FE-CAL (TV2) | #36, #39 (TV3, CHANGES_REQUESTED) | Không |
| 4 Nhóm + giao việc | Group + members có; assignments **501** (AS-01/02, TV4) | Chưa (FE-GR-01, FE-AS-01/02, TV4) | Không có nhánh trên origin | Không |
| 5 AI | **501** (AI-01/02, TV4) | Chưa (FE-AI, TV4) | Không có nhánh trên origin | Không |
| 6 BUSY/FREE | `GET /v1/groups/{id}/availability` có, nhưng trả 501 khi chưa có calendar provider (CAL-01) | Chưa (FE-AS-02 lưới BUSY/FREE, TV4) | Phụ thuộc #39 | Không |

Gate 11-30 yêu cầu chạy **qua UI**. Kiểm bằng API (Swagger, curl) chỉ để tìm lỗi backend sớm, không tính là PASS cho gate.

## 1. Chuẩn bị chung

### Môi trường
- PostgreSQL (không dùng H2 cho E2E): `./scripts/setup-dev.sh --docker`, rồi `cd Code/backend && ./gradlew bootRun`. Log phải có `HikariPool-1 - Added connection org.postgresql...` (thấy `jdbc:h2` là sai môi trường, dừng lại).
- Frontend: `cd Code/frontend && npm ci && npm run dev`, mở `http://localhost:5173`.
- Ghi lại SHA của `develop` đang chạy và trình duyệt (tên + phiên bản) vào bảng kết quả.

### Tài khoản và dữ liệu
Mỗi lần chạy dùng email mới để không phụ thuộc dữ liệu cũ. Thay `<run>` bằng số lần chạy (ví dụ `dry1`, `gate`, `e2e`).

| Vai | Email | Họ tên | Ghi chú |
|---|---|---|---|
| A, leader | `a-<run>@example.test` | Lan Anh | Tạo ở luồng 1 |
| B, member | `b-<run>@example.test` | Minh Bảo | Có dữ liệu riêng tư ở luồng 6 |
| C, ngoài nhóm | `c-<run>@example.test` | Chí Công | Chỉ dùng cho ca bị chặn |

Mật khẩu chung: `PlanWise-<run>-2026` (≥ 8 ký tự). Múi giờ mọi tài khoản: `Asia/Ho_Chi_Minh`.
Ngày gốc **D** = thứ Hai của tuần kế tiếp ngày chạy. Các mốc trong checklist tính theo D (giờ địa phương).

### Ghi kết quả
- Cột **KQ**: `PASS`, `FAIL` hoặc `BLOCKED` (không chạy được vì phụ thuộc chưa xong). Ô trống = chưa chạy.
- `FAIL` → ghi một dòng trong [mục 8](#8-bug-log) và điền ID bug vào cột Ghi chú.
- Ảnh chụp: `IT-<run>-F<luồng>-S<bước>.png`, lưu ở thư mục chung của nhóm (không commit vào repo). Không chụp token hay nội dung `localStorage`.

## 2. Luồng 1 — Đăng ký → đăng nhập → sửa profile

Owner: TV1. Liên quan: UC đăng ký/đăng nhập/hồ sơ; JWT 24h; mật khẩu ≥ 8 ký tự, ≤ 72 byte.
Điều kiện trước: backend + frontend chạy; chưa đăng nhập.

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 1.1 | Mở `/register`, nhập họ tên, email A, mật khẩu chung, bấm **Tạo tài khoản** | Vào `/tasks`; menu tài khoản hiện "Lan Anh" và email A (`POST /v1/auth/register` 201, rồi `POST /v1/auth/login` 200) | | |
| 1.2 | Đăng xuất (menu tài khoản → **Đăng xuất**) | Về `/login` | | |
| 1.3 | Đăng ký lại bằng email A | Báo "Email này đã được dùng cho một tài khoản khác." (409 `EMAIL_ALREADY_EXISTS`), vẫn ở `/register` | | |
| 1.4 | Đăng ký email mới với mật khẩu 7 ký tự | Lỗi dưới ô mật khẩu, không gửi request | | |
| 1.5 | Đăng nhập email A, mật khẩu sai | "Email hoặc mật khẩu không đúng." (401 `INVALID_CREDENTIALS`) | | |
| 1.6 | Đăng nhập email A, mật khẩu đúng | Vào `/tasks` | | |
| 1.7 | Mở **Cài đặt**: đổi họ tên thành "Lan Anh Nguyễn", múi giờ `Asia/Bangkok`, khung giờ 07:30–21:00, bấm **Lưu thay đổi** | "Đã lưu thay đổi."; menu tài khoản đổi tên ngay (`PUT /v1/users/me` 200) | | |
| 1.8 | Tải lại trang `/settings` | Họ tên, múi giờ, khung giờ đúng giá trị vừa lưu (`GET /v1/users/me`) | | |
| 1.9 | Đặt khung giờ 18:00–18:00, bấm Lưu | Lỗi "Giờ kết thúc phải sau giờ bắt đầu.", dữ liệu không đổi sau khi tải lại | | |
| 1.10 | Đặt lại múi giờ `Asia/Ho_Chi_Minh`, khung giờ 07:30–21:00, Lưu | Lưu thành công (dùng cho luồng 3) | | |
| 1.11 | Đăng xuất. Mở thẳng `http://localhost:5173/settings` | Chuyển tới `/login?redirect=%2Fsettings`; đăng nhập xong quay lại `/settings` | | |
| 1.12 | Đăng xuất. Mở `/login?redirect=//evil.com`, đăng nhập | Vào `/tasks`, không rời `localhost:5173` (SA-13) | | |
| 1.13 | Mở 2 tab đã đăng nhập; đăng xuất ở tab 1 | Tab 2 cũng về `/login` | | |
| 1.14 | Tạo thêm tài khoản B và C qua `/register` | Thành công; dùng ở luồng 4 và 6 | | |

## 3. Luồng 2 — Tạo / sửa / đổi status / xoá task; badge quá hạn

Owner: TV3. Liên quan: priority LOW/MEDIUM/HIGH/URGENT; status TODO/IN_PROGRESS/COMPLETED/CANCELLED; `overdue` tính từ deadline + status, không lưu; xoá mềm.
Điều kiện trước: đăng nhập A; #35 (TK-02..04) và FE-TK-01/02 đã merge.

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 2.1 | Tạo task **T1** "Ôn Mạng máy tính": HIGH, STUDY, 120 phút, deadline D+3 17:00 | Có trong danh sách, status TODO (`POST /v1/tasks` 201) | | |
| 2.2 | Tạo **T2** "Báo cáo đồ án": URGENT, PROJECT, 180 phút, deadline D+2 12:00 | Có trong danh sách | | |
| 2.3 | Tạo **T3** "Đọc tài liệu": LOW, OTHER, 60 phút, deadline D+6 | Có trong danh sách | | |
| 2.4 | Tạo task với tiêu đề trống, hoặc thời lượng 0 | Lỗi dưới đúng ô, không tạo (400 `INVALID_INPUT`) | | |
| 2.5 | Sửa T3: đổi tiêu đề, mô tả | Danh sách và chi tiết hiện giá trị mới (`PUT /v1/tasks/{id}`) | | |
| 2.6 | Đổi status T3: TODO → IN_PROGRESS → COMPLETED | Status đổi đúng (`PATCH /v1/tasks/{id}/status`) | | |
| 2.7 | Tạo **T4** có deadline đã qua (nếu form không cho chọn ngày quá khứ: deadline = 5 phút sau, chờ qua mốc rồi tải lại) | T4 có badge **Quá hạn**; status vẫn TODO (quá hạn không phải status) | | Hỏi TV3 form có chặn deadline quá khứ không |
| 2.8 | Đổi T4 sang COMPLETED | Badge quá hạn biến mất | | |
| 2.9 | Lọc theo status TODO, rồi theo priority URGENT | Chỉ hiện đúng task (`GET /v1/tasks?status=&priority=`) | | |
| 2.10 | Xoá T4, xác nhận | Biến mất khỏi danh sách (`DELETE /v1/tasks/{id}` 204) | | |
| 2.11 | Mở cùng task ở 2 tab; sửa ở tab 1 rồi lưu; sửa ở tab 2 rồi lưu | Tab 2 báo dữ liệu đã thay đổi (409 `STALE_VERSION`), không ghi đè âm thầm | | |
| 2.12 | Đăng nhập B, gọi chi tiết task của A (sửa id trên URL nếu có trang chi tiết) | Không thấy (404), không lộ tiêu đề | | |
| 2.13 | Các danh sách khi trống / đang tải / lỗi mạng (tắt backend) | Có trạng thái Empty, Loading, Error rõ ràng | | |

## 4. Luồng 3 — Availability + fixed events → generate → chỉnh session → confirm

Owner: TV2 + TV3. Liên quan: chỉ xếp trong availability ∩ khung giờ lập kế hoạch; không chồng fixed event hay session CONFIRMED; không quá deadline; thứ tự priority giảm, deadline tăng; một session liên tục cho mỗi task (BR-08); khoảng nửa mở `[start, end)`; thiếu thời gian → báo conflict, không xếp sai; confirm kiểm lại mọi HC trong một transaction (BR-10, BR-11).
Điều kiện trước: đăng nhập A; luồng 1 bước 1.10 và luồng 2 bước 2.1–2.2 xong (T1, T2 TODO); #36, #39, FE-AV, FE-CAL, FE-SC-01..03 đã merge.

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 3.1 | Thêm availability rule thứ Hai → thứ Sáu, 08:00–17:00 | 5 rule hiện đúng (`POST /v1/availability-rules`) | | |
| 3.2 | Thêm rule có giờ kết thúc trước giờ bắt đầu | Bị từ chối, báo lỗi | | |
| 3.3 | Thêm fixed event WEEKLY "Họp nhóm" thứ Tư 09:00–11:00 | Hiện trên lịch tuần mọi thứ Tư (`POST /v1/fixed-events`, `GET /v1/calendar`) | | |
| 3.4 | Thêm fixed event ONCE "Khám răng" D+1 13:00–14:00 | Chỉ hiện ngày D+1 | | |
| 3.5 | Tạo **T5** "Việc gấp quá sức": MEDIUM, 600 phút, deadline D 12:00 | Tạo được | | |
| 3.6 | Mở **Lịch trình**, generate cho tuần [D, D+7) | Các session hiện là **đề xuất** (PROPOSED), chưa lưu chính thức (`POST /v1/schedules/generate`) | | |
| 3.7 | Kiểm từng session đề xuất | Nằm trong 08:00–17:00 thứ Hai–Sáu và trong 07:30–21:00; không chồng "Họp nhóm" hay "Khám răng"; kết thúc trước deadline; T2 (URGENT) được xếp trước T1 (HIGH); mỗi task tối đa 1 session liền | | |
| 3.8 | Xem phần task không xếp được | T5 nằm trong danh sách không xếp được, có lý do (không đủ thời gian trước deadline) | | |
| 3.9 | Kéo/sửa một session sang thứ Tư 09:30–11:30 | Báo conflict chồng fixed event (`FIXED_EVENT_OVERLAP`), session không đổi | | |
| 3.10 | Sửa session sang giờ hợp lệ khác | Lưu được, vẫn PROPOSED (`PUT /v1/schedules/sessions/{id}`) | | |
| 3.11 | Mở trang ở tab 2, sửa một session; quay lại tab 1 bấm **Xác nhận** | Tab 1 báo dữ liệu đã đổi (409 `STALE_VERSION`) và tải lại đề xuất | | |
| 3.12 | Bấm **Xác nhận** ở bản mới nhất | Session thành CONFIRMED, hiện trên lịch tuần (`POST /v1/schedules/{proposalId}/confirm`) | | |
| 3.13 | Generate lại cho cùng tuần | Session CONFIRMED giữ nguyên giờ; đề xuất mới không chồng lên chúng | | |
| 3.14 | Đổi T3 sang CANCELLED (hoặc dùng task COMPLETED), generate | Task COMPLETED/CANCELLED không được xếp | | |
| 3.15 | Generate với khoảng > 366 ngày (nếu UI cho chọn) | Bị từ chối, 400 `INVALID_TIME_RANGE` | | |

## 5. Luồng 4 — Nhóm → leader giao task → member accept / decline

Owner: TV4 + TV2. Liên quan: người tạo nhóm = LEADER; MEMBER chỉ ACCEPT/DECLINE; decline bắt buộc lý do; assignment PENDING/ACCEPTED/DECLINED/REVOKED tách khỏi status task; chỉ ACCEPTED mới vào việc của member; một bản ghi task, không nhân bản.
Điều kiện trước: A, B, C đã có (luồng 1); AS-01/02, FE-GR-01, FE-AS-01/02 đã merge.

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 4.1 | A tạo nhóm "Đồ án PlanWise" | A là LEADER (`POST /v1/groups` 201) | | |
| 4.2 | A thêm thành viên bằng email B | B có trong danh sách, vai MEMBER (`POST /v1/groups/{id}/members`) | | |
| 4.3 | A thêm email chưa đăng ký | Báo lỗi, không thêm | | |
| 4.4 | A giao task mới "Thiết kế ERD" (HIGH, 90 phút, deadline D+4) cho B | Assignment PENDING (`POST /v1/assignments` 201) | | |
| 4.5 | B mở hộp thư giao việc | Thấy "Thiết kế ERD" ở trạng thái chờ (`GET /v1/assignments`) | | |
| 4.6 | B bấm **Từ chối** nhưng để trống lý do | Không gửi được, báo cần lý do | | |
| 4.7 | B từ chối với lý do "Trùng lịch thi" | DECLINED; A thấy lý do (`POST /v1/assignments/{id}/decline`) | | |
| 4.8 | A giao task mới "Viết API spec" cho B; B **Chấp nhận** | ACCEPTED (`POST /v1/assignments/{id}/accept`) | | |
| 4.9 | B mở danh sách task, tab "Được giao" | Có "Viết API spec"; không có "Thiết kế ERD" (đã từ chối) | | |
| 4.10 | B generate lịch tuần [D, D+7) rồi xác nhận | "Viết API spec" được xếp và vào lịch của B | | |
| 4.11 | A giao task thứ ba cho B, rồi **Thu hồi** khi còn PENDING | REVOKED; B không còn thấy để phản hồi (`POST /v1/assignments/{id}/revoke`) | | |
| 4.12 | B đổi status "Viết API spec" sang IN_PROGRESS | Status task đổi; assignment vẫn ACCEPTED | | |
| 4.13 | C mở nhóm qua URL | Bị chặn (403/404), không thấy thành viên hay task | | |
| 4.14 | B (MEMBER) thử giao task trong nhóm | Không có nút, hoặc bị chặn 403 | | |

## 6. Luồng 5 — AI parse → sửa draft → lưu; tắt Gemini vẫn nhập tay được

Owner: TV4. Liên quan: AI chỉ gợi ý; backend kiểm JSON, kiểu, deadline, priority, thời lượng; người dùng xác nhận mới lưu; AI không ghi DB; timeout 5 s / JSON sai / hết quota → 503 `AI_UNAVAILABLE`, người dùng nhập tay tiếp.
Điều kiện trước: đăng nhập A; AI-01/02, FE-AI đã merge; TV4 cung cấp cách bật/tắt Gemini trên máy chạy (không đưa API key vào repo hay vào checklist).

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 5.1 | Nhập "Ôn thi Cơ sở dữ liệu 3 tiếng trước thứ Sáu tuần sau, ưu tiên cao", bấm phân tích | Hiện bản nháp có tiêu đề, deadline, priority, thời lượng để sửa (`POST /v1/ai/tasks/parse` 200) | | |
| 5.2 | Mở danh sách task ở tab khác (chưa lưu nháp) | Chưa có task mới: AI không tự lưu | | |
| 5.3 | Sửa thời lượng nháp thành 150 phút, bấm Lưu | Task được tạo với 150 phút (`POST /v1/tasks` 201) | | |
| 5.4 | Gửi nội dung trống | Báo lỗi nhập, không gọi AI | | |
| 5.5 | TV4 tắt Gemini; nhập lại câu ở 5.1 | Báo AI tạm không dùng được (503 `AI_UNAVAILABLE`); form nhập tay vẫn mở | | |
| 5.6 | Tạo task bằng form nhập tay khi Gemini đang tắt | Tạo được bình thường | | |
| 5.7 | Bật lại Gemini | Phân tích hoạt động lại | | |

## 7. Luồng 6 — Leader xem BUSY/FREE, không lộ title/note

Owner: TV1 + TV4. Liên quan: leader chỉ thấy khoảng BUSY/FREE; không lộ tiêu đề task cá nhân, nội dung fixed event, ghi chú hay chi tiết session.
Điều kiện trước: luồng 4 bước 4.1–4.2 xong; CAL-01 (#39) và FE-AS-02 đã merge.

| # | Thao tác (UI) | Kết quả mong đợi | KQ | Ghi chú |
|---|---|---|---|---|
| 7.1 | B tạo fixed event ONCE "Hẹn bác sĩ riêng" D+2 15:00–16:00, ghi chú "bí mật" | Tạo được | | |
| 7.2 | B tạo task "Việc riêng của Bảo" có mô tả, generate và xác nhận để có session CONFIRMED trong tuần D | Có session trên lịch B | | |
| 7.3 | A mở lưới BUSY/FREE của nhóm cho tuần [D, D+7) | Thấy các khoảng BUSY của B, gồm D+2 15:00–16:00 và khung của session (`GET /v1/groups/{id}/availability`) | | |
| 7.4 | Trên lưới, rê chuột/mở chi tiết khoảng BUSY | Không hiện "Hẹn bác sĩ riêng", "bí mật", "Việc riêng của Bảo" hay mô tả | | |
| 7.5 | DevTools → Network, xem response của 7.3 | Mỗi member chỉ có `userId` và các khoảng `startAt`/`endAt`; không có title, note, description, taskId | | |
| 7.6 | C (ngoài nhóm) gọi availability của nhóm (sửa URL) | Bị chặn 403 | | |
| 7.7 | A chọn khoảng > 366 ngày (nếu UI cho chọn) | Bị từ chối, 400 `INVALID_TIME_RANGE` | | |

## 8. Bug log

| ID | Luồng/bước | Mô tả (thực tế vs mong đợi) | Mức (Blocker/Major/Minor) | Owner (theo module, D3) | Hạn | Trạng thái |
|---|---|---|---|---|---|---|
| | | | | | | |

Owner sửa theo module của lỗi; TV1 điều phối lỗi liên module và chốt E2E (D3).

## 9. Tiêu chí gate (IT-GATE, 11-30)

- Một luồng **PASS** khi mọi bước của luồng đó PASS qua UI trên PostgreSQL, trong cùng một lần chạy.
- Bước `BLOCKED` tính là chưa đạt. Không thay bước UI bằng API để tính PASS.
- Mọi `FAIL` phải có dòng trong bug log, có owner và hạn.
- Gate đạt khi cả 6 luồng PASS. Không đạt → TV1 quyết định theo [07_RISKS_MVP_CUTS.md](../plan/07_RISKS_MVP_CUTS.md) (không cắt 6 luồng demo); mở P1 chỉ khi gate đạt và được duyệt.

### Kết quả các lần chạy

| Lần chạy | Ngày | `develop` SHA | Trình duyệt | L1 | L2 | L3 | L4 | L5 | L6 | Người chạy |
|---|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | | |
