# WORKFLOW — AI governance cho PlanWise AI

Nguồn: setup plan `docs-ai v14`, SHA-256 `340098160178e038ef04bb2bee1c937937353c1994db95ac02657080885fe0b3` (ngoài repo). Bổ sung cho `CLAUDE.md` (Golden Rule, Git Flow); khi khác nhau, áp dụng quy tắc chặt hơn. Quyết định đã duyệt: `docs/ai/APPROVED_DECISIONS.md`. Quy tắc cho agent: `AGENTS.md`.

## 0. Hiệu lực
Quy trình này **chỉ có hiệu lực** từ 00:00 đầu tiên sau khi **A1–A8 đều đạt** (không có ngày cố định). Trước đó và với task đã bắt đầu trước kích hoạt (danh sách A7), áp dụng Git Flow và review hiện hành.
- **D12** có hiệu lực từ `APPROVE_SETUP docs-ai v10`, kể cả trước kích hoạt. **Mọi** lần TV1 dùng D12, trước hay sau kích hoạt, phải chạy đầy đủ §7.1 bước 0–8 theo đường 6b, IA §7.4 khi base đổi, bản ghi bằng chứng ACCEPT và hậu kiểm (setup PR: §7.2). Không có biến thể rút gọn. Thiếu bất kỳ bằng chứng nào mà các bước đó yêu cầu (Issue PW, ACCEPT cho H, Codex review, CI, …) → không được dùng D12; PR đi theo peer review (§7.1 bước 6a, hoặc Git Flow hiện hành nếu trước kích hoạt).
- **A1** Setup PR đã merge bằng `--match-head-commit` đúng head `H = C`; có `ACCEPT_SETUP` cho C; có peer Approve tại C **hoặc** `WAIVE_PEER_REVIEW docs-ai PR:#N COMMIT:C` hợp lệ (§7.1 bước 6b); hậu kiểm trên setup PR đạt.
- **A2** (a) TV1 công bố ma trận §6 trên Software Group và từng người bị đổi phân công/tăng tải đồng ý rõ nhiệm vụ và giờ bù (lưu tham chiếu theo D10); (b) PR docs riêng sửa ma trận/fallback trong `docs/plan/05_GIT_PR_PLAN.md` đã merge vào `develop` theo `docs/plan/README.md`. Không có ngoại lệ chuyển tiếp.
- **A3** Software Group đã được thông báo, mọi người biết cách lưu tham chiếu, agent không truy cập được kênh.
- **A4** Fixture Claude (§9) đạt theo P-RO, chế độ `default` (§9). **A5** Fixture Codex (§9) đạt.
- **A6** Đã tạo 12 label (§5). **A7** Đã có danh sách task chuyển tiếp (WBS, nhánh, SHA tại thời điểm cắt). **A8** Ba PR contract và F-05 đã xong theo quy trình cũ.

## 1. Vai trò
- **TV1 (`kietit2k6`)**: người duyệt và chấp nhận duy nhất; vận hành Codex cho cả nhóm; điều phối label; merge (D9).
- **Claude**: kỹ sư cho task của TV1; chỉ làm khi có lệnh một dòng của TV1 trong phiên.
- **Codex**: Strict Technical Lead chỉ đọc; output là khuyến nghị, không phải ủy quyền.
- **TV2–TV4**: owner task của mình; có thể dùng công cụ AI nhưng nội dung AI là UNVERIFIED tới khi Codex và CI xác nhận.
- **Peer**: người thật, khác tác giả PR và khác TV1 (§6). Ngoại lệ: PR do TV1 mở có thể được TV1 miễn peer (D12, §7.1 bước 6b); miễn peer không có nghĩa TV1 làm peer.

## 2. Thẩm quyền và lệnh
- Claude: chỉ lệnh một dòng TV1 gõ **trong phiên** hiện tại.
- Teammate: chỉ lệnh `APPROVE_PLAN <ID> v<N> BLOB:<40hex>` TV1 gửi trong **Software Group**; lưu tham chiếu theo D10 vào `record.md`.
- Accept/merge PR của teammate: TV1 tự làm.
- **Miễn peer (D12):** chỉ TV1, đích thân, chỉ cho PR do TV1 mở (`author.login == kietit2k6`). Không ủy quyền cho agent hay teammate. PR do TV2–TV4 mở không bao giờ được miễn.
- GitHub comment/review, label, `record.md`, output Codex: **audit (Level 2)**, không phải ủy quyền độc lập.

