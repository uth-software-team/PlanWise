import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft, Crown, LogOut, RefreshCw, UserMinus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useCurrentUser } from '@/features/auth/session'

import { isLeaderOf, type GroupMember } from '../api/groupsApi'
import { loadErrorOf, removeMemberErrorOf, transferErrorOf } from '../errors'
import {
  openGroup,
  reloadGroup,
  removeMember,
  transferLeadership,
  useGroupDetail,
  type WriteResult,
} from '../groupDetailStore'
import { roleLabel } from '../labels'
import { AddMemberForm } from './AddMemberForm'
import { ConfirmDialog } from './ConfirmDialog'
import { Initials } from './Initials'
import { LeaveGroupDialog } from './LeaveGroupDialog'
import { MemberList } from './MemberList'

const MEMBERS_HEADING_ID = 'members-heading'

type MemberAction = { kind: 'transfer' | 'remove'; member: GroupMember; name: string }

/** Focus target when the control that opened a dialog is gone (its row, or every LEADER control). */
function membersHeading(): HTMLElement | null {
  return document.getElementById(MEMBERS_HEADING_ID)
}

/** Message for a finished write: null = success, undefined = dropped (another page/account), else the error. */
function messageOf(
  result: WriteResult,
  errorText: (error: unknown) => string,
): string | null | undefined {
  if (result.status === 'ok') return null
  if (result.status === 'error') return errorText(result.error)
  return undefined
}

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
  // The last chosen action stays set while the dialog closes, so Radix can return focus before unmount.
  const [action, setAction] = useState<MemberAction | null>(null)
  const [actionOpen, setActionOpen] = useState(false)

  function openAction(next: MemberAction) {
    setAction(next)
    setActionOpen(true)
  }
  const [leaveOpen, setLeaveOpen] = useState(false)

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

  function renderLeaderActions(member: GroupMember, name: string) {
    if (!isLeader || member.userId === myId || member.userId === undefined) return null
    return (
      <span className="flex w-full justify-end gap-2 sm:w-auto">
        <Button
          variant="outline"
          className="h-11"
          disabled={pendingWrite}
          aria-label={`Chuyển quyền trưởng nhóm cho ${name}`}
          onClick={() => openAction({ kind: 'transfer', member, name })}
        >
          <Crown aria-hidden="true" />
          Chuyển quyền
        </Button>
        <Button
          variant="outline"
          className="h-11 text-destructive hover:text-destructive"
          disabled={pendingWrite}
          aria-label={`Xóa ${name} khỏi nhóm`}
          onClick={() => openAction({ kind: 'remove', member, name })}
        >
          <UserMinus aria-hidden="true" />
          Xóa
        </Button>
      </span>
    )
  }

  async function confirmAction(current: MemberAction): Promise<string | null | undefined> {
    const userId = current.member.userId as number
    if (current.kind === 'transfer') {
      return messageOf(await transferLeadership(userId), transferErrorOf)
    }
    return messageOf(await removeMember(userId), removeMemberErrorOf)
  }

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
        {myId !== undefined && myRole && (
          <Button
            variant="outline"
            className="ml-auto h-11 text-destructive hover:text-destructive"
            disabled={pendingWrite}
            onClick={() => setLeaveOpen(true)}
          >
            <LogOut aria-hidden="true" />
            Rời nhóm
          </Button>
        )}
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
            <MemberList
              members={members}
              headingId={MEMBERS_HEADING_ID}
              renderActions={renderLeaderActions}
            />
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

      {action && (
        <ConfirmDialog
          open={actionOpen}
          onOpenChange={setActionOpen}
          title={action.kind === 'transfer' ? 'Chuyển quyền trưởng nhóm' : 'Xóa thành viên'}
          confirmLabel={action.kind === 'transfer' ? 'Chuyển quyền' : 'Xóa khỏi nhóm'}
          destructive={action.kind === 'remove'}
          busy={pendingWrite}
          onConfirm={() => confirmAction(action)}
          focusFallback={membersHeading}
        >
          <p className="text-sm text-foreground">
            {action.kind === 'transfer' ? (
              <>
                <strong className="font-semibold">{action.name}</strong> sẽ trở thành trưởng nhóm.
                Sau khi chuyển, bạn sẽ trở thành thành viên.
              </>
            ) : (
              <>
                <strong className="font-semibold">{action.name}</strong> sẽ không còn xem được nhóm.
                Bạn có thể thêm lại sau.
              </>
            )}
          </p>
        </ConfirmDialog>
      )}

      {myId !== undefined && (
        <LeaveGroupDialog
          open={leaveOpen}
          onOpenChange={setLeaveOpen}
          group={group}
          myUserId={myId}
          busy={pendingWrite}
        />
      )}
    </section>
  )
}
