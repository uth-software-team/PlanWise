import { useSyncExternalStore } from 'react'

// MVP token storage per CG-04: access JWT in localStorage, no refresh token.
// Every access is wrapped because storage can be unavailable (private mode, blocked site data).
// This module is the only token store; apiClient reads it and clears it on a 401 for the current token (FE-02).
const TOKEN_KEY = 'planwise.accessToken'

// Key written by the old schedule token bar. Its value is never read or migrated: the current session always
// wins, and a token removed by logout can never come back from it. It is only deleted.
const LEGACY_TOKEN_KEY = 'planwise_token'

type Listener = () => void
const listeners = new Set<Listener>()

function notify(): void {
  listeners.forEach((listener) => listener())
}

function discardLegacyToken(): void {
  try {
    window.localStorage.removeItem(LEGACY_TOKEN_KEY)
  } catch {
    // Storage unavailable: nothing to discard.
  }
}

discardLegacyToken()

export function getToken(): string | null {
  try {
    return window.localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string): void {
  discardLegacyToken()
  try {
    window.localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // Storage unavailable: the user stays signed out.
  }
  notify()
}

export function clearToken(): void {
  discardLegacyToken()
  try {
    window.localStorage.removeItem(TOKEN_KEY)
  } catch {
    // Nothing to clear.
  }
  notify()
}

export function hasToken(): boolean {
  return Boolean(getToken())
}

/**
 * Subscribe to token changes: setToken/clearToken in this tab, and `storage`
 * events from other tabs (key === null means the whole storage was cleared).
 */
export function subscribeToken(listener: Listener): () => void {
  listeners.add(listener)

  function onStorage(event: StorageEvent) {
    if (event.key === TOKEN_KEY || event.key === null) {
      listener()
    }
  }
  window.addEventListener('storage', onStorage)

  return () => {
    listeners.delete(listener)
    window.removeEventListener('storage', onStorage)
  }
}

/** Re-renders whenever the token appears or disappears, in this tab or another. */
export function useHasToken(): boolean {
  return useSyncExternalStore(subscribeToken, hasToken)
}