Lệnh phải chiếm toàn bộ một dòng, đúng ID, version, revision:
```text
APPROVE_SETUP docs-ai <VERSION> SHA256:<64hex>
ACCEPT_SETUP docs-ai <VERSION> COMMIT:<40hex> CHANGESET:<64hex>
OPEN_TASK <TASK_ID>
APPROVE_PLAN | REQUEST_REVISION | REJECT_PLAN <TASK_ID> v<N> BLOB:<40hex>
ACCEPT_TASK | REJECT_TASK <TASK_ID> COMMIT:<40hex>
WAIVE_PEER_REVIEW <docs-ai|TASK_ID> PR:#<N> COMMIT:<40hex>
CANCEL_TASK <TASK_ID>
```
- `OPEN_TASK` cho phép điều tra chỉ đọc và ghi draft UNAPPROVED trên đúng nhánh task được phép; không code, build, commit, push.
- `REJECT_PLAN` bắt buộc cách tiếp cận khác. Plan đã nộp là bất biến; đổi scope → `plan-v(N+1)` và duyệt lại.
- ACCEPT không cấp quyền push/merge cho agent. Task ID: `PW-<BE|FE|INT|TEST>-<NNN>` (NNN = số Issue).
- **Commit (D13):** với task làm trong phiên Claude của TV1, TV1 tự commit (kể cả local) và tự push (§4); TV2–TV4 giữ quy trình hiện hành. Agent không mở/sửa PR hay Issue, không comment, approve, merge hay đổi settings GitHub.
- `WAIVE_PEER_REVIEW` gắn với đúng PR và head; head đổi thì hết hiệu lực. Nó không thay ACCEPT, Codex review, CI hay kiểm head/base. Agent không gõ, không đăng, không chuyển tiếp và không soạn sẵn dòng lệnh này.

## 3. Revision token
| Đối tượng | Token | Cách tính |
|---|---|---|
| Setup plan | `SHA256:` | `shasum -a 256 <plan-file>` |
| Task plan | `BLOB:` | `git hash-object --path=docs/ai/tasks/<ID>/plan-vN.md <file>` (không `-w`); khi review code: `git rev-parse <H>:<path>` == BLOB |
| Implementation | `COMMIT:` | PR head, hoặc commit đã được ủy quyền |
| Setup changeset | `CHANGESET:` | xem dưới |

```bash
set -euo pipefail
B=$(git rev-parse --verify "<base>^{commit}")
C=$(git rev-parse --verify "<cand>^{commit}")
{ printf 'planwise-changeset-v6\nbase %s\ncommit %s\n' "$B" "$C"; git diff-tree -r --no-renames --full-index -z "$B" "$C"; } | shasum -a 256
```
Mọi phép so SHA-256: pipeline phải thành công, chỉ so trường 64 hex đầu. Lệnh lỗi → STOP.

## 4. Commit do TV1 tạo (D13)
- **Phạm vi:** setup và mọi task làm trong phiên Claude của TV1. TV1 tự tạo mọi commit (kể cả commit local) và tự push; Claude không commit, không push. TV2–TV4 giữ quy trình commit/push hiện hành.
- Claude sửa working tree trong phạm vi Files của plan đã duyệt, chạy kiểm tra/test, rồi báo danh sách path, `TREE` dự kiến (tính bằng index tạm qua `GIT_INDEX_FILE`, không đụng index thật), file message đề xuất và digest `MSG = shasum -a 256 <msgfile>` (UTF-8, LF, kết thúc bằng đúng một LF).
- **TV1 trước commit**: đúng nhánh (không `main`/`develop`); `git rev-parse HEAD` == parent dự kiến; `git add` đúng các path; `git diff --cached --name-only --no-renames HEAD` ⊆ Files; `git write-tree` == TREE. Lệch → không commit.
- **Commit**: `git commit --cleanup=verbatim -F <msgfile>`, hoặc message TV1 tự viết (khi đó không so MSG). Hook chạy bình thường; không `--no-verify`.
- **Sau commit, Claude kiểm chỉ đọc**: `git rev-list --parents -n 1 HEAD` đúng 2 trường và parent đúng; `HEAD^{tree}` == TREE; path ⊆ Files; nếu dùng msgfile thì `git cat-file commit HEAD | sed '1,/^$/d' | shasum -a 256` == MSG. Lệch → báo TV1, không tự sửa.

