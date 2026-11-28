import { useSyncExternalStore } from 'react'

import { getMe, logout, type UserResponse } from '@/features/auth/api/authApi'
import { ApiError } from '@/lib/apiClient'
import { clearToken, getToken, subscribeToken } from '@/lib/auth-token'

export type CurrentUserState =
  | { status: 'signed-out' }
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; user: UserResponse }

/** One entry per token. A GET/PUT result is applied only while its entry is still current (see `isCurrent`). */
type Entry = { token: string | null; state: CurrentUserState }

const SIGNED_OUT: CurrentUserState = { status: 'signed-out' }
const LOADING: CurrentUserState = { status: 'loading' }

let entry: Entry = { token: null, state: SIGNED_OUT }
const listeners = new Set<() => void>()
let unsubscribeToken: (() => void) | null = null

function notify(): void {
  listeners.forEach((listener) => listener())
}

/** True while `target` is the live entry and the stored token is still the one it was created for. */
function isCurrent(target: Entry): boolean {
  return entry === target && getToken() === target.token
}

function load(target: Entry): void {
  getMe().then(
    (user) => {
      if (!isCurrent(target)) return
      entry = { token: target.token, state: { status: 'ready', user } }
      notify()
    },
    (error: unknown) => {
      if (!isCurrent(target)) return
      const message =
        error instanceof ApiError ? error.message : 'Không tải được thông tin tài khoản.'
      entry = { token: target.token, state: { status: 'error', message } }
      notify()
    },
  )
}

/** Starts a new entry when the token changed (login, logout, 401, another tab); the old entry's results are dropped. */
function syncToken(): void {
  const token = getToken()
  if (token === entry.token) return
  entry = { token, state: token ? LOADING : SIGNED_OUT }
  if (token) load(entry)
  notify()
}

export function subscribeCurrentUser(listener: () => void): () => void {
  listeners.add(listener)
  if (!unsubscribeToken) unsubscribeToken = subscribeToken(syncToken)
  syncToken()
  return () => {
    listeners.delete(listener)
    if (listeners.size === 0 && unsubscribeToken) {
      unsubscribeToken()
      unsubscribeToken = null
    }
  }
}

export function getCurrentUserSnapshot(): CurrentUserState {
  // Until a subscriber has synced the store, never expose another token's user.
  if (entry.token !== getToken()) return getToken() ? LOADING : SIGNED_OUT
  return entry.state
}

/** The signed-in user for the token in storage; re-fetched whenever that token changes. */
export function useCurrentUser(): CurrentUserState {
  return useSyncExternalStore(subscribeCurrentUser, getCurrentUserSnapshot)
}

/** Fetches the current user again (e.g. retry after an error). */
export function reloadCurrentUser(): void {
  const token = getToken()
  if (!token) return
  entry = { token, state: LOADING }
  load(entry)
  notify()
}

/** Stores a user returned by PUT /users/me, only if `token` (sent with that PUT) is still the session's token. */
export function setCurrentUser(user: UserResponse, token: string): boolean {
  if (getToken() !== token) return false
  entry = { token, state: { status: 'ready', user } }
  notify()
  return true
}

/**
 * Shared logout for the user menu and Settings. The local session ends first, so it works offline; the server call
 * is best-effort and sends the old token explicitly, so its (late) response can never touch a newer session.
 * Navigation is left to ProtectedRoute, which sends the signed-out user to /login?redirect=<current page>.
 */
export function signOut(): void {
  const token = getToken()
  clearToken()
  if (token) {
    logout(token).catch(() => {
      // Stateless JWT: nothing to undo when the server cannot be reached.
    })
  }
}
