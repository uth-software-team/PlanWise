import { useState, type FormEvent } from 'react'
import { UserPlus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { FormAlert, FormField } from '@/features/auth/components/FormField'

import { addMemberErrorOf, groupFieldErrorsOf } from '../errors'
import { addMember } from '../groupDetailStore'

/** LEADER only: adds an existing account as MEMBER by email (G8). Disabled while any group write is in flight. */
export function AddMemberForm({ pendingWrite }: { pendingWrite: boolean }) {
  const [email, setEmail] = useState('')
  const [fieldError, setFieldError] = useState<string | undefined>()
  const [formError, setFormError] = useState<string | null>(null)
  const [added, setAdded] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = email.trim()
    setAdded(null)
    setFormError(null)
    if (!value || !value.includes('@')) {
      setFieldError('Nhập email của tài khoản cần thêm.')
      return
    }
    setFieldError(undefined)
    const result = await addMember(value)
    // `stale` (another page, group or account) and `busy` show nothing.
    if (result.status === 'ok') {
      setEmail('')
      setAdded(value)
    } else if (result.status === 'error') {
      const fields = groupFieldErrorsOf(result.error)
      if (fields.email) setFieldError(fields.email)
      else setFormError(addMemberErrorOf(result.error))
    }
  }

  return (
    <form noValidate onSubmit={handleSubmit} className="space-y-3">
      <FormAlert message={formError} />
      <FormField
        id="member-email"
        label="Thêm thành viên bằng email"
        type="email"
        inputMode="email"
        autoComplete="off"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        error={fieldError}
        hint="Người được thêm phải đã có tài khoản PlanWise."
      />
      <Button type="submit" className="h-11" disabled={pendingWrite}>
        <UserPlus aria-hidden="true" />
        {pendingWrite ? 'Đang thêm…' : 'Thêm thành viên'}
      </Button>
      <p role="status" aria-live="polite" className="text-sm text-muted-foreground">
        {added ? `Đã thêm ${added} vào nhóm.` : ''}
      </p>
    </form>
  )
}