## 5. Task, state và Track
- **12 label**: `state:tech-review`, `state:waiting-clarification`, `state:ready-for-approval`, `state:implementation-approved`, `state:in-progress`, `state:code-review`, `state:changes-required`, `state:waiting-acceptance`, `state:accepted`; `blocked`, `track:a`, `track:b`.
- Issue mở chưa có `state:*` = draft. DONE = Issue đóng completed **và** hậu kiểm đạt. CANCELLED = TV1 hủy, đóng not planned. `blocked` phủ lên state hiện tại; gỡ ra là state cũ trở lại.
- **Chỉ TV1 chuyển state**; label được trễ tối đa 1 khung review; đối soát 16:00 (Claude đọc GitHub theo §9; preflight thất bại thì TV1 sửa credential trước).
- Luồng: owner tạo Issue (D5; TV1 kiểm ID/WBS/Track/reviewer/label) → owner nộp `plan-vN.md` (BLOB) → TV1 chạy Codex review, relay Directive (ID, base, BLOB/head, finding + bằng chứng, khuyến nghị) → TV1 duyệt qua đúng kênh → implement, mở PR → peer review **trước** Codex code review → TV1 ACCEPT → merge §7.
- **Track A**: API, DB/migration, security, CI/dependency, scheduling, tích hợp liên module, cách ly dữ liệu test. **Track B**: thay đổi cô lập, rủi ro thấp (plan ≤ ~30 dòng). Chỉ TV1 hạ A → B.
- Claim test phải có bằng chứng chạy. Audit sau ACCEPT ghi ở comment Issue (setup: comment setup PR), không thêm commit vào head đã accept.

## 6. Ma trận peer review
- Hiệu lực từ ngày kích hoạt và chỉ khi A2 đạt; trước đó dùng ma trận cũ trong `docs/plan/05`.
- Đổi người cần người được giao mới đồng ý trên Software Group và TV1 công bố.
- Leo thang: chính không review trong 1 khung (≤ 4h) → dự phòng; cả hai vắng > 1 khung → TV1 nhờ thành viên còn lại (không phải tác giả, không phải TV1) và người đó đồng ý; không có ai → PR chờ. Không bao giờ để TV1 hoặc tác giả làm peer. Miễn peer theo D12 là việc của TV1 cho PR của chính TV1, không phải TV1 làm peer.

| Gói | Chính | Dự phòng |
|---|---|---|
| TV1 FE (FE-00/01, FE-02/03, FE-AU) | TV3 | TV4 |
| TV1 còn lại (GR-04, IT-01, RL-01 PR-1/PR-2, fix) | TV4 | TV3 |
| TV2 SC-04, 05a, 05b, 06, 07 | TV4 | TV3 |
| TV2 FE | TV4 | TV3 |
| TV3 TK-03/04, AV-02/04, AV-03, CAL-01/AV-05, FE | TV2 | TV4 |
| TV4 BE (GR-03, AS-01, AS-PG, AS-02, AI-01, AI-02) | TV3 | TV2 |
| TV4 FE | TV2 | TV3 |
| Fix batch | người không phải owner (xoay vòng) | người không phải owner còn lại |

## 7. Merge checklist (TV1 làm mọi thao tác ghi; Claude tự đọc GitHub theo §9 và báo cáo, không ghi)
Chỉ TV1 merge, từng PR một (D9).

