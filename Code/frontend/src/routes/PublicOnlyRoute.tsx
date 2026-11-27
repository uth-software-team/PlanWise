import { Navigate, Outlet } from 'react-router-dom'

import { DEFAULT_APP_PATH } from '@/app/nav'
import { useHasToken } from '@/lib/auth-token'

/** Signed-in users skip /login and /register. */
export function PublicOnlyRoute() {
  const signedIn = useHasToken()

  if (signedIn) {
    return <Navigate to={DEFAULT_APP_PATH} replace />
  }

  return <Outlet />
}
