import { Navigate, Outlet, useSearchParams } from 'react-router-dom'

import { useHasToken } from '@/lib/auth-token'
import { resolveRedirect } from '@/lib/safe-redirect'

/**
 * Signed-in users skip /login and /register. This is the only place that decides where a user goes after signing
 * in or registering: the forms just store the token, and this guard sends the user to a safe `?redirect=` target
 * (same-origin path with its query) or to /tasks, replacing the auth page in history.
 */
export function PublicOnlyRoute() {
  const signedIn = useHasToken()
  const [searchParams] = useSearchParams()

  if (signedIn) {
    return <Navigate to={resolveRedirect(searchParams.get('redirect'))} replace />
  }

  return <Outlet />
}