### 7.1 Khóa CI và merge (mọi luồng)
Đầu vào: `REPO=uth-software-team/PlanWise`, PR `N`, base `B`, head đã accept `H`, `D = git rev-parse origin/<B>` sau `git fetch origin`.
0. `gh pr view N -R $REPO --json number,url,title,body,state,author,isDraft,baseRefName,headRefOid,closingIssuesReferences`: đúng repo, PR mở, `baseRefName == B`, `headRefOid == H`. Issue: feature → `closingIssuesReferences` đúng Issue PW; setup → không Issue, không `Closes`; release/hotfix → theo giai đoạn §7.3. Ngay trước merge kiểm lại repo, base, head, base tip, review.
1. `gh run list -R $REPO --workflow gitflow-check --event pull_request --commit H --limit 100 --json databaseId,headSha,attempt,conclusion,createdAt,event`; mới nhất trước, chọn run đầu tiên có log `refs/remotes/pull/N/merge`. `gh run view <id> -R $REPO --json event,headSha,attempt,conclusion,jobs`: `event == pull_request`, `headSha == H`, `conclusion == success`; ghi `attempt`.
2. Job áp dụng (theo workflow tại M): hiện `gitflow`, `test`, `pg`; sau FE-00 thêm `frontend`. Tất cả `success`.
3. `gh run view <id> --attempt <a> --log`: log **từng** job có `+M:refs/remotes/pull/N/merge` và `HEAD is now at … Merge H into D_ci`; mọi job cùng `M`.
4. `gh api repos/$REPO/git/commits/M`: `parents == [D, H]` (M^1 == D, M^2 == H); ghi `tree(M)`.
5. **Phục hồi khi bước 1–4 không đạt** (chưa có bằng chứng CI):
   - **5a** D, H không đổi nhưng bằng chứng hỏng/thiếu → TV1 được Re-run đúng run đó (chỉ có giá trị cho cặp cũ); kiểm lại bước 1–4 với attempt mới. `M^1 ≠ D` → 5b.
   - **5b** Base đổi, head không đổi → không Re-run; owner cập nhật nhánh (merge `origin/<B>`), IA §7.4. Nếu `git merge-base --is-ancestor D H` đúng (merge báo "Already up to date") thì cần **commit rỗng do owner là người thật** tạo và push (task của TV1, kể cả setup: TV1 tự làm; agent không làm):
     - trước commit: đúng nhánh head của PR (không phải `main`/`develop`), `git rev-parse HEAD` == H, `git write-tree` == `git rev-parse H^{tree}`, `git merge-base --is-ancestor D H` đúng;
     - `git commit --allow-empty -m "test(ci): refresh checks for base <D:7>"` (hook chạy bình thường);
     - sau commit, trước push: đúng một parent == H và `HEAD^{tree}` == `H^{tree}`; lệch thì không push, TV1 quyết;
     - sau push: CI cho `(D, H')` theo bước 0–4; peer (hoặc `WAIVE_PEER_REVIEW` mới, nếu D12) và TV1 xác nhận lại trên `H'`. Setup: quay lại §7.2 từ bước 4 với `C' = H'`.
   - **5c** Head đổi do thay đổi nội dung → không phải IA; quay về code review, chạy lại từ bước 0; Approve/ACCEPT/WAIVE cũ hết hiệu lực.
   - Không bao giờ đóng/mở lại PR để làm mới CI.
