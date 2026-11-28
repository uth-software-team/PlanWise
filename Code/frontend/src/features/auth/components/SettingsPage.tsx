import { useMemo, useState, type FormEvent } from 'react'
import { LogOut } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { updateMe, type UserResponse } from '@/features/auth/api/authApi'
import { FormAlert, FormField } from '@/features/auth/components/FormField'
import { fieldErrorsOf, formErrorOf } from '@/features/auth/errors'
import { reloadCurrentUser, setCurrentUser, signOut, useCurrentUser } from '@/features/auth/session'
import { getToken } from '@/lib/auth-token'
import { cn } from '@/lib/utils'

/** The backend may send `HH:mm:ss`; `<input type="time">` works with `HH:mm`. */
function toTimeInput(value: string | undefined): string {
  return value ? value.slice(0, 5) : ''
}

function timezoneOptions(current: string): string[] {
  let zones: string[]
  try {
    zones = Intl.supportedValuesOf('timeZone')
  } catch {
    zones = []
  }
  return zones.includes(current) || !current ? zones : [current, ...zones]
}

/** /settings. MVP scope (FE-AU): the profile only (name, time zone, planning window) and logout. */
export function SettingsPage() {
  const current = useCurrentUser()

  return (
    <section aria-labelledby="page-title" className="mx-auto w-full max-w-3xl">
      <h1 id="page-title" className="text-3xl font-semibold tracking-tight text-foreground">
        Cài đặt
      </h1>
      <p className="mt-2 text-muted-foreground">
        Điều chỉnh PlanWise phù hợp với nhịp sống của bạn.
      </p>

      <div className="mt-8">
        {current.status === 'loading' && (
          <p
            role="status"
            className="rounded-xl border border-border bg-card p-6 text-muted-foreground"
          >
            Đang tải hồ sơ…
          </p>
        )}
        {current.status === 'error' && (
          <div role="alert" className="rounded-xl border border-destructive/40 bg-card p-6">
            <p className="text-destructive">Không tải được hồ sơ: {current.message}</p>
            <Button variant="outline" className="mt-4 h-11" onClick={reloadCurrentUser}>
              Thử lại
            </Button>
          </div>
        )}
        {current.status === 'ready' && (
          // A different account (e.g. after a login in another tab) gets a fresh form.
          <ProfileForm key={current.user.id ?? current.user.email} user={current.user} />
        )}
      </div>

      <Button
        variant="ghost"
        className="mt-8 h-11 text-destructive hover:bg-destructive/10 hover:text-destructive"
        onClick={signOut}
      >
        <LogOut aria-hidden="true" />
        Đăng xuất
      </Button>
    </section>
  )
}

