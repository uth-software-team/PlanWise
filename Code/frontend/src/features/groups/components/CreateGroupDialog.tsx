import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { FormAlert, FormField } from '@/features/auth/components/FormField'
import { getToken } from '@/lib/auth-token'

import { createGroup } from '../api/groupsApi'
import { createGroupErrorOf, groupFieldErrorsOf } from '../errors'
import { GroupDialog } from './GroupDialog'

/** Backend limit (GroupRequest.groupName). */
const GROUP_NAME_MAX = 100

type CreateGroupDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** "Tạo nhóm": the caller becomes the group's only LEADER; on success the new group's page opens. */
export function CreateGroupDialog({ open, onOpenChange }: CreateGroupDialogProps) {
  const navigate = useNavigate()
  const [groupName, setGroupName] = useState('')
  const [description, setDescription] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  function handleOpenChange(next: boolean) {
    if (!next) {
      setGroupName('')
      setDescription('')
      setFieldErrors({})
      setFormError(null)
    }
    onOpenChange(next)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    const name = groupName.trim()
    if (!name || name.length > GROUP_NAME_MAX) {
      setFieldErrors({ groupName: 'Tên nhóm không được để trống (tối đa 100 ký tự).' })
      setFormError(null)
      return
    }
    const sentToken = getToken()
    setSubmitting(true)
    setFieldErrors({})
    setFormError(null)
    try {
      const group = await createGroup({ groupName: name, description: description.trim() })
      // Drop the result if the page closed or another account signed in meanwhile.
      if (!mounted.current || getToken() !== sentToken || group.id === undefined) return
      handleOpenChange(false)
      navigate(`/groups/${group.id}`)
    } catch (error) {
      if (!mounted.current || getToken() !== sentToken) return
      setFieldErrors(groupFieldErrorsOf(error))
      setFormError(createGroupErrorOf(error))
    } finally {
      if (mounted.current) setSubmitting(false)
    }
  }

  return (
    <GroupDialog
      open={open}
      onOpenChange={handleOpenChange}
      title="Tạo nhóm"
      description="Bạn sẽ là trưởng nhóm và có thể thêm thành viên bằng email."
    >
      <form noValidate onSubmit={handleSubmit} className="space-y-4">
        <FormAlert message={formError} />
        <FormField
          id="group-name"
          label="Tên nhóm"
          value={groupName}
          onChange={(event) => setGroupName(event.target.value)}
          maxLength={GROUP_NAME_MAX}
          autoComplete="off"
          required
          error={fieldErrors.groupName}
        />
        <FormField
          id="group-description"
          label="Mô tả (không bắt buộc)"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          autoComplete="off"
          error={fieldErrors.description}
        />
        <div className="flex justify-end gap-2 pt-2">
          <Button
            type="button"
            variant="outline"
            className="h-11"
            onClick={() => handleOpenChange(false)}
          >
            Hủy
          </Button>
          <Button type="submit" className="h-11" disabled={submitting}>
            {submitting ? 'Đang tạo…' : 'Tạo nhóm'}
          </Button>
        </div>
      </form>
    </GroupDialog>
  )
}