6. Review — chọn đúng một đường:
   - **6a. Peer** (mặc định; bắt buộc với PR do TV2–TV4 mở): `latestReviews` có Approve của peer (§6) với `commit.oid == H`.
     - **PR do teammate mở**: thêm Approve của TV1 với body đúng `ACCEPT_TASK <ID> COMMIT:H` và `commit.oid == H`.
     - **PR do TV1 mở** (task TV1/Claude, setup): không yêu cầu review của TV1; bằng chứng = lệnh trong phiên (`ACCEPT_TASK …` hoặc `ACCEPT_SETUP … COMMIT:C …` với C == H) + comment audit TV1 dán trên PR + Approve của peer tại H.
     - Thiếu field thì đối chiếu review gốc; thiếu bằng chứng → dừng. `reviewDecision == APPROVED`, `mergeStateStatus == CLEAN`.
   - **6b. TV1 miễn peer** (D12; chỉ PR do TV1 mở). Đạt **tất cả**:
     1. `author.login == kietit2k6`, `isDraft == false` (bước 0).
     2. Có bằng chứng ACCEPT của TV1 cho đúng H như PR do TV1 mở ở 6a (lệnh trong phiên + comment audit), và Codex review theo Track (setup: §7.2 bước 4) đã xong cho H.
     3. Cần **cả hai**, do TV1 tự làm: TV1 gõ trong phiên `WAIVE_PEER_REVIEW <docs-ai|TASK_ID> PR:#N COMMIT:H` (SES), **và** tự đăng đúng nguyên văn dòng đó làm comment trên PR N bằng tài khoản `kietit2k6` khi `headRefOid == H` (L2). Dùng cho đúng một lần merge; head đổi → hết hiệu lực. Thiếu một trong hai → không có miễn → quay về 6a.
     4. `gh pr view N -R $REPO --json headRefOid,mergeable,reviewDecision,mergeStateStatus,statusCheckRollup`: `headRefOid == H`; `mergeable == MERGEABLE`; `reviewDecision ∈ {APPROVED, REVIEW_REQUIRED}` (`CHANGES_REQUESTED` → dừng, không dùng miễn để vượt yêu cầu sửa); mọi check bắt buộc (`gitflow`, `test`) và `pg` (D6) của H là `SUCCESS`; `mergeStateStatus == CLEAN`, **hoặc** `BLOCKED` chỉ khi `reviewDecision == REVIEW_REQUIRED` và mọi điều trên cùng bước 6b.5 đạt. Giá trị khác (`DIRTY`, `BEHIND`, `UNSTABLE`, `UNKNOWN`, `HAS_HOOKS`, `DRAFT`) → dừng.
     5. `gh api repos/$REPO/rules/branches/<B>`: chỉ có rule `deletion`, `non_fast_forward`, `pull_request`, `required_status_checks`; trong `pull_request`: `required_approving_review_count == 1`, `require_code_owner_review` và `require_last_push_approval` đều `false`; required checks ⊆ {`gitflow`, `test`}. Có rule hoặc tham số khác → dừng, TV1 đánh giá rule mới trước khi dùng `--admin`. **Resolve thread:** không đọc được `required_review_thread_resolution` → dừng. Nếu giá trị là `true`: chạy `gh api graphql -f query='query($o:String!,$r:String!,$n:Int!){repository(owner:$o,name:$r){pullRequest(number:$n){reviewThreads(first:100){totalCount pageInfo{hasNextPage} nodes{isResolved}}}}}' -f o=uth-software-team -f r=PlanWise -F n=N`; chỉ đạt khi lệnh thành công, `hasNextPage == false`, số `nodes` == `totalCount` và mọi `isResolved == true`. Lỗi, thiếu field, còn trang sau hoặc có thread chưa resolve → dừng.
     - Bước 0–4 vẫn bắt buộc; 6b chỉ thay bước peer.
7. Merge (feature/setup) từ terminal TV1: đường 6a `gh pr merge N --squash --match-head-commit H`; đường 6b `gh pr merge N --squash --admin --match-head-commit H`, chạy ngay sau khi 6b đạt. `--admin` vượt mọi rule của ruleset, nên bước 1–4 và 6b là cổng thay thế, không được bỏ.
8. Hậu kiểm squash: `S` có đúng 1 parent và `S^ == D`; `git rev-parse S^{tree}` == `tree(M)`. Đạt → TV1 comment kết quả; không đạt → §7.5. Đường 6b: `gh pr view N --json mergedBy,mergeCommit` có `mergedBy.login == kietit2k6` và `mergeCommit.oid == S`.

