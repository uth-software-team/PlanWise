import { useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { login } from '@/features/auth/api/authApi'
import { FormAlert, FormField } from '@/features/auth/components/FormField'
import { fieldErrorsOf, formErrorOf } from '@/features/auth/errors'
import { setCurrentUser } from '@/features/auth/session'
import { setToken } from '@/lib/auth-token'

/** Set by RegisterPage when the account was created but signing in right after failed. */
export type LoginLocationState = { registered?: boolean; email?: string }

function readState(state: unknown): LoginLocationState {
  if (state === null || typeof state !== 'object') return {}
  const { registered, email } = state as Record<string, unknown>
  return {
    registered: registered === true,
    email: typeof email === 'string' ? email : undefined,
  }
}

/** /login. Stores the token only; PublicOnlyRoute then sends the user to the safe `?redirect=` target. */
export function LoginPage() {
  const location = useLocation()
  const { registered, email: registeredEmail } = readState(location.state)

  const [email, setEmail] = useState(registeredEmail ?? '')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const errors: Record<string, string> = {}
    if (!email.trim()) errors.email = 'Vui lòng nhập email.'
    if (!password) errors.password = 'Vui lòng nhập mật khẩu.'
    setFieldErrors(errors)
    setFormError(null)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      const result = await login({ email: email.trim(), password })
      if (!result.accessToken) {
        setFormError('Máy chủ không trả về phiên đăng nhập. Vui lòng thử lại.')
        return
      }
      setToken(result.accessToken)
      if (result.user) setCurrentUser(result.user, result.accessToken)
    } catch (error) {
      setFieldErrors(fieldErrorsOf(error))
      setFormError(formErrorOf(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="login-title">
      <p className="text-xs font-semibold tracking-widest text-primary uppercase">
        Chào mừng trở lại
      </p>
      <h1 id="login-title" className="mt-3 text-3xl font-semibold tracking-tight text-foreground">
        Đăng nhập
      </h1>
      <p className="mt-2 text-muted-foreground">Đăng nhập để tiếp tục với kế hoạch của bạn.</p>

      {registered && (
        <p
          role="status"
          className="mt-6 rounded-md border border-primary/30 bg-accent px-3 py-2 text-sm text-accent-foreground"
        >
          Tài khoản đã được tạo. Vui lòng đăng nhập để tiếp tục.
        </p>
      )}

      <form noValidate onSubmit={handleSubmit} className="mt-8 space-y-5">
        <FormField
          id="login-email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="ten@email.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
          required
        />
        <FormField
          id="login-password"
          label="Mật khẩu"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
          required
        />
        <FormAlert message={formError} />
        <Button type="submit" className="h-11 w-full" disabled={submitting}>
          {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
          {!submitting && <ArrowRight aria-hidden="true" />}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{' '}
        <Link
          to={{ pathname: '/register', search: location.search }}
          className="rounded-sm font-semibold text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring"
        >
          Đăng ký miễn phí
        </Link>
      </p>
    </section>
  )
}
