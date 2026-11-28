# Runbook: chuyển sang một LEADER mỗi nhóm (D16, V11)

Người chạy: TV1, trên từng DB đang dùng (dev của từng thành viên, DB demo). File này chỉ có placeholder `<...>`; **không ghi id hay dữ liệu thật vào repo**.

- V11 (`V11__enforce_single_active_leader_per_group.sql`) tạo partial unique index `uk_group_members_one_active_leader` (tối đa 1 LEADER ACTIVE mỗi nhóm). Guard trong V11 làm migration thất bại (và rollback sạch, không cần `flyway repair`) nếu có nhóm có thành viên ACTIVE mà số LEADER ACTIVE khác 1.
- "Ứng viên đủ điều kiện" = membership `ACTIVE` **và** `users.is_active = true` (O10a).

## Chuyển giao M0–M6

| Bước | Việc | Xong khi |
|---|---|---|
| M0 | **Trước khi merge PR backend:** chuẩn bị hai PR draft để rollback: (1) revert FE-GR-01b (khi 01b đã có); (2) revert backend **kèm** migration kế tiếp còn trống (ví dụ `V12__drop_single_active_leader_index.sql`), **giữ nguyên file V11**. Để CI chạy xong trên cả hai | Hai PR draft có CI pass |
| M1 | Dừng backend cũ trên DB đó, đóng các phiên psql có ghi dữ liệu. DB dùng chung: báo trong Software Group | Không còn tiến trình ghi |
| M2 | Chạy M2a, M2b (chỉ đọc) | Có danh sách nhóm sai |
| M3a | Sao lưu các dòng của nhóm sai ra CSV **ngoài repo** (có user id; không commit). Ghi lại người giữ quyền chọn cho từng nhóm | Có file |
| M3 | Chạy khối M3 cho từng nhóm sai (và nhóm M2b nếu muốn thay LEADER có tài khoản bị vô hiệu hóa) | Mỗi khối `COMMIT` không lỗi |
| M4 | Chạy lại M2a | 0 dòng |
| M5 | Khởi động backend mới; Flyway áp dụng V11 | App lên bình thường |
| M6 | `SELECT version, success FROM flyway_schema_history WHERE version = '11';` và `SELECT indexname FROM pg_indexes WHERE indexname = 'uk_group_members_one_active_leader';` | `success = true`; có index |

### M2a: nhóm có số LEADER ACTIVE khác 1
```sql
SELECT gm.group_id,
       COUNT(*) FILTER (WHERE gm.role = 'LEADER') AS leaders,
       string_agg(gm.user_id || ':' || gm.role || ':' || u.is_active || ':' || gm.joined_at,
                  ', ' ORDER BY gm.joined_at, gm.id) AS members
FROM group_members gm JOIN users u ON u.id = gm.user_id
WHERE gm.status = 'ACTIVE'
GROUP BY gm.group_id HAVING COUNT(*) FILTER (WHERE gm.role = 'LEADER') <> 1;
```

### M2b: LEADER có tài khoản bị vô hiệu hóa (O10c)
Nhóm này không ai quản lý được qua API. Sửa bằng M3 nếu muốn.
```sql
SELECT gm.group_id, gm.user_id FROM group_members gm JOIN users u ON u.id = gm.user_id
WHERE gm.status = 'ACTIVE' AND gm.role = 'LEADER' AND NOT u.is_active;
```

### M3a: sao lưu (psql, ghi ra ngoài repo)
```sql
\copy (SELECT id, group_id, user_id, role, status, joined_at, left_at FROM group_members WHERE group_id IN (<ids>) ORDER BY group_id, id) TO '<ngoài-repo>/group_members_pre_m3_<db>_<yyyymmdd>.csv' CSV HEADER
```

### M3: sửa một nhóm (điền `g` và `k`)
Chỉ đổi cột `role` của các dòng ACTIVE: hạ mọi LEADER khác `k`, rồi nâng `k`. `status`, `joined_at`, `left_at` không đổi. Chạy được cả trước và sau V11 (hạ trước, nâng sau).
```sql
BEGIN;
DO $$
DECLARE g bigint := <groupId>; k bigint := <keeperUserId>; n int;
BEGIN
  PERFORM 1 FROM team_groups WHERE id = g FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'group % not found', g; END IF;
  PERFORM 1 FROM group_members gm JOIN users u ON u.id = gm.user_id
   WHERE gm.group_id = g AND gm.user_id = k AND gm.status = 'ACTIVE' AND u.is_active;
  IF NOT FOUND THEN RAISE EXCEPTION 'user % is not an eligible ACTIVE member of group %', k, g; END IF;
  UPDATE group_members SET role = 'MEMBER'
   WHERE group_id = g AND status = 'ACTIVE' AND role = 'LEADER' AND user_id <> k;
  UPDATE group_members SET role = 'LEADER'
   WHERE group_id = g AND status = 'ACTIVE' AND user_id = k AND role <> 'LEADER';
  SELECT COUNT(*) INTO n FROM group_members WHERE group_id = g AND status = 'ACTIVE' AND role = 'LEADER';
  IF n <> 1 OR NOT EXISTS (SELECT 1 FROM group_members
       WHERE group_id = g AND user_id = k AND status = 'ACTIVE' AND role = 'LEADER') THEN
    RAISE EXCEPTION 'post-check failed for group %', g;
  END IF;
END $$;
COMMIT;
```

## Rollback R1–R7

Nguy cơ chính: backend cũ bỏ qua `expectedSuccessorUserId` và cho xóa LEADER khác. Phải dừng ghi và thu hồi các tab FE-01b **trước** khi chạy lại backend cũ.

| Bước | Việc | Sang bước sau khi |
|---|---|---|
| R1 | Báo trong Software Group. **Dừng backend mới** trên mọi DB cần rollback | Không còn tiến trình backend nào trên DB đó |
| R2 | Merge PR draft revert FE-GR-01b (M0; rebase nếu cần). Từng thành viên pull, khởi động lại `npm run dev`/bản build, **đóng mọi tab PlanWise rồi mở lại** | Từng người xác nhận không còn nút "Chuyển quyền", "Xóa", "Rời nhóm" |
| R3 | Sao lưu toàn bộ `group_members` ra ngoài repo (`\copy` như M3a, bỏ điều kiện `WHERE`) | Có file |
| R4 | Merge PR draft revert backend + V12 (M0; rebase nếu cần). Giữ nguyên file V11. V12: `DROP INDEX IF EXISTS uk_group_members_one_active_leader;` | CI pass |
| R5 | Khởi động backend cũ; Flyway áp dụng V12 | V12 `success = true`; `pg_indexes` không còn index |
| R6 | (Tùy chọn) Phục hồi vai trò từ CSV M3a: với mỗi dòng, `UPDATE group_members SET role = <csv.role> WHERE id = <csv.id> AND status = 'ACTIVE';`. Dòng đã đổi `status` sau M3 thì bỏ qua và ghi lại | TV1 đồng ý |
| R7 | **Mở lại thao tác ghi** chỉ khi R2 đã xác nhận cho mọi người dùng và R5 đã kiểm xong. Báo trong Software Group | — |

Muốn bật lại sau đó: thêm migration mới tạo lại index, rồi chạy lại M0–M6.