**Bản ghi bằng chứng ACCEPT** (comment Issue; setup: comment setup PR): `ACCEPTED_HEAD = H`, `D_old = M^1`, `M`, run id, attempt, link Approve của peer (`commit.oid`). PR teammate: thêm link Approve của TV1 (body `ACCEPT_TASK`, `commit.oid == H`). PR do TV1 mở: thêm nguyên văn lệnh ACCEPT trong phiên, thời điểm kèm múi giờ, link comment audit, và ghi "no TV1 GitHub review (author)". Đường 6b: thay link Approve của peer bằng nguyên văn `WAIVE_PEER_REVIEW`, thời điểm kèm múi giờ, link comment của TV1, giá trị `reviewDecision`/`mergeStateStatus`/`mergeable`/`statusCheckRollup` danh sách rule và (nếu bắt buộc resolve thread) `totalCount` cùng kết quả "all resolved" ở bước 6b, ghi "peer review waived by TV1 (D12)". Không bao giờ yêu cầu TV1 tự review PR của mình.

### 7.2 Setup (`docs/ai-governance-setup` → `develop`, squash, không Issue PW)
1. `APPROVE_SETUP docs-ai <v> SHA256:<plan>`. 2. Claude tạo/sửa đúng 6 path, chạy V-1..V-3 (V-3: theo setup plan SHA256 ở đầu file này) và báo TREE/MSG theo §4. 3. TV1 tự commit theo §4 → candidate `C`; Claude kiểm sau commit. 4. Codex review `git diff B C`, tự tính lại CHANGESET. 5. `ACCEPT_SETUP docs-ai <v> COMMIT:C CHANGESET:<x>`. 6. TV1 push đúng `C`, mở PR: `headRefOid == C`, base `develop`, body ghi `COMMIT:C`, `CHANGESET:`, plan SHA256, không `Closes`. 7. TV3 Approve tại C (vắng: TV4), **hoặc** TV1 miễn peer theo D12 (`WAIVE_PEER_REVIEW docs-ai PR:#N COMMIT:C`, §7.1 bước 6b). 8. §7.1 bước 0–8 với `H = C`, đường 6a hoặc 6b; bản ghi ACCEPT dạng "PR do TV1 mở". 9. TV1 comment hậu kiểm trên setup PR (`S`, `D`, `M`, `tree(S)`, `tree(M)`, kết quả). Thất bại/thiếu bước → A1 chưa đạt, §7.5. Head ≠ C → candidate mới, quay lại bước 3 (commit rỗng: bước 4); mọi Approve/ACCEPT/WAIVE cũ hết hiệu lực.

### 7.3 Release và hotfix (merge commit, hai base)
- Release: `release/x.y.z` từ `develop`, Issue `PW-INT-…`, tag `vx.y.z`. Hotfix: `hotfix/<desc>` từ tip `main`, Issue `PW-<BE|FE>-…`, Track A, tag `vX.Y.(Z+1)`. Không dùng `Closes`.
- **Kiểm PR-1**: title có `[PW-…]` trùng Issue gốc; body `Issue: #<n>` (Issue mở, đúng PW-ID) và `Pair: PR-1 #<n1> / PR-2 pending`.
- **PR-1 → `main`**: §7.1 với `B = main`, `D = D_main`, head `H1`; Codex review `git diff D_main...H1`; `gh pr merge N1 --merge --match-head-commit H1` → `MM`; kiểm `MM` có 2 parent, `MM^1 == D_main`, `MM^2 == H1`, `tree(MM) == tree(M1)`. Đạt → TV1 tag trên `MM`; không đạt → §7.5, dừng. PR do TV1 mở có thể dùng §7.1 bước 6b; khi đó lệnh merge thêm `--admin` (giữ `--merge --match-head-commit`).
- **Fast-forward** (chỉ khi MM đạt): TV1 fetch, xác minh đúng nhánh và HEAD == H1, `git merge --ff-only MM`, push → `H2 = MM`.
- **Mở PR-2**: body cùng `[PW-…]`, cùng `Issue: #<n>`, `Pair: PR-1 #<n1> / PR-2 #<n2>`; TV1 sửa body PR-1 thay `pending` bằng `#<n2>`.
- **Kiểm PR-2**: cả hai PR cùng PW-ID, cùng Issue, trỏ chéo nhau; PR-1 merged và MM đạt.
- **PR-2 → `develop`**: §7.1 với `B = develop`, head `H2`; mức review theo delta `git diff D_dev...H2` (chỉ thay đổi đã review ở PR-1 → Track B; có commit `main` mà `develop` chưa có hoặc resolve conflict → Track A); peer + TV1 ACCEPT tại `H2`; `--merge --match-head-commit H2` → `MD`; kiểm `MD` có 2 parent, `MD^1 == D_dev`, `MD^2 == H2`, `tree(MD) == tree(M2)`. PR do TV1 mở có thể dùng §7.1 bước 6b; khi đó lệnh merge thêm `--admin` (giữ `--merge --match-head-commit`).
- TV1 đóng Issue bằng tay sau PR-2, comment bản ghi của cả hai PR.

