import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, RefreshCw } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useCurrentUser } from '@/features/auth/session'

import { isLeaderOf } from '../api/groupsApi'
import { loadErrorOf } from '../errors'
import { openGroup, reloadGroup, useGroupDetail } from '../groupDetailStore'
import { roleLabel } from '../labels'
import { AddMemberForm } from './AddMemberForm'
import { Initials } from './Initials'
import { MemberList } from './MemberList'

const MEMBERS_HEADING_ID = 'members-heading'

/** Parses the :groupId route param; anything but a positive integer is "not found" without a request. */
function parseGroupId(raw: string | undefined): number | null {
  if (!raw || !/^\d+$/.test(raw)) return null
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

function BackLink() {
  return (
    <Link
      to="/groups"
      className="inline-flex min-h-11 items-center gap-1 rounded-md text-sm text-muted-foreground hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:outline-none"
    >
      <ChevronLeft aria-hidden="true" className="size-4" />
      Tất cả nhóm
    </Link>
  )
}

/** Group detail (team design 03.09.34, FE-GR-01a): group info, members, and "add member" for the LEADER. */
export function GroupDetailPage() {
  const groupId = parseGroupId(useParams().groupId)

  useEffect(() => {
    if (groupId === null) return
    return openGroup(groupId)
  }, [groupId])

  if (groupId === null) {
    return <GroupMessage message="Không tìm thấy nhóm." />
  }
  return <GroupDetailView />
}

function GroupMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <section aria-labelledby="page-title" className="mx-auto w-full max-w-6xl">
      <BackLink />
      <h1 id="page-title" className="mt-4 text-2xl font-semibold text-foreground">
        Nhóm
      </h1>
      <div
        role="alert"
        className="mt-6 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-border bg-card p-6"
      >
        <p className="text-sm text-foreground">{message}</p>
        {onRetry ? (
          <Button variant="outline" className="h-11" onClick={onRetry}>
            <RefreshCw aria-hidden="true" />
            Thử lại
          </Button>
        ) : (
          <Button asChild variant="outline" className="h-11">
            <Link to="/groups">Về danh sách nhóm</Link>
          </Button>
        )}
      </div>
    </section>
  )
}

function GroupDetailView() {
  const { detail, pendingWrite } = useGroupDetail()
  const currentUser = useCurrentUser()

  if (detail.status === 'idle' || detail.status === 'loading') {
    return (
      <section aria-labelledby="page-title" aria-busy="true" className="mx-auto w-full max-w-6xl">
        <BackLink />
        <h1 id="page-title" className="mt-4 text-2xl font-semibold text-foreground">
          Nhóm
        </h1>
        <p className="mt-6 rounded-xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
          Đang tải thông tin nhóm…
        </p>
      </section>
    )
  }

  if (detail.status === 'error') {
    // 403/404: no group data stays on screen, only the way back (retry would fail the same way).
    const retry = detail.code === 'OTHER' ? reloadGroup : undefined
    return <GroupMessage message={loadErrorOf(detail.error)} onRetry={retry} />
  }

  const { group } = detail
  const members = group.members ?? []
  // Admin controls need the signed-in user, so nothing appears before /users/me is known.
  const myId = currentUser.status === 'ready' ? currentUser.user.id : undefined
  const isLeader = isLeaderOf(group, myId)
  const myRole = members.find((member) => member.userId === myId)?.role

  return (
    <section aria-labelledby="page-title" className="mx-auto w-full max-w-6xl">
      <BackLink />
      <p className="mt-4 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
        Không gian cộng tác
      </p>
      <div className="mt-2 flex flex-wrap items-center gap-4">
        <Initials name={group.groupName} className="size-14 text-lg" />
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 id="page-title" className="text-3xl font-semibold tracking-tight text-foreground">
              {group.groupName}
            </h1>
            {myRole && (
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                {roleLabel(myRole)}
              </span>
            )}
          </div>
          {group.description && <p className="mt-1 text-muted-foreground">{group.description}</p>}
        </div>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,22rem)]">
        <div className="rounded-xl border border-border bg-card p-6">
          <h2
            id={MEMBERS_HEADING_ID}
            tabIndex={-1}
            className="text-lg font-semibold text-foreground focus-visible:outline-none"
          >
            Thành viên
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">{members.length} người trong nhóm</p>
          <div className="mt-4">
            <MemberList members={members} headingId={MEMBERS_HEADING_ID} />
          </div>
        </div>

        {isLeader && (
          <div className="h-fit rounded-xl border border-border bg-card p-6">
            <h2 className="text-lg font-semibold text-foreground">Quản lý thành viên</h2>
            <div className="mt-4">
              <AddMemberForm pendingWrite={pendingWrite} />
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
