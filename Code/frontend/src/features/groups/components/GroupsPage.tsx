import { useEffect, useState, useSyncExternalStore } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ChevronRight, Lock, Plus, RefreshCw, Users } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { getToken, subscribeToken } from '@/lib/auth-token'

import { listGroups, type GroupSummary } from '../api/groupsApi'
import { loadErrorOf } from '../errors'
import { roleLabel } from '../labels'
import { CreateGroupDialog } from './CreateGroupDialog'
import { Initials } from './Initials'

/** Results are tagged with the token they were loaded for, so another account's list is never shown. */
type ListState =
  | { token: string | null; status: 'loading' }
  | { token: string | null; status: 'error'; message: string }
  | { token: string | null; status: 'ready'; groups: GroupSummary[] }

/** "Nhóm của tôi" (team design 03.09.16): the caller's groups with their role, and "Tạo nhóm". */
export function GroupsPage() {
  const token = useSyncExternalStore(subscribeToken, getToken)
  const [state, setState] = useState<ListState>({ token: null, status: 'loading' })
  const [reloadKey, setReloadKey] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)

  useEffect(() => {
    if (!token) return
    let active = true
    listGroups().then(
      (groups) => {
        if (active && getToken() === token) setState({ token, status: 'ready', groups })
      },
      (error: unknown) => {
        if (active && getToken() === token) {
          setState({ token, status: 'error', message: loadErrorOf(error) })
        }
      },
    )
    return () => {
      active = false
    }
  }, [token, reloadKey])

  // A state for another token (or before the first result) is shown as loading.
  const view: ListState = state.token === token ? state : { token, status: 'loading' }

  function retry() {
    setState({ token, status: 'loading' })
    setReloadKey((key) => key + 1)
  }

  return (
    <section aria-labelledby="page-title" className="mx-auto w-full max-w-6xl">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
            Không gian cộng tác
          </p>
          <h1
            id="page-title"
            className="mt-2 text-3xl font-semibold tracking-tight text-foreground"
          >
            Nhóm của tôi
          </h1>
          <p className="mt-2 text-muted-foreground">Quản lý và truy cập các nhóm bạn tham gia.</p>
        </div>
        <Button className="h-11" onClick={() => setCreateOpen(true)}>
          <Plus aria-hidden="true" />
          Tạo nhóm
        </Button>
      </div>

      <h2 id="groups-heading" className="mt-10 text-xl font-semibold text-foreground">
        Nhóm đang tham gia
        {view.status === 'ready' && (
          <span className="ml-1 font-normal text-muted-foreground">({view.groups.length})</span>
        )}
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Chọn một nhóm để xem thành viên và vai trò.
      </p>

      <div className="mt-6" aria-live="polite" aria-busy={view.status === 'loading'}>
        {view.status === 'loading' && (
          <p className="rounded-xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
            Đang tải danh sách nhóm…
          </p>
        )}

        {view.status === 'error' && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-destructive/40 bg-destructive/5 p-6"
          >
            <p className="text-sm text-destructive">{view.message}</p>
            <Button variant="outline" className="h-11" onClick={retry}>
              <RefreshCw aria-hidden="true" />
              Thử lại
            </Button>
          </div>
        )}

        {view.status === 'ready' && view.groups.length === 0 && (
          <div className="rounded-xl border border-dashed border-border bg-card p-10 text-center">
            <Users aria-hidden="true" className="mx-auto size-8 text-muted-foreground" />
            <p className="mt-3 font-medium text-foreground">Bạn chưa tham gia nhóm nào</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Tạo nhóm mới, hoặc nhờ trưởng nhóm thêm bạn bằng email.
            </p>
          </div>
        )}

        {view.status === 'ready' && view.groups.length > 0 && (
          <ul aria-labelledby="groups-heading" className="grid gap-4 md:grid-cols-2">
            {view.groups.map((group) => (
              <li key={group.id}>
                <Link
                  to={`/groups/${group.id}`}
                  className="group flex items-center gap-4 rounded-xl border border-border bg-card p-5 transition-colors hover:border-primary/40 hover:bg-accent/40 focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:outline-none"
                >
                  <Initials name={group.groupName} className="size-14 text-lg" />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-semibold text-foreground">
                      {group.groupName}
                    </span>
                    <span className="mt-1 block text-sm text-muted-foreground">
                      {roleLabel(group.myRole)}
                    </span>
                    <span className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-primary">
                      Xem chi tiết
                      <ArrowRight aria-hidden="true" className="size-4" />
                    </span>
                  </span>
                  <ChevronRight aria-hidden="true" className="size-5 text-muted-foreground" />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      <p className="mt-8 flex items-center gap-2 rounded-xl bg-muted px-4 py-3 text-sm text-muted-foreground">
        <Lock aria-hidden="true" className="size-4 shrink-0" />
        Trạng thái bận/rảnh của thành viên chỉ hiển thị tổng quan, không chia sẻ chi tiết lịch cá
        nhân.
      </p>

      <CreateGroupDialog open={createOpen} onOpenChange={setCreateOpen} />
    </section>
  )
}