### 7.4 IA khi base đổi
- Dùng base thực tế của PR. `D_old` = `M^1` trong bản ghi ACCEPT gần nhất (không có → thiếu bằng chứng). `D_new = git rev-parse origin/<B>` sau fetch. Base đổi thì luôn cập nhật nhánh (§7.1 bước 5b).
1. Bằng chứng merge sạch, theo cấu trúc thực tế của `H'`:
   - merge: `H'` có 2 parent, `H'^1 == H`, `H'^2 == D_new`; `git merge-tree --write-tree H D_new` exit 0 và == `H'^{tree}`;
   - commit rỗng: `git merge-base --is-ancestor D_new H`; `H'` có 1 parent == H; `H'^{tree} == H^{tree}`;
   - CI: chỉ §7.1 bước 0–4 cho `(D_new, H')` trước khi quyết mức review.
2. Tác động: `git diff --name-only D_old D_new` so với nhóm API / DB / Security / Dependency / Shared / workflow CI và file PR đang sửa; kiểm thay đổi `*Api`, contract, hành vi liên module.
3. Quyết định:

| Tình huống | Cần làm |
|---|---|
| (1) đạt, (2) không tác động | `IA: none` + CI mới + peer Approve tại `H'` + TV1 ACCEPT `H'` |
| (1) đạt, overlap/thay đổi nhóm không rủi ro | Như trên + Codex delta review Track B |
| (1) đạt, tác động nhóm rủi ro/liên module | Codex delta review Track A (+ test nếu Codex yêu cầu) |
| (1) thiếu bằng chứng hoặc có resolve conflict | Mặc định rủi ro: Codex delta review Track A, peer review đầy đủ lại, TV1 ACCEPT; không được `IA: none` |

Chưa có CI thành công cho đúng cặp base/head hiện tại → chỉ đánh giá kỹ thuật; ACCEPT và merge dừng. Peer và TV1 luôn xác nhận trên `H'`. PR theo D12: mọi "peer Approve/peer review lại" trong bảng được thay bằng `WAIVE_PEER_REVIEW` mới cho `H'`; phần Codex review trong bảng giữ nguyên.

### 7.5 Ngoại lệ sau merge
Ghi S, D, M; Issue/setup chưa xong. TV1 chọn: chạy `./gradlew build pgTest` trên S (có ủy quyền) và Codex review `git diff M S`; hoặc revert qua PR.

## 8. Recovery sau `/clear`
1. Đọc `CLAUDE.md` (gốc, module), `AGENTS.md`, file này, `APPROVED_DECISIONS.md`.
2. Kiểm `git status`, nhánh, HEAD, `core.hooksPath`.
3. Tìm task: nhánh `*/pw-<cat>-<nnn>-*` → Issue #nnn, hoặc ID TV1 đưa.
4. Đọc `docs/ai/tasks/<ID>/plan-v*.md` và `record.md`, kể cả draft local; draft chỉ có trong chat → báo unavailable.
5. Đối chiếu BLOB/COMMIT; báo state theo label và state theo bằng chứng.
6. **STOP**, xin TV1 quyền mới trong phiên. Mọi quyền thực thi trước `/clear` đã hết hiệu lực.

