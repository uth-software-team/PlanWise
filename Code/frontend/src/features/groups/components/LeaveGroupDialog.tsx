import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { FormAlert } from '@/features/auth/components/FormField'

import type { GroupDetail } from '../api/groupsApi'
import { leaveErrorOf } from '../errors'
import { leaveGroup } from '../groupDetailStore'
import { successorNameOf } from '../labels'
import { GroupDialog } from './GroupDialog'

type LeaveGroupDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** The group as currently loaded; the variant follows it, so a reload after STALE_VERSION updates the text. */
  group: GroupDetail
  myUserId: number
  busy: boolean
}

/**
 * "Rời nhóm" (D16), three variants computed from the loaded group:
 * - MEMBER: leaves without a successor.
 * - LEADER with `successorUserId`: names the successor and sends it as `expectedSuccessorUserId`.
 * - LEADER without a successor: explains to add a member first; no confirm button.
 * On STALE_VERSION or DATA_CONFLICT the group is reloaded and the dialog stays open with the new variant.
 */
export function LeaveGroupDialog({
  open,
  onOpenChange,
  group,
  myUserId,
  busy,
}: LeaveGroupDialogProps) {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const me = (group.members ?? []).find((member) => member.userId === myUserId)
  const isLeader = me?.role === 'LEADER'
  const successorId = group.successorUserId ?? undefined
  const variant = !isLeader ? 'member' : successorId !== undefined ? 'leader' : 'leader-alone'

  function handleOpenChange(next: boolean) {
    if (!next) setError(null)
    onOpenChange(next)
  }

  async function confirm() {
    setError(null)
    const expected = variant === 'leader' ? successorId : undefined
    const result = await leaveGroup(myUserId, expected, () => navigate('/groups'))
    if (result.status === 'error') setError(leaveErrorOf(result.error))
  }

  return (
    <GroupDialog open={open} onOpenChange={handleOpenChange} title="Rời nhóm">
      <div className="space-y-4">
        {variant === 'member' && (
          <p className="text-sm text-foreground">
            Bạn sẽ không còn xem được nhóm “{group.groupName}”. Trưởng nhóm có thể thêm bạn lại sau.
          </p>
        )}
        {variant === 'leader' && (
          <p className="text-sm text-foreground">
            Bạn đang là trưởng nhóm. Khi bạn rời nhóm,{' '}
            <strong className="font-semibold">{successorNameOf(group)}</strong> sẽ trở thành trưởng
            nhóm.
          </p>
        )}
        {variant === 'leader-alone' && (
          <p className="text-sm text-foreground">
            Bạn là trưởng nhóm và chưa có thành viên nào nhận được quyền. Hãy thêm thành viên trước,
            rồi rời nhóm.
          </p>
        )}
        <FormAlert message={error} />
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            className="h-11"
            onClick={() => handleOpenChange(false)}
          >
            {variant === 'leader-alone' ? 'Đóng' : 'Hủy'}
          </Button>
          {variant !== 'leader-alone' && (
            <Button
              type="button"
              variant="destructive"
              className="h-11 focus-visible:ring-ring"
              disabled={busy}
              onClick={() => void confirm()}
            >
              {busy ? 'Đang xử lý…' : 'Rời nhóm'}
            </Button>
          )}
        </div>
      </div>
    </GroupDialog>
  )
}
