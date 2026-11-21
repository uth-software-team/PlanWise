import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useHasToken } from '@/lib/auth-token'

/** Signed-out users are sent to /login, keeping the requested path in `redirect`. */
export function ProtectedRoute() {
  const location = useLocation()
  const signedIn = useHasToken()

  if (!signedIn) {
    const redirect = `${location.pathname}${location.search}`
    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />
  }

  return <Outlet />
}