## 9. Bảo mật
- `.claude/settings.json` = **P-RO** (dùng chung): không allow lệnh `gh` nào; chặn `gh` ghi (PR, Issue, comment, review, merge, run rerun/cancel/delete, label, release, workflow, secret, variable, repo, ruleset, cache, config, extension, alias, auth login/logout/refresh/switch/token/setup-git), `git push` (kể cả `git -C … push`), `git credential`, `git config --global`, `security`, và đọc `.claude/settings.local.json`; `permissions.disableBypassPermissionsMode = "disable"`. Đổi policy cần plan và duyệt riêng.
- **Máy TV1 (D13, ngoài repo):** `.claude/settings.local.json` (đã nằm trong `.git/info/exclude`) đặt `env.GH_TOKEN` = fine-grained PAT **chỉ đọc**, chỉ repo `uth-software-team/PlanWise`, và `env.GH_CONFIG_DIR` = một thư mục rỗng riêng không chứa đăng nhập nào, nên `gh` trong phiên Claude không dùng được đăng nhập admin của TV1. File này allow `gh` đọc (`pr view/list/checks/diff`, `issue view/list`, `run view/list`) và chỉ năm truy vấn `gh api` đọc cố định (preflight `user`, ruleset `develop`/`main`, commit object, GraphQL review thread); `gh api` khác phải hỏi TV1, `gh api` có `-X`/`--method`/`--input` bị chặn; file này cũng chặn `git commit` (kể cả `git -C … commit`). Ghi phía server bị chặn bởi quyền token.
- **Preflight** (chi tiết trong setup plan): trước lệnh `gh` đầu tiên mỗi phiên, Claude kiểm `GH_TOKEN` có, `GH_CONFIG_DIR` trùng `PLANWISE_GH_RO_DIR`, khác thư mục mặc định và không có đăng nhập lưu sẵn, rồi `gh api -i user --jq .login` trả 200, đúng login, token còn hạn. Thất bại → Claude dừng mọi lệnh `gh` và báo TV1; không đổi biến môi trường, không dùng đăng nhập khác. Không in, ghi log hay commit token.
- **Chế độ quyền:** phiên PlanWise chạy ở chế độ `default` (deny chặn, allow chạy ngay, còn lại hỏi TV1); fixture A4 chạy ở đúng chế độ này. Dùng chế độ khác cần plan mới và chạy lại A4. Khi được hỏi, TV1 từ chối mọi lệnh ghi GitHub/git ở dạng lạ (ví dụ đổi biến môi trường trước `gh`, đường dẫn tuyệt đối tới `gh`/`git`).
- Codex: `~/.codex/config.toml` với `sandbox_mode = "read-only"`, `approval_policy = "never"` (TV1 cấu hình).
- D8: không bật browser, computer-use, cloud task hoặc connector có quyền ghi.
- **Fixture trước kích hoạt** (TV1 chạy trong thư mục scratch, không mạng ra ngoài, không credential, không repo thật; control và restricted dùng cùng lệnh; chi tiết trong setup plan):
  - A4 (P-RO, chế độ `default`): hash `.claude/settings.json` và hash phần không có `env` của file local khớp bản ghi; trong scratch với `gh` giả: preflight với dữ liệu giả đạt, còn thiếu token/token sai/sai config dir thì dừng; lệnh đọc (pr view, pr checks, run list, run view --log, năm truy vấn `gh api` được allow) chạy không hỏi; `gh api` ngoài danh sách phải hỏi, `gh api` có phương thức ghi bị deny; lệnh ghi (pr comment, pr merge, auth token, issue create, run rerun) bị deny; lệnh vòng (đổi biến môi trường, đường dẫn tuyệt đối) không tự chạy; `git push` và `git commit` (kể cả `-C`) bị deny. Trên repo thật chỉ kiểm đọc: cấu hình token chỉ đọc đã ghi; Claude đọc được PR, checks, log CI, ruleset, review thread; token thiếu hoặc sai → `gh` thất bại, không rơi về admin. Không thử ghi trên repo thật.
  - A5: Codex chạy được lệnh đọc (control), ghi file bị sandbox chặn và file không tồn tại, truy cập HTTP loopback bị chặn với control trước/sau đạt (nhập nhằng = INCONCLUSIVE), cloud/connector đã kiểm và ghi lại.
  - INCONCLUSIVE ở bất kỳ ca nào → điều kiện tương ứng chưa đạt.