function ProfileForm({ user }: { user: UserResponse }) {
  const [fullName, setFullName] = useState(user.fullName ?? '')
  const [timezone, setTimezone] = useState(user.timezone ?? '')
  const [planningStart, setPlanningStart] = useState(toTimeInput(user.planningStart))
  const [planningEnd, setPlanningEnd] = useState(toTimeInput(user.planningEnd))
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const zones = useMemo(() => timezoneOptions(user.timezone ?? ''), [user.timezone])

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {}
    const name = fullName.trim()
    if (!name) errors.fullName = 'Vui lòng nhập họ và tên.'
    else if (name.length > 100) errors.fullName = 'Họ và tên tối đa 100 ký tự.'
    if (!timezone) errors.timezone = 'Vui lòng chọn múi giờ.'
    if (!planningStart || !planningEnd) {
      errors.planningStart = 'Cần nhập cả giờ bắt đầu và giờ kết thúc.'
    } else if (planningStart >= planningEnd) {
      errors.planningEnd = 'Giờ kết thúc phải sau giờ bắt đầu.'
    }
    return errors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    setFormError(null)
    setSaved(false)
    if (Object.keys(errors).length > 0) return

    const token = getToken()
    if (!token) return
    setSubmitting(true)
    try {
      const updated = await updateMe({
        fullName: fullName.trim(),
        timezone,
        planningStart,
        planningEnd,
      })
      // A response for a session that already ended (logout, another account) is dropped.
      if (setCurrentUser(updated, token)) setSaved(true)
    } catch (error) {
      if (getToken() !== token) return
      setFieldErrors(fieldErrorsOf(error))
      setFormError(formErrorOf(error))
    } finally {
      setSubmitting(false)
    }
  }

  const timezoneError = fieldErrors.timezone
  const windowError = fieldErrors.planningStart ?? fieldErrors.planningEnd

  return (
    <form noValidate onSubmit={handleSubmit} className="space-y-6">
      <div className="rounded-xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground">Hồ sơ cá nhân</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Thông tin hiển thị trong không gian làm việc.
        </p>

        <div className="mt-6 grid gap-5 md:grid-cols-2">
          <FormField
            id="profile-full-name"
            label="Họ và tên"
            autoComplete="name"
            maxLength={100}
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            error={fieldErrors.fullName}
            required
          />
          <FormField
            id="profile-email"
            label="Địa chỉ email"
            type="email"
            value={user.email ?? ''}
            readOnly
            hint="Email dùng để đăng nhập, không đổi được ở đây."
            className="bg-muted"
          />
        </div>

        <div className="mt-5 space-y-2">
          <Label htmlFor="profile-timezone">Múi giờ</Label>
          <select
            id="profile-timezone"
            value={timezone}
            onChange={(event) => setTimezone(event.target.value)}
            aria-invalid={timezoneError ? true : undefined}
            aria-describedby={timezoneError ? 'profile-timezone-error' : undefined}
            className={cn(
              'h-11 w-full rounded-md border border-muted-foreground/70 bg-transparent px-3 text-base outline-none md:text-sm',
              'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring',
              'aria-invalid:border-destructive',
            )}
          >
            {!timezone && <option value="">Chọn múi giờ</option>}
            {zones.map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </select>
          {timezoneError && (
            <p id="profile-timezone-error" className="text-sm text-destructive">
              {timezoneError}
            </p>
          )}
        </div>
      </div>

      <fieldset
        className="rounded-xl border border-border bg-card p-6"
        aria-describedby={windowError ? 'profile-window-error' : 'profile-window-hint'}
      >
        <legend className="sr-only">Khung giờ lập kế hoạch</legend>
        <h2 aria-hidden="true" className="text-lg font-semibold text-foreground">
          Khung giờ lập kế hoạch
        </h2>
        <p id="profile-window-hint" className="mt-1 text-sm text-muted-foreground">
          Lịch thông minh chỉ xếp phiên làm việc trong khoảng giờ này mỗi ngày.
        </p>
        <div className="mt-6 grid gap-5 sm:grid-cols-2">
          <FormField
            id="profile-planning-start"
            label="Bắt đầu"
            type="time"
            value={planningStart}
            onChange={(event) => setPlanningStart(event.target.value)}
            aria-invalid={windowError ? true : undefined}
            required
          />
          <FormField
            id="profile-planning-end"
            label="Kết thúc"
            type="time"
            value={planningEnd}
            onChange={(event) => setPlanningEnd(event.target.value)}
            aria-invalid={windowError ? true : undefined}
            required
          />
        </div>
        {windowError && (
          <p id="profile-window-error" className="mt-3 text-sm text-destructive">
            {windowError}
          </p>
        )}
      </fieldset>

      <FormAlert message={formError} />
      <div className="flex flex-wrap items-center gap-4">
        <Button type="submit" className="h-11" disabled={submitting}>
          {submitting ? 'Đang lưu…' : 'Lưu thay đổi'}
        </Button>
        <p role="status" className="text-sm text-muted-foreground">
          {saved ? 'Đã lưu thay đổi.' : ''}
        </p>
      </div>
    </form>
  )
}
