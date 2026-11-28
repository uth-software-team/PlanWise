import { useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { login, register } from '@/features/auth/api/authApi'
import { FormAlert, FormField } from '@/features/auth/components/FormField'
import type { LoginLocationState } from '@/features/auth/components/LoginPage'
import { fieldErrorsOf, formErrorOf, passwordProblem } from '@/features/auth/errors'
import { setCurrentUser } from '@/features/auth/session'
import { setToken } from '@/lib/auth-token'

/** The browser's IANA time zone, sent with the sign-up; omitted when the browser does not report one. */
function browserTimezone(): string | undefined {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || undefined
  } catch {
    return undefined
  }
}

/**
 * /register. POST /auth/register returns the user without a token (201), so the page signs in right after.
 * If that sign-in fails, the account still exists: the user continues on /login with the same `?redirect=`.
 */
export function RegisterPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {}
    const name = fullName.trim()
    if (!name) errors.fullName = 'Vui lòng nhập họ và tên.'
    else if (name.length > 100) errors.fullName = 'Họ và tên tối đa 100 ký tự.'
    const mail = email.trim()
    if (!mail) errors.email = 'Vui lòng nhập email.'
    else if (mail.length > 255 || !mail.includes('@')) errors.email = 'Email không hợp lệ.'
    const problem = passwordProblem(password)
    if (problem) errors.password = problem
    return errors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    setFormError(null)
    if (Object.keys(errors).length > 0) return

    const credentials = { email: email.trim(), password }
    setSubmitting(true)
    try {
      await register({ ...credentials, fullName: fullName.trim(), timezone: browserTimezone() })
    } catch (error) {
      setFieldErrors(fieldErrorsOf(error))
      setFormError(formErrorOf(error))
      setSubmitting(false)
      return
    }

    try {
      const result = await login(credentials)
      if (!result.accessToken) throw new Error('No access token')
      setToken(result.accessToken)
      if (result.user) setCurrentUser(result.user, result.accessToken)
    } catch {
      const state: LoginLocationState = { registered: true, email: credentials.email }
      navigate({ pathname: '/login', search: location.search }, { replace: true, state })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="register-title">
      <p className="text-xs font-semibold tracking-widest text-primary uppercase">
        Bắt đầu miễn phí
      </p>
      <h1
        id="register-title"
        className="mt-3 text-3xl font-semibold tracking-tight text-foreground"
      >
        Tạo tài khoản
      </h1>
      <p className="mt-2 text-muted-foreground">Tạo không gian để bắt đầu sắp xếp tuần của bạn.</p>

      <form noValidate onSubmit={handleSubmit} className="mt-8 space-y-5">
        <FormField
          id="register-full-name"
          label="Họ và tên"
          autoComplete="name"
          placeholder="Nguyễn Minh Anh"
          maxLength={100}
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          error={fieldErrors.fullName}
          required
        />
        <FormField
          id="register-email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="ten@email.com"
          maxLength={255}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
          required
        />
        <FormField
          id="register-password"
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          placeholder="Tối thiểu 8 ký tự"
          hint="Ít nhất 8 ký tự, tối đa 72 byte (chữ có dấu chiếm nhiều byte hơn)."
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
          required
        />
        <FormAlert message={formError} />
        <Button type="submit" className="h-11 w-full" disabled={submitting}>
          {submitting ? 'Đang tạo tài khoản…' : 'Tạo tài khoản'}
          {!submitting && <ArrowRight aria-hidden="true" />}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link
          to={{ pathname: '/login', search: location.search }}
          className="rounded-sm font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring"
        >
          Đăng nhập
        </Link>
      </p>
    </section>
  )
}
